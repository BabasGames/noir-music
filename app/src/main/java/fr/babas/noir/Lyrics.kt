package fr.babas.noir

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
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
    data object Unavailable : Lyrics
    data object MobileData : Lyrics
}

/** Paroles depuis LRCLIB (gratuit, sans compte), gardées sur disque pour rouler hors ligne. */
class LyricsRepository(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val dir = File(context.cacheDir, "lyrics")
    /** Introuvables pendant cette session : ne pas réinterroger à chaque ouverture */
    private val missing = mutableSetOf<String>()

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
            if (cm.activeNetwork == null) return@withContext Lyrics.Unavailable
            if (cm.isActiveNetworkMetered && !allowMetered) return@withContext Lyrics.MobileData

            val best = try {
                find(title, artist, durationMs)
            } catch (e: Exception) {
                return@withContext Lyrics.Unavailable
            }
            if (best == null) {
                missing += key
                return@withContext Lyrics.NotFound
            }
            dir.mkdirs()
            file.writeText(best.toString())
            parse(best)
        }

    /** Essaie le titre exact, puis sans « (feat. …) », « - Remastered »… et avec le seul premier artiste. */
    private fun find(title: String, artist: String, durationMs: Long): JSONObject? {
        val queries = listOf(title to artist, cleanTitle(title) to firstArtist(artist)).distinct()
        for ((t, a) in queries) {
            pickBest(search(t, a), durationMs)?.let { return it }
        }
        return null
    }

    private fun pickBest(results: JSONArray, durationMs: Long): JSONObject? {
        val target = durationMs / 1000.0
        return (0 until results.length())
            .map { results.getJSONObject(it) }
            // Plusieurs versions (album, radio edit, extrait…) : la durée départage, sinon décalage garanti
            .filter { durationMs <= 0 || abs(it.optDouble("duration", 0.0) - target) <= 4 }
            .filter { it.optBoolean("instrumental") || it.hasText("syncedLyrics") || it.hasText("plainLyrics") }
            .minByOrNull {
                val synced = if (it.hasText("syncedLyrics")) 0 else 1000
                synced + abs(it.optDouble("duration", 0.0) - target)
            }
    }

    private fun search(title: String, artist: String): JSONArray {
        val q = "track_name=${enc(title)}&artist_name=${enc(artist)}"
        val conn = URL("https://lrclib.net/api/search?$q").openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "Noir/1.0 (controleur musique Android perso)")
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return JSONArray(conn.inputStream.bufferedReader().readText())
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(o: JSONObject): Lyrics = when {
        o.optBoolean("instrumental") -> Lyrics.Instrumental
        o.hasText("syncedLyrics") -> parseLrc(o.getString("syncedLyrics"))
            .takeIf { it.isNotEmpty() }?.let(Lyrics::Synced) ?: Lyrics.NotFound
        o.hasText("plainLyrics") -> Lyrics.Plain(o.getString("plainLyrics").trim().lines())
        else -> Lyrics.NotFound
    }

    private fun JSONObject.hasText(key: String) = !isNull(key) && optString(key).isNotBlank()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun sha1(s: String) =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private val STAMP = Regex("""^\[(\d+):(\d{1,2}(?:[.:]\d{1,3})?)]""")

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
