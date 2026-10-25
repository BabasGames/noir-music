package fr.babas.noir

import android.content.ComponentName
import android.content.Context
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

data class NowPlaying(
    val controller: MediaController? = null,
    val appName: String = "",
    val title: String = "",
    val artist: String = "",
    val art: Bitmap? = null,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    /** SystemClock.elapsedRealtime() au moment où positionMs a été mesurée */
    val positionTime: Long = 0,
    val speed: Float = 1f,
    val isPlaying: Boolean = false,
    val likeAction: PlaybackState.CustomAction? = null,
    /** Icône fournie par l'appli source : son dessin (cœur plein/vide) reflète l'état réel */
    val likeIcon: Drawable? = null,
) {
    fun currentPosition(): Long {
        if (!isPlaying) return positionMs
        val p = positionMs + ((SystemClock.elapsedRealtime() - positionTime) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p.coerceAtLeast(0)
    }
}

/** Suit les sessions média actives (Deezer, Spotify…) et expose celle à piloter. */
class MediaRepository(private val context: Context) {
    private val msm = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, MediaListenerService::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state

    private var controllers: List<MediaController> = emptyList()
    private var current: MediaController? = null

    // Caches : éviter de redécoder pochette / icône à chaque tick de PlaybackState
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

    /** @return false si l'accès aux notifications n'est pas accordé */
    fun start(): Boolean = try {
        msm.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
        setControllers(msm.getActiveSessions(component))
        true
    } catch (e: SecurityException) {
        false
    }

    fun stop() {
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
     * Choisit la session à afficher quand plusieurs applis en ont une.
     * La liste d'Android est triée par priorité (la plus récemment active d'abord).
     */
    private fun pickSession(list: List<MediaController>, previous: MediaController?): MediaController? {
        list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }?.let { return it }
        // Rien ne joue : rester sur la même appli plutôt que sauter vers une autre session en pause
        list.firstOrNull { it.sessionToken == previous?.sessionToken }?.let { return it }
        return list.firstOrNull()
    }

    private fun refresh() {
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
        val s = "${a.action} ${a.name}".lowercase()
        if (listOf("dislike", "thumb_down", "thumbs_down", "ban").any { it in s }) return false
        return listOf("like", "favorite", "favourite", "love", "heart", "favori", "coeur", "cœur", "j'aime")
            .any { it in s }
    }

    private fun appLabel(pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
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
        artCache = try {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use(BitmapFactory::decodeStream)
        } catch (e: Exception) {
            null // URI souvent non lisible depuis une autre appli
        }
        return artCache
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
}
