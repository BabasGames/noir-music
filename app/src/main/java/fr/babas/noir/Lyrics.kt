package fr.babas.noir

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class LyricLine(val timeMs: Long, val text: String)

sealed interface Lyrics {
    data object Loading : Lyrics
    data class Synced(val lines: List<LyricLine>, val source: String) : Lyrics
    data class Plain(val lines: List<String>, val source: String) : Lyrics
    data object Instrumental : Lyrics
    data object NotFound : Lyrics
    /** No network at all */
    data object Offline : Lyrics
    /** Network available but the sources don't answer (overload, outage…) */
    data object Unavailable : Lyrics
    data object MobileData : Lyrics
}

/**
 * Lyrics from LRCLIB then NetEase (free, no account), kept on disk so they still work offline on the road.
 * The cache keeps the LRCLIB format (syncedLyrics / plainLyrics / instrumental) whatever the source.
 */
class LyricsRepository(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    /**
     * Not cacheDir: Android empties it when storage runs low, and "Clear cache" wipes it,
     * which would break the retention setting and offline use. Not backed up either: it can be downloaded again.
     */
    private val dir = File(context.noBackupFilesDir, "lyrics").also { dir ->
        // Up to 1.0.0 the lyrics were in the cache: move them over (same partition, instant)
        val old = File(context.cacheDir, "lyrics")
        if (old.isDirectory && !dir.exists()) old.renameTo(dir)
    }
    /** Not found during this session: don't query again every time the lyrics are opened. Filled from IO threads. */
    private val missing: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val sources = listOf(LRCLIB to ::fromLrclib, NETEASE_NAME to ::fromNetease)

    /** Sources in use: LRCLIB, plus NetEase only when the fallback is enabled. */
    private fun sources(fallback: Boolean) = if (fallback) sources else sources.filter { it.first == LRCLIB }

    suspend fun load(title: String, artist: String, durationMs: Long, allowMetered: Boolean, fallback: Boolean): Lyrics =
        withContext(Dispatchers.IO) {
            val key = key(title, artist)
            val file = File(dir, "$key.json")
            if (file.exists()) {
                try {
                    // Played again: the retention period restarts from now
                    file.setLastModified(System.currentTimeMillis())
                    return@withContext parse(JSONObject(file.readText()))
                } catch (e: Exception) {
                    file.delete()
                }
            }
            // "Not found" depends on the sources in use: enabling the fallback allows a new lookup
            val missKey = "$key|$fallback"
            if (missKey in missing) return@withContext Lyrics.NotFound
            if (!networkAllowed(allowMetered)) {
                return@withContext if (cm.activeNetwork == null) Lyrics.Offline else Lyrics.MobileData
            }

            var failed = false
            for ((name, source) in sources(fallback)) {
                val found = try {
                    source(title, artist, durationMs)
                } catch (e: Exception) {
                    Log.w(TAG, "$name: lookup failed for \"$artist - $title\"", e)
                    failed = true
                    null
                } ?: continue
                dir.mkdirs()
                file.writeText(found.put("source", name).toString())
                return@withContext parse(found)
            }
            // A source that's down may have the lyrics: don't remember the failure, we'll retry
            if (failed) return@withContext Lyrics.Unavailable
            missing += missKey
            Lyrics.NotFound
        }

    /** Deletes lyrics of tracks not played again for [maxAgeDays] days (null = nothing). */
    suspend fun purge(maxAgeDays: Int?) = withContext(Dispatchers.IO) {
        if (maxAgeDays == null) return@withContext
        val limit = System.currentTimeMillis() - maxAgeDays * 86_400_000L
        dir.listFiles()?.filter { it.lastModified() < limit }?.forEach { it.delete() }
    }

    /** Number of saved tracks and space used (bytes). */
    suspend fun stats(): Pair<Int, Long> = withContext(Dispatchers.IO) {
        val files = dir.listFiles().orEmpty()
        files.size to files.sumOf { it.length() }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
        missing.clear()
    }

    /**
     * Lyrics out of sync or wrong: tries the other sources for this track. What's found replaces
     * the cache, so the choice sticks. null if no other source has anything better.
     */
    suspend fun loadOther(
        title: String,
        artist: String,
        durationMs: Long,
        current: String,
        allowMetered: Boolean,
        fallback: Boolean,
    ): Lyrics? =
        withContext(Dispatchers.IO) {
            if (!networkAllowed(allowMetered)) return@withContext null
            for ((name, source) in sources(fallback)) {
                if (name == current) continue
                val found = try {
                    source(title, artist, durationMs)
                } catch (e: Exception) {
                    Log.w(TAG, "$name: lookup failed for \"$artist - $title\"", e)
                    null
                } ?: continue
                dir.mkdirs()
                File(dir, "${key(title, artist)}.json").writeText(found.put("source", name).toString())
                return@withContext parse(found)
            }
            null
        }

    private fun networkAllowed(allowMetered: Boolean) =
        cm.activeNetwork != null && (allowMetered || !cm.isActiveNetworkMetered)

    private fun key(title: String, artist: String) = sha1("${artist.lowercase()}\n${title.lowercase()}")

    // ── LRCLIB ──────────────────────────────────────────────────────────────

    /** Tries the exact title, then without "(feat. …)", "- Remastered"… and with only the first artist. */
    private suspend fun fromLrclib(title: String, artist: String, durationMs: Long): JSONObject? {
        val queries = listOf(title to artist, cleanTitle(title) to firstArtist(artist)).distinct()
        for ((t, a) in queries) {
            val results = JSONArray(httpGet("https://lrclib.net/api/search?track_name=${enc(t)}&artist_name=${enc(a)}"))
            pickLrclib(results, durationMs)?.let { return it }
        }
        return null
    }

    private fun pickLrclib(results: JSONArray, durationMs: Long): JSONObject? {
        val target = durationMs / 1000.0
        return results.objects()
            // Several versions (album, radio edit, excerpt…): the duration breaks the tie, otherwise it's sure to be out of sync
            .filter { durationMs <= 0 || abs(it.optDouble("duration", 0.0) - target) <= 4 }
            .filter { it.optBoolean("instrumental") || it.hasText("syncedLyrics") || it.hasText("plainLyrics") }
            .minByOrNull {
                val synced = if (it.hasText("syncedLyrics")) 0 else 1000
                // Unknown duration: keep the search's own order rather than favoring the shortest version
                synced + if (durationMs > 0) abs(it.optDouble("duration", 0.0) - target) else 0.0
            }
    }

    // ── NetEase (unofficial API, very well stocked, French included) ──────────

    private suspend fun fromNetease(title: String, artist: String, durationMs: Long): JSONObject? {
        val q = enc("${cleanTitle(title)} ${firstArtist(artist)}")
        val songs = JSONObject(httpGet("https://music.163.com/api/search/get?s=$q&type=1&limit=10", NETEASE))
            .optJSONObject("result")?.optJSONArray("songs") ?: return null
        val wanted = cleanTitle(title).lowercase()
        val artists = artist.lowercase()
        val song = songs.objects()
            .filter { durationMs <= 0 || abs(it.optLong("duration") - durationMs) <= 4000 }
            .filter { s ->
                s.optJSONArray("artists")?.objects().orEmpty()
                    .any { a -> a.optString("name").lowercase().let { it.isNotEmpty() && (it in artists || artists in it) } }
            }
            // The exact title before "(Album Version)", "(Remix)"…
            .minByOrNull {
                (if (it.optString("name").lowercase() == wanted) 0 else 5_000) +
                    if (durationMs > 0) abs(it.optLong("duration") - durationMs) else 0
            }
            ?: return null

        val lrc = JSONObject(httpGet("https://music.163.com/api/song/lyric?id=${song.getLong("id")}&lv=1", NETEASE))
            .optJSONObject("lrc")?.optString("lyric").orEmpty()
            .replace(CREDITS, "")
        if (lrc.isBlank()) return null
        if ("纯音乐" in lrc) return JSONObject().put("instrumental", true) // "pure music, enjoy"
        val synced = parseLrc(lrc).any { it.timeMs > 0 && it.text.isNotEmpty() }
        return JSONObject().put(if (synced) "syncedLyrics" else "plainLyrics", lrc)
    }

    // ── Common ──────────────────────────────────────────────────────────────

    /**
     * A server or network error (LRCLIB sometimes returns 503 when overloaded) gets a second try.
     * A client error (4xx) fails right away: asking again won't change the answer.
     */
    private suspend fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String {
        repeat(2) { attempt ->
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "NoirMusic/1.0 (+https://github.com/BabasGames/noir-music)")
            headers.forEach(conn::setRequestProperty)
            val code = try {
                conn.responseCode.also { if (it == 200) return conn.inputStream.bufferedReader().readText() }
            } catch (e: IOException) {
                if (attempt == 1) throw e
                null
            } finally {
                conn.disconnect()
            }
            if (code != null && (code < 500 || attempt == 1)) throw IOException("HTTP $code")
            delay(1500)
        }
        error("unreachable")
    }

    private fun parse(o: JSONObject): Lyrics {
        // Old cache entries: no "source" field, they all came from LRCLIB
        val source = when (o.optString("source")) {
            "", LRCLIB -> LRCLIB
            else -> NETEASE_NAME // including the old lowercase "netease"
        }
        return when {
            o.optBoolean("instrumental") -> Lyrics.Instrumental
            o.hasText("syncedLyrics") -> parseLrc(o.getString("syncedLyrics"))
                .takeIf { it.isNotEmpty() }?.let { Lyrics.Synced(it, source) } ?: Lyrics.NotFound
            o.hasText("plainLyrics") -> Lyrics.Plain(
                o.getString("plainLyrics").lines().map { it.replace(STAMPS, "").trim() }.dropWhile { it.isEmpty() },
                source,
            )
            else -> Lyrics.NotFound
        }
    }

    private fun JSONArray.objects() = (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONObject.hasText(key: String) = !isNull(key) && optString(key).isNotBlank()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun sha1(s: String) =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private const val TAG = "NoirLyrics"
        const val LRCLIB = "LRCLIB"
        const val NETEASE_NAME = "NetEase"
        private val NETEASE = mapOf("User-Agent" to "Mozilla/5.0", "Referer" to "https://music.163.com")
        private val STAMP = Regex("""^\[(\d+):(\d{1,2}(?:[.:]\d{1,3})?)]""")
        private val STAMPS = Regex("""^(\[[^]]*])+""")
        /** NetEase credit lines: "[00:00.000] 作词 : …", "作曲", "制作人"… */
        private val CREDITS = Regex("""(?m)^(\[[^]]*])*\s*\p{IsHan}+\s*[:：].*$\n?""")

        /** "[01:02.34][02:10.00] text" → one line per timestamp, sorted. */
        fun parseLrc(lrc: String): List<LyricLine> = lrc.lines().flatMap { raw ->
            var rest = raw.trim()
            val times = mutableListOf<Long>()
            while (true) {
                val m = STAMP.find(rest) ?: break
                val (min, sec) = m.destructured
                times += min.toLong() * 60_000 + (sec.replace(':', '.').toDouble() * 1000).toLong()
                rest = rest.substring(m.range.last + 1)
            }
            times.map { LyricLine(it, rest.trim()) }
        }.sortedBy { it.timeMs }

        fun cleanTitle(t: String) = t
            .replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "")
            .substringBefore(" - ")
            .trim()
            .ifEmpty { t }

        fun firstArtist(a: String) = a.split(", ", " & ", " feat. ", " feat ", " ft. ", " x ", " / ").first().trim()
    }
}
