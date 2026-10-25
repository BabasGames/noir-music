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
import kotlin.math.abs

data class LyricLine(val timeMs: Long, val text: String)

sealed interface Lyrics {
    data object Loading : Lyrics
    data class Synced(val lines: List<LyricLine>) : Lyrics
    data class Plain(val lines: List<String>) : Lyrics
    data object Instrumental : Lyrics
    data object NotFound : Lyrics
    /** Aucun réseau du tout */
    data object Offline : Lyrics
    /** Réseau présent mais les sources ne répondent pas (surcharge, coupure…) */
    data object Unavailable : Lyrics
    data object MobileData : Lyrics
}

/**
 * Paroles depuis LRCLIB puis NetEase (gratuits, sans compte), gardées sur disque pour rouler hors ligne.
 * Le cache garde le format LRCLIB (syncedLyrics / plainLyrics / instrumental) quelle que soit la source.
 */
class LyricsRepository(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val dir = File(context.cacheDir, "lyrics")
    /** Introuvables pendant cette session : ne pas réinterroger à chaque ouverture */
    private val missing = mutableSetOf<String>()

    private val sources = listOf("LRCLIB" to ::fromLrclib, "NetEase" to ::fromNetease)

    suspend fun load(title: String, artist: String, durationMs: Long, allowMetered: Boolean): Lyrics =
        withContext(Dispatchers.IO) {
            val key = sha1("${artist.lowercase()}\n${title.lowercase()}")
            val file = File(dir, "$key.json")
            if (file.exists()) {
                try {
                    return@withContext parse(JSONObject(file.readText()))
                } catch (e: Exception) {
                    file.delete()
                }
            }
            if (key in missing) return@withContext Lyrics.NotFound
            if (cm.activeNetwork == null) return@withContext Lyrics.Offline
            if (cm.isActiveNetworkMetered && !allowMetered) return@withContext Lyrics.MobileData

            var failed = false
            for ((name, source) in sources) {
                val found = try {
                    source(title, artist, durationMs)
                } catch (e: Exception) {
                    Log.w(TAG, "$name : échec pour « $artist – $title »", e)
                    failed = true
                    null
                } ?: continue
                dir.mkdirs()
                file.writeText(found.toString())
                return@withContext parse(found)
            }
            // Une source en panne a peut-être les paroles : ne pas mémoriser l'échec, on réessaiera
            if (failed) return@withContext Lyrics.Unavailable
            missing += key
            Lyrics.NotFound
        }

    // ── LRCLIB ──────────────────────────────────────────────────────────────

    /** Essaie le titre exact, puis sans « (feat. …) », « - Remastered »… et avec le seul premier artiste. */
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
            // Plusieurs versions (album, radio edit, extrait…) : la durée départage, sinon décalage garanti
            .filter { durationMs <= 0 || abs(it.optDouble("duration", 0.0) - target) <= 4 }
            .filter { it.optBoolean("instrumental") || it.hasText("syncedLyrics") || it.hasText("plainLyrics") }
            .minByOrNull {
                val synced = if (it.hasText("syncedLyrics")) 0 else 1000
                synced + abs(it.optDouble("duration", 0.0) - target)
            }
    }

    // ── NetEase (API non officielle, très fournie, y compris en français) ─────

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
            // Le titre exact avant « (Album Version) », « (Remix) »…
            .minByOrNull { (if (it.optString("name").lowercase() == wanted) 0 else 5_000) + abs(it.optLong("duration") - durationMs) }
            ?: return null

        val lrc = JSONObject(httpGet("https://music.163.com/api/song/lyric?id=${song.getLong("id")}&lv=1", NETEASE))
            .optJSONObject("lrc")?.optString("lyric").orEmpty()
            .replace(CREDITS, "")
        if (lrc.isBlank()) return null
        if ("纯音乐" in lrc) return JSONObject().put("instrumental", true) // « musique pure, profitez-en »
        val synced = parseLrc(lrc).any { it.timeMs > 0 && it.text.isNotEmpty() }
        return JSONObject().put("source", "netease").put(if (synced) "syncedLyrics" else "plainLyrics", lrc)
    }

    // ── Commun ──────────────────────────────────────────────────────────────

    /** Une erreur serveur (LRCLIB renvoie parfois 503 quand il sature) a droit à un second essai. */
    private suspend fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String {
        repeat(2) { attempt ->
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Noir/1.0 (controleur musique Android perso)")
            headers.forEach(conn::setRequestProperty)
            try {
                val code = conn.responseCode
                if (code == 200) return conn.inputStream.bufferedReader().readText()
                if (code < 500 || attempt == 1) throw IOException("HTTP $code")
            } catch (e: IOException) {
                if (attempt == 1) throw e
            } finally {
                conn.disconnect()
            }
            delay(1500)
        }
        error("inatteignable")
    }

    private fun parse(o: JSONObject): Lyrics = when {
        o.optBoolean("instrumental") -> Lyrics.Instrumental
        o.hasText("syncedLyrics") -> parseLrc(o.getString("syncedLyrics"))
            .takeIf { it.isNotEmpty() }?.let(Lyrics::Synced) ?: Lyrics.NotFound
        o.hasText("plainLyrics") -> Lyrics.Plain(
            o.getString("plainLyrics").lines().map { it.replace(STAMPS, "").trim() }.dropWhile { it.isEmpty() },
        )
        else -> Lyrics.NotFound
    }

    private fun JSONArray.objects() = (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONObject.hasText(key: String) = !isNull(key) && optString(key).isNotBlank()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun sha1(s: String) =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private const val TAG = "NoirParoles"
        private val NETEASE = mapOf("User-Agent" to "Mozilla/5.0", "Referer" to "https://music.163.com")
        private val STAMP = Regex("""^\[(\d+):(\d{1,2}(?:[.:]\d{1,3})?)]""")
        private val STAMPS = Regex("""^(\[[^]]*])+""")
        /** Lignes de crédits NetEase : « [00:00.000] 作词 : … », « 作曲 », « 制作人 »… */
        private val CREDITS = Regex("""(?m)^(\[[^]]*])*\s*\p{IsHan}+\s*[:：].*$\n?""")

        /** « [01:02.34][02:10.00] texte » → une ligne par horodatage, triées. */
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
