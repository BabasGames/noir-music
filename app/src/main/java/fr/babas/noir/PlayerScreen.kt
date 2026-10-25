package fr.babas.noir

import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
data class Palette(val fg: Color, val fg2: Color, val faint: Color, val track: Color, val artLevel: Float)

private val DayPalette = Palette(Color.White, Gray, Dim, Color(0xFF2A2A2A), artLevel = 1f)
// La pochette est assombrie dans la même proportion que le blanc du texte (0x8C ≈ 55 %)
private val NightPalette = Palette(Color(0xFF8C8C8C), Color(0xFF555555), Color(0xFF2A2A2A), Color(0xFF1E1E1E), artLevel = 0.55f)

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

/** Paroles telles que les voient les dispositions ; null quand la fonction est coupée dans les réglages. */
private class LyricsUi(val open: Boolean, val state: Lyrics, val toggle: () -> Unit)

@OptIn(ExperimentalSharedTransitionApi::class)
private class SharedScopes(val layout: SharedTransitionScope, val content: AnimatedVisibilityScope)

private val LocalShared = staticCompositionLocalOf<SharedScopes?> { null }

/** Élément qui « voyage » entre l'écran normal et l'écran paroles (pochette, horloge, appli). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.shared(key: String, scale: Boolean = false): Modifier {
    val s = LocalShared.current ?: return this
    return with(s.layout) {
        val state = rememberSharedContentState(key)
        // Le texte ne se re-mesure pas en douceur d'une taille de police à l'autre : on le met à l'échelle
        if (scale) this@shared.sharedBounds(state, s.content, resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds())
        else this@shared.sharedElement(state, s.content)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerScreen(
    np: NowPlaying,
    prefs: Prefs,
    lyricsRepo: LyricsRepository,
    onOpenSettings: () -> Unit,
    onOpenSource: () -> Unit,
) {
    val current by rememberUpdatedState(np)
    val burnTarget = rememberBurnInShift(
        prefs.antiBurn,
        Prefs.BURN_INTERVALS.getOrElse(prefs.burnInterval) { Prefs.BURN_INTERVALS[1] }.first * 1000L,
        Prefs.BURN_AMPLITUDES.getOrElse(prefs.burnAmplitude) { Prefs.BURN_AMPLITUDES[1] }.first,
    )
    // Glisse vers la nouvelle position au lieu de sauter ; lu dans offset {} : pas de recomposition par image
    val shift by animateValueAsState(burnTarget, DpOffset.VectorConverter, tween(1800, easing = FastOutSlowInEasing), label = "décalage")
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var lyricsOpen by rememberSaveable { mutableStateOf(false) }
    val showLyrics = prefs.lyrics && lyricsOpen
    val lyrics = rememberLyrics(lyricsRepo, np, showLyrics, prefs.lyricsOnMobile)

    CompositionLocalProvider(LocalPalette provides if (prefs.dimText) NightPalette else DayPalette) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                // Gauche/droite = suivant/précédent, haut/bas = paroles (les sliders consomment leurs propres gestes)
                .pointerInput(Unit) {
                    val threshold = 96.dp.toPx()
                    detectDragGestures(
                        onDragEnd = {
                            if (abs(dragX) > abs(dragY)) {
                                val tc = current.controller?.transportControls
                                if (dragX < -threshold) tc?.skipToNext() else if (dragX > threshold) tc?.skipToPrevious()
                            } else if (prefs.lyrics) {
                                if (dragY < -threshold) lyricsOpen = true else if (dragY > threshold) lyricsOpen = false
                            }
                            dragX = 0f
                            dragY = 0f
                        },
                        onDragCancel = { dragX = 0f; dragY = 0f },
                    ) { change, amount ->
                        change.consume()
                        dragX += amount.x
                        dragY += amount.y
                    }
                }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .offset {
                    val follow = if (abs(dragX) > abs(dragY)) (dragX * 0.3f).roundToInt() else 0
                    IntOffset(shift.x.roundToPx() + follow, shift.y.roundToPx())
                }
                .padding(if (landscape) 16.dp else 24.dp),
        ) {
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                AnimatedContent(
                    showLyrics,
                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                    label = "paroles",
                ) { open ->
                    CompositionLocalProvider(LocalShared provides SharedScopes(this@SharedTransitionLayout, this)) {
                        val lyr = if (prefs.lyrics) LyricsUi(open, lyrics) { lyricsOpen = !lyricsOpen } else null
                        val header: @Composable (Boolean) -> Unit = { withGear ->
                            Header(np, prefs, landscape, compact = open, if (withGear) onOpenSettings else null, onOpenSource)
                        }
                        if (landscape) LandscapeLayout(np, prefs, lyr, header)
                        else PortraitLayout(np, prefs, lyr, header, onOpenSettings)
                    }
                }
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    np: NowPlaying,
    prefs: Prefs,
    lyr: LyricsUi?,
    header: @Composable (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val swap = prefs.showArt && prefs.swapArt
    if (lyr?.open == true) {
        // Paroles : pochette et horloge réduites dans l'en-tête, les paroles prennent l'espace libéré
        Column(Modifier.fillMaxSize()) {
            header(true)
            val panel: @Composable () -> Unit = {
                LyricsPanel(np, lyr.state, prefs, Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp))
            }
            if (swap) {
                Spacer(Modifier.height(16.dp))
                Controls(np, prefs, lyr = lyr)
                panel()
            } else {
                panel()
                Controls(np, prefs, lyr = lyr)
            }
        }
        return
    }
    if (!prefs.showArt) {
        // Sans pochette : horloge + contrôles forment un seul bloc centré, la roue reste dans le coin
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                header(false)
                Spacer(Modifier.height(40.dp))
                Controls(np, prefs, lyr = lyr)
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
                Artwork(np.art, prefs.grayArt, Modifier.shared("art").aspectRatio(1f, matchHeightConstraintsFirst = true))
            }
        }
        if (swap) {
            Spacer(Modifier.height(16.dp))
            Controls(np, prefs, lyr = lyr)
            art()
        } else {
            art()
            Controls(np, prefs, lyr = lyr)
        }
    }
}

@Composable
private fun LandscapeLayout(np: NowPlaying, prefs: Prefs, lyr: LyricsUi?, header: @Composable (Boolean) -> Unit) {
    val open = lyr?.open == true
    val swap = prefs.showArt && prefs.swapArt
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        // Les paroles prennent la place de la pochette (ou la moitié de l'écran s'il n'y en a pas)
        val side: @Composable () -> Unit = {
            if (open) LyricsPanel(np, lyr!!.state, prefs, Modifier.weight(1f).fillMaxHeight())
            else Artwork(np.art, prefs.grayArt, Modifier.shared("art").fillMaxHeight().aspectRatio(1f))
        }
        val hasSide = open || prefs.showArt
        if (hasSide && !swap) {
            side()
            Spacer(Modifier.width(32.dp))
        }
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = if (hasSide) Arrangement.SpaceBetween else Arrangement.Center,
        ) {
            header(true)
            if (!hasSide) Spacer(Modifier.height(16.dp))
            Column { Controls(np, prefs, compact = true, lyr = lyr) }
        }
        if (hasSide && swap) {
            Spacer(Modifier.width(32.dp))
            side()
        }
    }
}

@Composable
private fun Header(
    np: NowPlaying,
    prefs: Prefs,
    landscape: Boolean,
    compact: Boolean,
    onOpenSettings: (() -> Unit)?,
    onOpenSource: () -> Unit,
) {
    val pal = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top) {
        // Écran paroles : la pochette devient une vignette à côté de l'heure
        if (compact && prefs.showArt) {
            Artwork(np.art, prefs.grayArt, Modifier.shared("art").size(if (landscape) 64.dp else 76.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            val clockSp = when (prefs.clockSize) {
                Prefs.CLOCK_BIG -> if (compact) 48f else if (landscape) 72f else 88f
                Prefs.CLOCK_MEDIUM -> if (compact) 34f else 44f
                else -> 0f
            }
            if (clockSp > 0f) {
                Text(
                    rememberClock(),
                    fontSize = clockSp.sp,
                    lineHeight = (clockSp * 1.05f).sp,
                    fontWeight = FontWeight.Thin,
                    color = pal.fg,
                    modifier = Modifier.shared("clock", scale = true),
                )
            }
            if (prefs.showSource && np.appName.isNotEmpty()) {
                Text(
                    np.appName,
                    fontSize = 15.sp,
                    color = pal.fg2,
                    modifier = Modifier
                        .shared("source")
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenSource),
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
private fun ColumnScope.Controls(np: NowPlaying, prefs: Prefs, compact: Boolean = false, lyr: LyricsUi? = null) {
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
        // Bouton paroles, ou vide pour l'équilibre visuel avec le ♥
        if (lyr != null) LyricsButton(lyr) else Spacer(Modifier.size(56.dp))
    }
    if (prefs.showVolume) VolumeRow()
}

@Composable
private fun LyricsButton(lyr: LyricsUi) {
    val pal = LocalPalette.current
    Box(Modifier.size(56.dp).clip(CircleShape).clickable(onClick = lyr.toggle), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Lyrics, "Paroles", Modifier.size(28.dp), tint = if (lyr.open) pal.fg else pal.faint)
    }
}

@Composable
private fun rememberLyrics(repo: LyricsRepository, np: NowPlaying, active: Boolean, allowMetered: Boolean): Lyrics {
    val latest by rememberUpdatedState(np)
    val state by produceState<Lyrics>(Lyrics.Loading, repo, np.title, np.artist, active, allowMetered) {
        value = Lyrics.Loading
        if (!active || np.title.isEmpty()) return@produceState
        delay(300) // morceaux passés à la chaîne : une seule requête, pour celui où l'on s'arrête
        while (true) {
            value = repo.load(latest.title, latest.artist, latest.durationMs, allowMetered)
            if (value != Lyrics.Unavailable && value != Lyrics.Offline) break
            delay(30_000)
        }
    }
    return state
}

@Composable
private fun LyricsPanel(np: NowPlaying, lyrics: Lyrics, prefs: Prefs, modifier: Modifier) {
    val pal = LocalPalette.current
    when (lyrics) {
        is Lyrics.Synced -> {
            val texts = remember(lyrics) { lyrics.lines.map { it.text } }
            LyricLines(texts, rememberSyncedIndex(np, lyrics.lines), highlight = true, prefs.textScale, modifier)
        }
        // Sans horodatage : défilement estimé au prorata de la durée, sans ligne mise en avant
        is Lyrics.Plain -> LyricLines(
            lyrics.lines, rememberEstimatedIndex(np, lyrics.lines.size), highlight = false, prefs.textScale, modifier,
        )
        else -> Box(modifier, contentAlignment = Alignment.Center) {
            val msg = when {
                np.title.isEmpty() -> ""
                lyrics == Lyrics.Loading -> "…"
                lyrics == Lyrics.Instrumental -> "♪  Instrumental"
                lyrics == Lyrics.Offline -> "Pas de réseau\n(nouvel essai toutes les 30 s)"
                lyrics == Lyrics.Unavailable -> "Les sources de paroles ne répondent pas\n(nouvel essai toutes les 30 s)"
                lyrics == Lyrics.MobileData -> "Paroles en pause sur données mobiles\n(réglable dans les paramètres)"
                else -> "Pas de paroles trouvées"
            }
            Text(msg, fontSize = (18f * prefs.textScale).sp, color = pal.faint, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LyricLines(lines: List<String>, current: Int, highlight: Boolean, scale: Float, modifier: Modifier) {
    val pal = LocalPalette.current
    val list = rememberLazyListState()
    BoxWithConstraints(modifier) {
        // La ligne en cours se cale au tiers de la hauteur : on voit un peu de passé, surtout ce qui vient
        val top = maxHeight * 0.3f
        LaunchedEffect(current) { list.animateScrollToItem(current.coerceAtLeast(0)) }
        LazyColumn(
            state = list,
            userScrollEnabled = false, // le geste vertical sert à ouvrir/fermer les paroles
            contentPadding = PaddingValues(top = top, bottom = maxHeight - top),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(FadeEdges, blendMode = BlendMode.DstIn)
                },
        ) {
            itemsIndexed(lines) { i, line ->
                val color by animateColorAsState(
                    when {
                        !highlight -> pal.fg2
                        i == current -> pal.fg
                        i > current -> lerp(pal.faint, pal.fg2, 0.35f)
                        else -> pal.faint
                    },
                    tween(300),
                    label = "ligne",
                )
                Text(
                    line.ifBlank { "♪" },
                    fontSize = (22f * scale).sp,
                    lineHeight = (29f * scale).sp,
                    fontWeight = FontWeight.Light,
                    color = color,
                    modifier = Modifier.padding(vertical = 7.dp),
                )
            }
        }
    }
}

private val FadeEdges = Brush.verticalGradient(
    0f to Color.Transparent,
    0.12f to Color.Black,
    0.85f to Color.Black,
    1f to Color.Transparent,
)

/** Ligne en cours ; se réveille pile à l'horodatage de la suivante plutôt que de sonder en boucle. */
@Composable
private fun rememberSyncedIndex(np: NowPlaying, lines: List<LyricLine>): Int {
    var idx by remember { mutableIntStateOf(-1) }
    LaunchedEffect(np, lines) {
        while (true) {
            val pos = np.currentPosition()
            idx = lines.indexOfLast { it.timeMs <= pos }
            if (!np.isPlaying) break
            val next = lines.getOrNull(idx + 1) ?: break
            delay(((next.timeMs - pos) / np.speed).toLong() + 20)
        }
    }
    return idx
}

