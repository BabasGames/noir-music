package fr.babas.noir

import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

val Gray = Color(0xFF9A9A9A)
val Dim = Color(0xFF3A3A3A)

@Composable
fun noirSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = Color.White,
    activeTrackColor = Color.White,
    inactiveTrackColor = Color(0xFF2A2A2A),
    disabledThumbColor = Dim,
    disabledActiveTrackColor = Dim,
    disabledInactiveTrackColor = Color(0xFF1A1A1A),
)

@Composable
fun PlayerScreen(np: NowPlaying, prefs: Prefs, onOpenSettings: () -> Unit) {
    val current by rememberUpdatedState(np)
    val shift = rememberBurnInShift(prefs.antiBurn)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var dragX by remember { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Glisser à gauche = suivant, à droite = précédent (les sliders consomment leurs propres gestes)
            .pointerInput(Unit) {
                val threshold = 96.dp.toPx()
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val tc = current.controller?.transportControls
                        if (dragX < -threshold) tc?.skipToNext() else if (dragX > threshold) tc?.skipToPrevious()
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f },
                ) { change, dx ->
                    change.consume()
                    dragX += dx
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .offset { IntOffset(shift.x.roundToPx() + (dragX * 0.3f).roundToInt(), shift.y.roundToPx()) }
            .padding(if (landscape) 16.dp else 24.dp),
    ) {
        if (landscape) LandscapeLayout(np, prefs, onOpenSettings) else PortraitLayout(np, prefs, onOpenSettings)
    }
}

@Composable
private fun PortraitLayout(np: NowPlaying, prefs: Prefs, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header(np.appName, bigClock = !prefs.showArt, onOpenSettings = onOpenSettings)
        if (prefs.showArt) {
            // La boîte prend tout l'espace libre ; la pochette y tient en carré le plus grand possible
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Artwork(np.art, prefs.grayArt, Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true))
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Controls(np)
    }
}

@Composable
private fun LandscapeLayout(np: NowPlaying, prefs: Prefs, onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        if (prefs.showArt) {
            Artwork(np.art, prefs.grayArt, Modifier.fillMaxHeight().aspectRatio(1f))
            Spacer(Modifier.width(32.dp))
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Header(np.appName, bigClock = !prefs.showArt, onOpenSettings = onOpenSettings)
            Spacer(Modifier.weight(1f))
            Controls(np, compact = true)
        }
    }
}

@Composable
private fun Header(appName: String, bigClock: Boolean, onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                rememberClock(),
                fontSize = if (bigClock) 88.sp else 44.sp,
                fontWeight = FontWeight.Thin,
                color = Color.White,
                lineHeight = if (bigClock) 92.sp else 48.sp,
            )
            if (appName.isNotEmpty()) Text(appName, fontSize = 15.sp, color = Gray)
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Settings, "Paramètres", tint = Dim, modifier = Modifier.size(26.dp))
        }
    }
}

/** Titre, progression, boutons, volume : la moitié basse en portrait, la droite en paysage. */
@Composable
private fun ColumnScope.Controls(np: NowPlaying, compact: Boolean = false) {
    val tc = np.controller?.transportControls
    val togglePlay = { if (np.isPlaying) tc?.pause() else tc?.play() }

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { togglePlay() },
            ),
    ) {
        Text(
            np.title.ifEmpty { "Rien en lecture" },
            fontSize = if (compact) 26.sp else 30.sp,
            lineHeight = if (compact) 30.sp else 36.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            np.artist.ifEmpty { if (np.controller == null) "Lance la musique dans Deezer, Spotify…" else "" },
            fontSize = 19.sp,
            color = Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(Modifier.height(if (compact) 4.dp else 12.dp))
    Progress(np)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val enabled = np.controller != null
        LikeButton(np)
        CtrlButton(Icons.Rounded.SkipPrevious, "Précédent", 48.dp, enabled) { tc?.skipToPrevious() }
        CtrlButton(
            if (np.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (np.isPlaying) "Pause" else "Lecture",
            if (compact) 64.dp else 76.dp,
            enabled,
        ) { togglePlay() }
        CtrlButton(Icons.Rounded.SkipNext, "Suivant", 48.dp, enabled) { tc?.skipToNext() }
        Spacer(Modifier.size(56.dp)) // équilibre visuel avec le ♥
    }
    VolumeRow()
}

@Composable
private fun CtrlButton(icon: ImageVector, desc: String, size: Dp, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(size + 20.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, Modifier.size(size), tint = if (enabled) Color.White else Dim)
    }
}

