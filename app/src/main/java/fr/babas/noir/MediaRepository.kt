package fr.babas.noir

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

data class NowPlaying(
    val controller: MediaController? = null,
    val appName: String = "",
    val title: String = "",
    val artist: String = "",
    val art: Bitmap? = null,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    /** SystemClock.elapsedRealtime() at the moment positionMs was measured */
    val positionTime: Long = 0,
    val speed: Float = 1f,
    val isPlaying: Boolean = false,
    val likeAction: PlaybackState.CustomAction? = null,
    /** Icon supplied by the source app: its drawing (filled/empty heart) reflects the real state */
    val likeIcon: Drawable? = null,
) {
    fun currentPosition(): Long {
        if (!isPlaying) return positionMs
        val p = positionMs + ((SystemClock.elapsedRealtime() - positionTime) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p.coerceAtLeast(0)
    }
}

/** An app that has a media session (playing or paused), for the source picker. */
data class SessionInfo(val pkg: String, val appName: String, val title: String, val isPlaying: Boolean)

/** Tracks the active media sessions (Deezer, Spotify…) and exposes the one to control. */
class MediaRepository(private val context: Context, private val preferredApp: () -> String) {
    private val msm = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, MediaListenerService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    /** Artwork decoding, off the UI thread */
    private val decoder = Executors.newSingleThreadExecutor()
    private var listening = false

    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state

    private val _sessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    val sessions: StateFlow<List<SessionInfo>> = _sessions

    /** Picked by hand in the picker: overrides everything as long as its session exists */
    private var manualPkg: String? = null
    private val labels = mutableMapOf<String, String>()

    private var controllers: List<MediaController> = emptyList()
    private var current: MediaController? = null

    // Caches: avoid decoding the artwork / icon again on every PlaybackState tick
    private var artKey: Any? = null
    private var artCache: Bitmap? = null
    private var iconKey: Pair<String, Int>? = null
    private var iconCache: Drawable? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        setControllers(list.orEmpty())
    }

    /** @return false if notification access isn't granted */
    fun start(): Boolean = try {
        // Still listening if the sleep timer kept us connected in the background
        if (!listening) {
            msm.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
            listening = true
        }
        setControllers(msm.getActiveSessions(component))
        true
    } catch (e: SecurityException) {
        false
    }

    fun stop() {
        if (!listening) return
        listening = false
        msm.removeOnActiveSessionsChangedListener(sessionsListener)
        controllers.forEach { it.unregisterCallback(callback) }
        controllers = emptyList()
    }

    private fun setControllers(list: List<MediaController>) {
        controllers.forEach { it.unregisterCallback(callback) }
        controllers = list
        list.forEach { it.registerCallback(callback, handler) }
        refresh()
    }

    /**
     * Picks the session to show when several apps have one.
     * Android's list is sorted by priority (most recently active first).
     */
    private fun pickSession(list: List<MediaController>, previous: MediaController?): MediaController? {
        manualPkg?.let { m ->
            list.firstOrNull { it.packageName == m }?.let { return it }
            manualPkg = null // session gone: back to automatic choice
        }
        val pref = preferredApp()
        if (pref.isNotEmpty()) list.firstOrNull { it.packageName == pref }?.let { return it }
        list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }?.let { return it }
        // Nothing playing: stay on the same app rather than jumping to another paused session
        list.firstOrNull { it.sessionToken == previous?.sessionToken }?.let { return it }
        return list.firstOrNull()
    }

    /** Apps offered as "preferred app": those exposing a media service + current sessions. */
    fun candidateApps(): List<Pair<String, String>> {
        val pm = context.packageManager
        val browsers = pm.queryIntentServices(Intent("android.media.browse.MediaBrowserService"), 0)
            .map { it.serviceInfo.packageName }
        return (browsers + controllers.map { it.packageName })
            .distinct()
            .filter { it != context.packageName }
            .map { it to appLabel(it) }
            .sortedBy { it.second.lowercase() }
    }

    fun choose(pkg: String) {
        manualPkg = pkg
        refresh()
    }

    fun refresh() {
        _sessions.value = controllers.distinctBy { it.packageName }.map {
            SessionInfo(
                it.packageName,
                appLabel(it.packageName),
                it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                it.playbackState?.state == PlaybackState.STATE_PLAYING,
            )
        }
        val c = pickSession(controllers, current)
        current = c
        if (c == null) {
            _state.value = NowPlaying()
            return
        }
        val md = c.metadata
        val ps = c.playbackState
        val like = ps?.customActions?.firstOrNull(::isLikeAction)
        _state.value = NowPlaying(
            controller = c,
            appName = appLabel(c.packageName),
            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty(),
            artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty(),
            art = loadArt(md),
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0,
            positionMs = ps?.position ?: 0,
            positionTime = ps?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = ps?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            isPlaying = ps?.state == PlaybackState.STATE_PLAYING,
            likeAction = like,
            likeIcon = like?.let { loadIcon(c.packageName, it.icon) },
        )
    }

    private fun isLikeAction(a: PlaybackState.CustomAction): Boolean {
        // Whole words ("ACTION_LIKE", "toggleFavorite", "Thumbs up"): "ban" mustn't match "banner"
        val words = "${a.action} ${a.name}"
            .replace(CAMEL_CASE, " ")
            .lowercase()
            .split(NOT_LETTER)
            .filter { it.isNotEmpty() }
        if (words.any { it in DISLIKE }) return false
        if ("thumb" in words || "thumbs" in words) return "up" in words
        return words.any { w -> LIKE.any { w.startsWith(it) } }
    }

    private fun appLabel(pkg: String): String = labels.getOrPut(pkg) {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun loadArt(md: MediaMetadata?): Bitmap? {
        if (md == null) return null
        val bmp = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (bmp != null) return bmp
        val uri = md.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: md.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: return null
        if (uri == artKey) return artCache
        artKey = uri
        artCache = null
        decoder.execute {
            val bmp = decodeScaled(Uri.parse(uri))
            handler.post {
                // Still the same track: show it
                if (artKey == uri) {
                    artCache = bmp
                    refresh()
                }
            }
        }
        return null
    }

    /** Scaled down to at most ART_MAX_PX: a 4000 px cover would take 64 MB for a 400 dp square. */
    private fun decodeScaled(uri: Uri): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= ART_MAX_PX) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Exception) {
        null // URI often unreadable from another app
    }

    private fun loadIcon(pkg: String, res: Int): Drawable? {
        val key = pkg to res
        if (key == iconKey) return iconCache
        iconKey = key
        iconCache = try {
            context.packageManager.getResourcesForApplication(pkg).getDrawable(res, null)
        } catch (e: Exception) {
            null
        }
        return iconCache
    }

    private companion object {
        const val ART_MAX_PX = 1024
        val CAMEL_CASE = Regex("(?<=\\p{Ll})(?=\\p{Lu})")
        val NOT_LETTER = Regex("[^\\p{L}']+")
        val DISLIKE = setOf("dislike", "disliked", "ban", "banned", "down")
        /** Prefixes: "liked", "favorites", "loved"… */
        val LIKE = listOf("like", "favorit", "favourit", "love", "heart", "favori", "coeur", "cœur", "j'aime")
    }
}
