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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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

/** Couleurs du lecteur ; la variante « nuit » atténue tout le blanc. */
data class Palette(val fg: Color, val fg2: Color, val faint: Color, val track: Color)

private val DayPalette = Palette(Color.White, Gray, Dim, Color(0xFF2A2A2A))
private val NightPalette = Palette(Color(0xFF8C8C8C), Color(0xFF555555), Color(0xFF2A2A2A), Color(0xFF1E1E1E))

val LocalPalette = staticCompositionLocalOf { DayPalette }

@Composable
fun noirSliderColors(pal: Palette = LocalPalette.current): SliderColors = SliderDefaults.colors(
    thumbColor = pal.fg,
    activeTrackColor = pal.fg,
    inactiveTrackColor = pal.track,
    disabledThumbColor = pal.faint,
    disabledActiveTrackColor = pal.faint,
    disabledInactiveTrackColor = Color(0xFF141414),
)

@Composable
fun PlayerScreen(np: NowPlaying, prefs: Prefs, onOpenSettings: () -> Unit, onOpenSource: () -> Unit) {
    val current by rememberUpdatedState(np)
    val shift = rememberBurnInShift(prefs.antiBurn)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var dragX by remember { mutableFloatStateOf(0f) }

    CompositionLocalProvider(LocalPalette provides if (prefs.dimText) NightPalette else DayPalette) {
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
            val header: @Composable (Boolean) -> Unit = { withGear ->
                Header(np.appName, prefs, landscape, if (withGear) onOpenSettings else null, onOpenSource)
            }
            if (landscape) LandscapeLayout(np, prefs, header) else PortraitLayout(np, prefs, header, onOpenSettings)
        }
    }
}

@Composable
private fun PortraitLayout(
    np: NowPlaying,
    prefs: Prefs,
    header: @Composable (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (!prefs.showArt) {
        // Sans pochette : horloge + contrôles forment un seul bloc centré, la roue reste dans le coin
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                header(false)
                Spacer(Modifier.height(40.dp))
                Controls(np, prefs)
            }
            SettingsButton(Modifier.align(Alignment.TopEnd), onOpenSettings)
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        header(true)
        val art: @Composable () -> Unit = {
            // La boîte prend tout l'espace libre ; la pochette y tient en carré le plus grand possible
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Artwork(np.art, prefs.grayArt, Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true))
            }
        }
        if (prefs.swapArt) {
            Spacer(Modifier.height(16.dp))
            Controls(np, prefs)
            art()
        } else {
            art()
            Controls(np, prefs)
        }
    }
}

@Composable
private fun LandscapeLayout(np: NowPlaying, prefs: Prefs, header: @Composable (Boolean) -> Unit) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        val art: @Composable () -> Unit = {
            Artwork(np.art, prefs.grayArt, Modifier.fillMaxHeight().aspectRatio(1f))
        }
        if (prefs.showArt && !prefs.swapArt) {
            art()
            Spacer(Modifier.width(32.dp))
        }
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = if (prefs.showArt) Arrangement.SpaceBetween else Arrangement.Center,
        ) {
            header(true)
            if (!prefs.showArt) Spacer(Modifier.height(16.dp))
            Column { Controls(np, prefs, compact = true) }
        }
        if (prefs.showArt && prefs.swapArt) {
            Spacer(Modifier.width(32.dp))
            art()
        }
    }
}

@Composable
private fun Header(
    appName: String,
    prefs: Prefs,
    landscape: Boolean,
    onOpenSettings: (() -> Unit)?,
    onOpenSource: () -> Unit,
) {
    val pal = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            val clockSp = when (prefs.clockSize) {
                Prefs.CLOCK_BIG -> if (landscape) 72f else 88f
                Prefs.CLOCK_MEDIUM -> 44f
                else -> 0f
            }
            if (clockSp > 0f) {
                Text(
                    rememberClock(),
                    fontSize = clockSp.sp,
                    lineHeight = (clockSp * 1.05f).sp,
                    fontWeight = FontWeight.Thin,
                    color = pal.fg,
                )
            }
            if (prefs.showSource && appName.isNotEmpty()) {
                Text(
                    appName,
                    fontSize = 15.sp,
                    color = pal.fg2,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onOpenSource),
                )
            }
        }
        if (onOpenSettings != null) SettingsButton(Modifier, onOpenSettings)
    }
}

@Composable
private fun SettingsButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Settings, "Paramètres", tint = LocalPalette.current.faint, modifier = Modifier.size(26.dp))
    }
}

/** Titre, progression, boutons, volume : le bloc « contrôles ». */
@Composable
private fun ColumnScope.Controls(np: NowPlaying, prefs: Prefs, compact: Boolean = false) {
    val pal = LocalPalette.current
    val tc = np.controller?.transportControls
    val togglePlay = { if (np.isPlaying) tc?.pause() else tc?.play() }
    val scale = prefs.textScale
    val titleSp = (if (compact) 26f else 30f) * scale

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
            fontSize = titleSp.sp,
            lineHeight = (titleSp * 1.2f).sp,
            fontWeight = FontWeight.Light,
            color = pal.fg,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            np.artist.ifEmpty { if (np.controller == null) "Lance la musique dans Deezer, Spotify…" else "" },
            fontSize = (19f * scale).sp,
            color = pal.fg2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (prefs.showProgress) {
        Spacer(Modifier.height(if (compact) 4.dp else 12.dp))
        Progress(np)
    } else {
        Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
    }

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
    if (prefs.showVolume) VolumeRow()
}

@Composable
private fun CtrlButton(icon: ImageVector, desc: String, size: Dp, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(size + 20.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val pal = LocalPalette.current
        Icon(icon, desc, Modifier.size(size), tint = if (enabled) pal.fg else pal.faint)
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
            Image(icon, action.name.toString(), Modifier.size(28.dp), colorFilter = ColorFilter.tint(LocalPalette.current.fg))
        } else {
            Icon(Icons.Rounded.FavoriteBorder, action.name.toString(), Modifier.size(28.dp), tint = LocalPalette.current.fg)
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
        val pal = LocalPalette.current
        Text(formatTime(shown), fontSize = 13.sp, color = pal.fg2)
        Spacer(Modifier.weight(1f))
        Text(if (dur > 0) formatTime(dur) else "", fontSize = 13.sp, color = pal.fg2)
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
            Icon(Icons.AutoMirrored.Rounded.VolumeDown, "Baisser", tint = LocalPalette.current.fg2)
        }
        Slider(
            value = vol.toFloat(),
            onValueChange = { set(it.roundToInt()) },
            valueRange = 0f..max.toFloat(),
            colors = noirSliderColors(),
            modifier = Modifier.weight(1f).height(32.dp),
        )
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { set(vol + 1) }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Monter", tint = LocalPalette.current.fg2)
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
            Icon(Icons.Rounded.MusicNote, null, Modifier.size(64.dp), tint = LocalPalette.current.faint)
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