@Composable
private fun LikeButton(np: NowPlaying) {
    val action = np.likeAction
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(enabled = action != null) {
                np.controller?.transportControls?.sendCustomAction(action!!, action.extras)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (action == null) return@Box
        val icon = remember(np.likeIcon) { np.likeIcon?.toBitmap(96, 96)?.asImageBitmap() }
        if (icon != null) {
            // Icône de l'appli source, recolorée en blanc : la forme pleine/vide indique l'état
            Image(icon, action.name.toString(), Modifier.size(28.dp), colorFilter = ColorFilter.tint(Color.White))
        } else {
            Icon(Icons.Rounded.FavoriteBorder, action.name.toString(), Modifier.size(28.dp), tint = Color.White)
        }
    }
}

@Composable
private fun Progress(np: NowPlaying) {
    val dur = np.durationMs
    var pos by remember { mutableLongStateOf(np.currentPosition()) }
    var drag by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(np) {
        while (true) {
            pos = np.currentPosition()
            if (!np.isPlaying) break
            delay(1000 - pos % 1000 + 5) // se caler sur la seconde suivante
        }
    }

    val shown = drag?.let { (it * dur).toLong() } ?: pos
    Slider(
        value = if (dur > 0) (shown.toFloat() / dur).coerceIn(0f, 1f) else 0f,
        onValueChange = { drag = it },
        onValueChangeFinished = {
            drag?.let {
                val target = (it * dur).toLong()
                np.controller?.transportControls?.seekTo(target)
                pos = target
            }
            drag = null
        },
        enabled = dur > 0,
        colors = noirSliderColors(),
        modifier = Modifier.height(32.dp),
    )
    Row(Modifier.fillMaxWidth()) {
        Text(formatTime(shown), fontSize = 13.sp, color = Gray)
        Spacer(Modifier.weight(1f))
        Text(if (dur > 0) formatTime(dur) else "", fontSize = 13.sp, color = Gray)
    }
}

@Composable
private fun VolumeRow() {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val max = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var vol by remember { mutableIntStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    // Suit aussi les touches volume physiques (le volume est persisté dans Settings.System)
    DisposableEffect(Unit) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                vol = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    fun set(v: Int) {
        vol = v.coerceIn(0, max)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { set(vol - 1) }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.VolumeDown, "Baisser", tint = Gray)
        }
        Slider(
            value = vol.toFloat(),
            onValueChange = { set(it.roundToInt()) },
            valueRange = 0f..max.toFloat(),
            colors = noirSliderColors(),
            modifier = Modifier.weight(1f).height(32.dp),
        )
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { set(vol + 1) }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Monter", tint = Gray)
        }
    }
}

@Composable
private fun Artwork(art: Bitmap?, gray: Boolean, modifier: Modifier) {
    val img = remember(art) { art?.asImageBitmap() }
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF0C0C0C)),
        contentAlignment = Alignment.Center,
    ) {
        if (img != null) {
            Image(
                img, null, Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = if (gray) Grayscale else null,
            )
        } else {
            Icon(Icons.Rounded.MusicNote, null, Modifier.size(64.dp), tint = Dim)
        }
    }
}

private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun rememberClock(): String {
    val context = LocalContext.current
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    var text by remember { mutableStateOf("") }
    LaunchedEffect(pattern) {
        val fmt = SimpleDateFormat(pattern, Locale.getDefault())
        while (true) {
            val now = System.currentTimeMillis()
            text = fmt.format(Date(now))
            delay(60_000 - now % 60_000) // réveil pile au changement de minute
        }
    }
    return text
}

/** Décalage aléatoire de ±10 dp chaque minute pour ne pas marquer l'AMOLED. */
@Composable
private fun rememberBurnInShift(enabled: Boolean): DpOffset {
    var shift by remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
    LaunchedEffect(enabled) {
        shift = DpOffset(0.dp, 0.dp)
        while (enabled) {
            delay(60_000)
            shift = DpOffset(Random.nextInt(-10, 11).dp, Random.nextInt(-10, 11).dp)
        }
    }
    return shift
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
    else "%d:%02d".format(s / 60, s % 60)
}