@Composable
private fun rememberEstimatedIndex(np: NowPlaying, count: Int): Int {
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(np, count) {
        if (np.durationMs <= 0 || count == 0) return@LaunchedEffect
        val per = (np.durationMs / count).coerceAtLeast(1)
        while (true) {
            val pos = np.currentPosition()
            idx = (pos / per).toInt().coerceAtMost(count - 1)
            if (!np.isPlaying) break
            delay(per - pos % per + 20)
        }
    }
    return idx
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
    val level = LocalPalette.current.artLevel
    val filter = remember(gray, level) {
        if (!gray && level == 1f) null
        else ColorFilter.colorMatrix(
            ColorMatrix().apply {
                if (gray) setToSaturation(0f)
                if (level < 1f) timesAssign(ColorMatrix().apply { setToScale(level, level, level, 1f) })
            },
        )
    }
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF0C0C0C)),
        contentAlignment = Alignment.Center,
    ) {
        if (img != null) {
            Image(
                img, null, Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = filter,
            )
        } else {
            Icon(Icons.Rounded.MusicNote, null, Modifier.size(64.dp), tint = LocalPalette.current.faint)
        }
    }
}

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

/** Décalage aléatoire de ±amplitude dp toutes les intervalMs pour ne pas marquer l'AMOLED. */
@Composable
private fun rememberBurnInShift(enabled: Boolean, intervalMs: Long, amplitude: Int): DpOffset {
    var shift by remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
    LaunchedEffect(enabled, intervalMs, amplitude) {
        shift = DpOffset(0.dp, 0.dp)
        while (enabled) {
            delay(intervalMs)
            shift = DpOffset(
                Random.nextInt(-amplitude, amplitude + 1).dp,
                Random.nextInt(-amplitude, amplitude + 1).dp,
            )
        }
    }
    return shift
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
    else "%d:%02d".format(s / 60, s % 60)
}
