package fr.babas.noir

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.rounded.DirectionsCar
import fr.babas.noir.i18n.LocalLang
import fr.babas.noir.i18n.LocalStrings
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.toSize
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import kotlinx.coroutines.launch
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

private enum class Mode { NORMAL, LYRICS, CAR }

/** Paroles telles que les voient les dispositions ; null quand la fonction est coupée dans les réglages. */
private class LyricsUi(val open: Boolean, val state: LyricsCtl, val toggle: () -> Unit)

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
    night: Boolean,
    sleep: SleepTimer,
    sessions: List<SessionInfo>,
    onPickSession: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSource: () -> Unit,
) {
    var sheet by remember { mutableStateOf(SheetKind.NONE) }
    var likeFlash by remember { mutableIntStateOf(0) }

    // Veille : X minutes sans musique ni toucher
    var lastTouch by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var standby by remember { mutableStateOf(false) }
    val standbyMs = Prefs.STANDBY_DELAYS.getOrElse(prefs.standbyDelay) { Prefs.STANDBY_DELAYS[1] }.first * 1000L
    LaunchedEffect(np.isPlaying, lastTouch, prefs.standbyMode, standbyMs) {
        standby = false
        if (np.isPlaying || prefs.standbyMode == Prefs.STANDBY_OFF) return@LaunchedEffect
        delay(standbyMs)
        standby = true
    }

    Box(
        Modifier
            .fillMaxSize()
            // Observe chaque toucher sans le consommer : les boutons en dessous le reçoivent quand même
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    lastTouch = SystemClock.elapsedRealtime()
                }
            },
    ) {
        Player(
            np, prefs, lyricsRepo, night, sleep,
            onLike = {
                np.likeAction?.let { a ->
                    np.controller?.transportControls?.sendCustomAction(a, a.extras)
                    likeFlash++
                }
            },
            onSheet = { sheet = it },
            onOpenSettings = onOpenSettings,
            onOpenSource = onOpenSource,
        )
        LikeFlash(likeFlash)
        when (sheet) {
            SheetKind.SLEEP -> SleepSheet(sleep, np) { sheet = SheetKind.NONE }
            SheetKind.SOURCE -> SourceSheet(
                sessions, np.controller?.packageName, np.appName,
                onPick = onPickSession,
                onOpen = onOpenSource,
                onDismiss = { sheet = SheetKind.NONE },
            )
            SheetKind.NONE -> {}
        }
        AnimatedVisibility(standby, enter = fadeIn(tween(1500)), exit = fadeOut(tween(300))) {
            StandbyScreen(
                prefs.standbyMode == Prefs.STANDBY_CLOCK,
                clockPattern(prefs),
                rememberClockStyle(prefs),
                shiftDp = if (prefs.antiBurn) Prefs.BURN_AMPLITUDES.getOrElse(prefs.burnAmplitude) { Prefs.BURN_AMPLITUDES[1] }.first else 0,
            ) { lastTouch = SystemClock.elapsedRealtime() }
        }
    }
}

enum class SheetKind { NONE, SLEEP, SOURCE }

/** Grand cœur qui apparaît puis s'efface après un double-tap. */
@Composable
private fun BoxScope.LikeFlash(trigger: Int) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        alpha.snapTo(0.9f)
        alpha.animateTo(0f, tween(900))
    }
    Icon(
        Icons.Rounded.Favorite, null,
        Modifier.align(Alignment.Center).size(120.dp).graphicsLayer { this.alpha = alpha.value },
        tint = Color.White,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Player(
    np: NowPlaying,
    prefs: Prefs,
    lyricsRepo: LyricsRepository,
    night: Boolean,
    sleep: SleepTimer,
    onLike: () -> Unit,
    onSheet: (SheetKind) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSource: () -> Unit,
) {
    val current by rememberUpdatedState(np)
    val like by rememberUpdatedState(onLike)
    val openSource by rememberUpdatedState(onOpenSource)
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

    // Trou de la caméra : en portrait, le haut n'est plus réservé ; l'en-tête se place à côté du trou
    val view = LocalView.current
    val density = LocalDensity.current
    var frame by remember { mutableStateOf(Rect.Zero) }
    var cutouts by remember { mutableStateOf(emptyList<android.graphics.Rect>()) }
    val pad = if (landscape) PaddingValues(16.dp) else PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 36.dp)
    val reported = cutouts.firstOrNull { it.top <= 0 && it.bottom > 0 }
    // Position manuelle : même taille que ce qu'annonce Android (ou un trou typique), recentrée là où il est
    val camera = if (prefs.cameraPos == Prefs.CAM_AUTO) reported else with(density) {
        val w = reported?.width() ?: 36.dp.roundToPx()
        val h = reported?.bottom ?: 40.dp.roundToPx()
        val screen = view.width
        val cx = when (prefs.cameraPos) {
            Prefs.CAM_LEFT -> (screen * 0.12f).toInt()
            Prefs.CAM_RIGHT -> (screen * 0.88f).toInt()
            else -> screen / 2
        }
        android.graphics.Rect(cx - w / 2, 0, cx + w / 2, h)
    }
    val hole = if (landscape) null else camera?.let { c ->
        with(density) {
            val left = frame.left + 24.dp.toPx()
            val top = frame.top + 14.dp.toPx()
            val shiftMargin = if (prefs.antiBurn) Prefs.BURN_AMPLITUDES.getOrElse(prefs.burnAmplitude) { Prefs.BURN_AMPLITUDES[1] }.first.dp.toPx() else 0f
            Hole(
                Rect(c.left - left, c.top - top, c.right - left, c.bottom - top),
                width = frame.width - 48.dp.toPx(),
                margin = 6.dp.toPx() + shiftMargin,
            )
        }
    }
    // Voiture : pas de paroles, elles attirent le regard
    val showLyrics = prefs.lyrics && prefs.lyricsOpen && !prefs.carMode
    val mode = when {
        prefs.carMode -> Mode.CAR
        showLyrics -> Mode.LYRICS
        else -> Mode.NORMAL
    }
    val lyrics = rememberLyrics(lyricsRepo, np, showLyrics, prefs.lyricsOnMobile)

    CompositionLocalProvider(LocalPalette provides if (night) NightPalette else DayPalette) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(prefs.gestures) {
                    if (!prefs.gestures) return@pointerInput
                    detectTapGestures(onDoubleTap = { like() }, onLongPress = { openSource() })
                }
                // Gauche/droite = suivant/précédent, haut/bas = paroles (les sliders consomment leurs propres gestes)
                .pointerInput(Unit) {
                    val threshold = 96.dp.toPx()
                    detectDragGestures(
                        onDragEnd = {
                            if (abs(dragX) > abs(dragY)) {
                                val tc = current.controller?.transportControls
                                if (dragX < -threshold) tc?.skipToNext() else if (dragX > threshold) tc?.skipToPrevious()
                            } else if (prefs.lyrics && !prefs.carMode) {
                                if (dragY < -threshold) prefs.lyricsOpen = true else if (dragY > threshold) prefs.lyricsOpen = false
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
                .windowInsetsPadding(
                    if (landscape) WindowInsets.safeDrawing
                    else WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                )
                // Cadre avant décalage anti-marquage (la marge du trou en tient compte) ; le trou se lit ici,
                // une fois les encarts du système connus (ils changent avec la rotation)
                .onGloballyPositioned { c ->
                    frame = Rect(c.positionInWindow(), c.size.toSize())
                    val rects = view.rootWindowInsets?.displayCutout?.boundingRects.orEmpty()
                    if (rects != cutouts) cutouts = rects
                }
                .offset {
                    val follow = if (abs(dragX) > abs(dragY)) (dragX * 0.3f).roundToInt() else 0
                    IntOffset(shift.x.roundToPx() + follow, shift.y.roundToPx())
                }
                .padding(pad),
        ) {
            CompositionLocalProvider(LocalHole provides hole) {
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                AnimatedContent(
                    mode,
                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                    label = "disposition",
                ) { m ->
                    val open = m == Mode.LYRICS
                    CompositionLocalProvider(LocalShared provides SharedScopes(this@SharedTransitionLayout, this)) {
                        val lyr = if (prefs.lyrics) LyricsUi(open, lyrics) { prefs.lyricsOpen = !prefs.lyricsOpen } else null
                        val header: @Composable (Boolean) -> Unit = { withGear ->
                            Header(
                                np, prefs, landscape, sleep,
                                compact = open,
                                car = m == Mode.CAR,
                                atTop = withGear,
                                onOpenSettings = if (withGear) onOpenSettings else null,
                                onClock = { onSheet(SheetKind.SLEEP) },
                                onSource = { onSheet(SheetKind.SOURCE) },
                            )
                        }
                        when {
                            m == Mode.CAR -> CarLayout(np, prefs, landscape, header)
                            landscape -> LandscapeLayout(np, prefs, lyr, header)
                            else -> PortraitLayout(np, prefs, lyr, header, onOpenSettings)
                        }
                    }
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
            val hole = LocalHole.current
            val mirror = mirrorClock(prefs, hole)
            val gearTop = hole?.let { h ->
                with(LocalDensity.current) {
                    val g = 96.dp.toPx()
                    (if (mirror) h.pushDown(0f, g) else h.pushDown(h.width - g, h.width)).toDp()
                }
            } ?: 0.dp
            TopButtons(
                prefs, mirror, onOpenSettings,
                Modifier.align(if (mirror) Alignment.TopStart else Alignment.TopEnd).padding(top = gearTop),
            )
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

/** Trou de la caméra en coordonnées du contenu (px), avec la largeur du contenu et une marge de sécurité. */
class Hole(val rect: Rect, val width: Float, val margin: Float) {
    /** Décalage vers le bas d'un élément posé en haut entre x0 et x1 : 0 s'il passe à côté du trou. */
    fun pushDown(x0: Float, x1: Float): Float =
        if (rect.bottom > 0f && x1 > rect.left - margin && x0 < rect.right + margin) rect.bottom + margin else 0f
}

val LocalHole = staticCompositionLocalOf<Hole?> { null }

/** Heure à droite (roue à gauche) : réglage, ou en auto quand il y a plus de place à droite du trou. */
private fun mirrorClock(prefs: Prefs, hole: Hole?): Boolean = when (prefs.clockSide) {
    Prefs.SIDE_LEFT -> false
    Prefs.SIDE_RIGHT -> true
    else -> hole != null && hole.rect.bottom > 0f && hole.width - hole.rect.right > hole.rect.left
}

@Composable
private fun Header(
    np: NowPlaying,
    prefs: Prefs,
    landscape: Boolean,
    sleep: SleepTimer,
    compact: Boolean,
    car: Boolean,
    atTop: Boolean,
    onOpenSettings: (() -> Unit)?,
    onClock: () -> Unit,
    onSource: () -> Unit,
) {
    val pal = LocalPalette.current
    val density = LocalDensity.current
    val anyHole = LocalHole.current
    val mirror = mirrorClock(prefs, anyHole)
    // Le trou ne gêne que l'en-tête du haut de l'écran
    val hole = if (atTop) anyHole?.takeIf { it.rect.bottom > 0f } else null
    val baseSp = when (prefs.clockSize) {
        // Voiture : la plus grande heure possible, ramenée ensuite à la largeur disponible
        Prefs.CLOCK_BIG -> if (car) (if (landscape) 80f else 200f) else if (compact) 48f else if (landscape) 72f else 88f
        Prefs.CLOCK_MEDIUM -> if (car) (if (landscape) 56f else 120f) else if (compact) 34f else 44f
        else -> 0f
    }
    val style = rememberClockStyle(prefs)
    val time = rememberFormatted(clockPattern(prefs), LocalLang.current.locale)
    val thumb = compact && prefs.showArt
    val thumbSize = if (landscape) 64.dp else 76.dp
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Boutons voiture + roue : toujours en haut, dans la bande du trou s'ils passent à côté
        val buttons = onOpenSettings != null
        var clockSp = baseSp
        var mainTop = 0f
        var gearTop = 0f
        var mainEndPad = 0f
        with(density) {
            val width = constraints.maxWidth.toFloat()
            val groupW = if (buttons) 96.dp.toPx() else 0f
            val groupH = 48.dp.toPx()
            if (hole != null && buttons) {
                gearTop = if (mirror) hole.pushDown(0f, groupW) else hole.pushDown(width - groupW, width)
            }
            val side = if (thumb) (thumbSize + 16.dp).toPx() else 0f
            // Chiffres remplacés par des 0 : la taille ne saute pas d'une minute à l'autre
            val w = if (baseSp > 0f) measurer.measure(
                time.replace(Regex("\\d"), "0"),
                TextStyle(fontSize = baseSp.sp, fontFamily = style.family, fontWeight = style.weight),
            ).size.width.toFloat() else 0f

            // A : l'heure dans la bande du trou, à côté de lui (rétrécie s'il le faut)
            val fitBeside = if (hole == null || w == 0f) 0f else {
                val free = (if (mirror) width - hole.rect.right else hole.rect.left) - hole.margin - side
                (free / w).coerceAtMost(1f)
            }
            // En voiture l'heure doit rester grande : plutôt sous le trou que rétrécie à côté
            val minBeside = if (car) 0.97f else 0.6f
            if (hole != null && baseSp > 0f && fitBeside >= minBeside) {
                clockSp = baseSp * fitBeside
                mainEndPad = groupW
            } else {
                // B : sous le trou (ou tout en haut s'il n'y en a pas), sur toute la largeur libre
                mainTop = hole?.let { it.rect.bottom + it.margin } ?: 0f
                if (buttons && mainTop < gearTop + groupH) {
                    // En voiture portrait, l'heure passe sous les boutons pour prendre toute la largeur ;
                    // sinon (et en paysage, où c'est la hauteur qui manque) elle reste à côté d'eux
                    if (car && !landscape) mainTop = gearTop + groupH else mainEndPad = groupW + 8.dp.toPx()
                }
                if (w > 0f) clockSp = baseSp * ((width - side - mainEndPad) / w).coerceAtMost(1f)
            }
        }

        Row(
            Modifier
                .align(if (mirror) Alignment.TopEnd else Alignment.TopStart)
                .padding(top = with(density) { mainTop.toDp() })
                .padding(
                    start = if (mirror) with(density) { mainEndPad.toDp() } else 0.dp,
                    end = if (mirror) 0.dp else with(density) { mainEndPad.toDp() },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbnail: @Composable () -> Unit = {
                Artwork(np.art, prefs.grayArt, Modifier.shared("art").size(thumbSize))
            }
            if (thumb && !mirror) {
                // Écran paroles : la pochette devient une vignette à côté de l'heure
                thumbnail()
                Spacer(Modifier.width(16.dp))
            }
            Column(horizontalAlignment = if (mirror) Alignment.End else Alignment.Start) {
                if (clockSp > 0f) {
                    Text(
                        time,
                        fontSize = clockSp.sp,
                        lineHeight = (clockSp * 1.05f).sp,
                        fontFamily = style.family,
                        fontWeight = style.weight,
                        color = pal.fg,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .shared("clock", scale = true)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClock),
                    )
                }
                InfoLine(prefs, sleep, onClock)
                if (prefs.showSource && np.appName.isNotEmpty()) {
                    Text(
                        np.appName,
                        fontSize = 15.sp,
                        color = pal.fg2,
                        modifier = Modifier
                            .shared("source")
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onSource),
                    )
                }
            }
            if (thumb && mirror) {
                Spacer(Modifier.width(16.dp))
                thumbnail()
            }
        }
        if (onOpenSettings != null) {
            TopButtons(
                prefs, mirror, onOpenSettings,
                Modifier
                    .align(if (mirror) Alignment.TopStart else Alignment.TopEnd)
                    .padding(top = with(density) { gearTop.toDp() }),
            )
        }
    }
}

/** Voiture + roue dentée, la roue côté bord de l'écran. */
@Composable
private fun TopButtons(prefs: Prefs, mirror: Boolean, onOpenSettings: () -> Unit, modifier: Modifier) {
    Row(modifier) {
        if (mirror) {
            SettingsButton(Modifier, onOpenSettings)
            CarButton(prefs)
        } else {
            CarButton(prefs)
            SettingsButton(Modifier, onOpenSettings)
        }
    }
}

/** « sam. 25 oct. · 82 % ⚡ · ☾ 23 min » : date et batterie (réglage), minuteur s'il tourne. */
@Composable
private fun InfoLine(prefs: Prefs, sleep: SleepTimer, onClick: () -> Unit) {
    val sleepLabel = rememberSleepLabel(sleep)
    if (!prefs.showDate && sleepLabel == null) return
    val pal = LocalPalette.current
    Row(
        Modifier.shared("info").clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (prefs.showDate) {
            val (level, charging) = rememberBattery()
            val date = rememberFormatted(datePattern(prefs), LocalLang.current.locale)
            Text(if (level >= 0) "$date · $level %" else date, fontSize = 15.sp, color = pal.fg2)
            if (charging) Icon(Icons.Rounded.Bolt, LocalStrings.current.charging, Modifier.size(16.dp), tint = pal.fg2)
        }
        if (sleepLabel != null) {
            if (prefs.showDate) Text(" · ", fontSize = 15.sp, color = pal.fg2)
            Icon(Icons.Rounded.Bedtime, LocalStrings.current.sleepTimer, Modifier.size(15.dp), tint = pal.fg2)
            Text(" $sleepLabel", fontSize = 15.sp, color = pal.fg2)
        }
    }
}

/** Niveau de batterie (en %, -1 si inconnu) et branchement, suivis en direct. */
@Composable
private fun rememberBattery(): Pair<Int, Boolean> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(-1 to false) }
    DisposableEffect(Unit) {
        fun read(i: Intent) {
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            state = (if (level >= 0 && scale > 0) level * 100 / scale else -1) to
                (i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = read(i)
        }
        // Diffusion « collante » : l'enregistrement renvoie aussitôt le dernier état connu
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )?.let(::read)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

@Composable
private fun SettingsButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.shared("gear").size(48.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Settings, LocalStrings.current.settings, tint = LocalPalette.current.faint, modifier = Modifier.size(26.dp))
    }
}

/** Titre, progression, boutons, volume : le bloc « contrôles ». */
@Composable
private fun ColumnScope.Controls(np: NowPlaying, prefs: Prefs, compact: Boolean = false, lyr: LyricsUi? = null) {
    // Un seul bloc qui glisse entre l'écran normal et l'écran paroles, comme l'heure et la pochette
    Column(Modifier.shared("controls")) { ControlsContent(np, prefs, compact, lyr) }
}

@Composable
private fun ColumnScope.ControlsContent(np: NowPlaying, prefs: Prefs, compact: Boolean, lyr: LyricsUi?) {
    val str = LocalStrings.current
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
            np.title.ifEmpty { str.nothingPlaying },
            fontSize = titleSp.sp,
            lineHeight = (titleSp * 1.2f).sp,
            fontWeight = FontWeight.Light,
            color = pal.fg,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            np.artist.ifEmpty { if (np.controller == null) str.startMusicHint else "" },
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
        CtrlButton(Icons.Rounded.SkipPrevious, str.previous, 48.dp, enabled) { tc?.skipToPrevious() }
        CtrlButton(
            if (np.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (np.isPlaying) str.pause else str.play,
            if (compact) 64.dp else 76.dp,
            enabled,
        ) { togglePlay() }
        CtrlButton(Icons.Rounded.SkipNext, str.next, 48.dp, enabled) { tc?.skipToNext() }
        // Bouton paroles, ou vide pour l'équilibre visuel avec le ♥
        if (lyr != null) LyricsButton(lyr) else Spacer(Modifier.size(56.dp))
    }
    if (prefs.showVolume) VolumeRow()
}

@Composable
private fun CarButton(prefs: Prefs) {
    val pal = LocalPalette.current
    Box(
        Modifier.shared("car").size(48.dp).clip(CircleShape).clickable { prefs.carMode = !prefs.carMode },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.DirectionsCar, LocalStrings.current.carMode, Modifier.size(26.dp),
            tint = if (prefs.carMode) pal.fg else pal.faint,
        )
    }
}

/** Mode voiture : peu d'éléments, très gros, lisibles d'un coup d'œil ; pas de paroles. */
@Composable
private fun CarLayout(np: NowPlaying, prefs: Prefs, landscape: Boolean, header: @Composable (Boolean) -> Unit) {
    if (landscape) {
        // Pochette à gauche (ou à droite si inversée) ; à côté, une colonne : heure, titre, et une seule
        // rangée ◀ · ▶ · ▶▶ (· ♥) pour tenir dans la hauteur
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            val art: @Composable () -> Unit = {
                Artwork(np.art, prefs.grayArt, Modifier.shared("art").fillMaxHeight().aspectRatio(1f))
            }
            val swap = prefs.swapArt
            if (prefs.showArt && !swap) {
                art()
                Spacer(Modifier.width(28.dp))
            }
            // Les boutons ont leur taille fixe et passent en premier : c'est l'espace vide au-dessus
            // du titre qui s'adapte (avant, posés en dernier, ils étaient écrasés par manque de hauteur)
            Column(Modifier.weight(1f).fillMaxHeight()) {
                header(true)
                Spacer(Modifier.weight(1f))
                CarTitle(np, prefs, titleSp = 26f, artistSp = 18f, maxLines = 1)
                if (prefs.showProgress) Progress(np)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CarButtons(np, Modifier.weight(1f), side = 72.dp, main = 104.dp)
                    if (np.likeAction != null) LikeButton(np)
                }
            }
            if (prefs.showArt && swap) {
                Spacer(Modifier.width(28.dp))
                art()
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            header(true)
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                if (prefs.showArt) {
                    Artwork(np.art, prefs.grayArt, Modifier.shared("art").aspectRatio(1f, matchHeightConstraintsFirst = true))
                }
            }
            // Les gros boutons tout en bas, sous le pouce ; le ♥ (s'il existe) à côté du titre
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { CarTitle(np, prefs, titleSp = 40f, artistSp = 26f, maxLines = 2) }
                if (np.likeAction != null) LikeButton(np)
            }
            if (prefs.showProgress) {
                Spacer(Modifier.height(8.dp))
                Progress(np)
            }
            Spacer(Modifier.height(16.dp))
            CarButtons(np, Modifier.fillMaxWidth(), side = 80.dp, main = 120.dp)
        }
    }
}

@Composable
private fun CarTitle(np: NowPlaying, prefs: Prefs, titleSp: Float, artistSp: Float, maxLines: Int) {
    val str = LocalStrings.current
    val pal = LocalPalette.current
    val tc = np.controller?.transportControls
    val t = titleSp * prefs.textScale
    Column(
        Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) {
            if (np.isPlaying) tc?.pause() else tc?.play()
        },
    ) {
        Text(
            np.title.ifEmpty { str.nothingPlaying },
            fontSize = t.sp, lineHeight = (t * 1.15f).sp, fontWeight = FontWeight.Normal, color = pal.fg,
            maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        )
        Text(
            np.artist, fontSize = (artistSp * prefs.textScale).sp, color = pal.fg2,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CarButtons(np: NowPlaying, modifier: Modifier, side: Dp, main: Dp) {
    val str = LocalStrings.current
    val tc = np.controller?.transportControls
    val enabled = np.controller != null
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        CtrlButton(Icons.Rounded.SkipPrevious, str.previous, side, enabled) { tc?.skipToPrevious() }
        CtrlButton(
            if (np.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (np.isPlaying) str.pause else str.play,
            main,
            enabled,
        ) { if (np.isPlaying) tc?.pause() else tc?.play() }
        CtrlButton(Icons.Rounded.SkipNext, str.next, side, enabled) { tc?.skipToNext() }
    }
}

@Composable
private fun LyricsButton(lyr: LyricsUi) {
    val pal = LocalPalette.current
    Box(Modifier.size(56.dp).clip(CircleShape).clickable(onClick = lyr.toggle), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Lyrics, LocalStrings.current.lyrics, Modifier.size(28.dp), tint = if (lyr.open) pal.fg else pal.faint)
    }
}

/** Paroles du morceau en cours, et de quoi essayer l'autre source quand elles sont fausses ou décalées. */
private class LyricsCtl(val value: Lyrics, val busy: Boolean, val nothingOther: Boolean, val switch: () -> Unit)

@Composable
private fun rememberLyrics(repo: LyricsRepository, np: NowPlaying, active: Boolean, allowMetered: Boolean): LyricsCtl {
    val latest by rememberUpdatedState(np)
    var value by remember { mutableStateOf<Lyrics>(Lyrics.Loading) }
    var busy by remember { mutableStateOf(false) }
    var nothingOther by remember { mutableStateOf(false) }
    LaunchedEffect(repo, np.title, np.artist, active, allowMetered) {
        value = Lyrics.Loading
        nothingOther = false
        if (!active || np.title.isEmpty()) return@LaunchedEffect
        delay(300) // morceaux passés à la chaîne : une seule requête, pour celui où l'on s'arrête
        while (true) {
            value = repo.load(latest.title, latest.artist, latest.durationMs, allowMetered)
            if (value != Lyrics.Unavailable && value != Lyrics.Offline) break
            delay(30_000)
        }
    }
    val scope = rememberCoroutineScope()
    val switch = {
        val source = (value as? Lyrics.Synced)?.source ?: (value as? Lyrics.Plain)?.source
        if (source != null && !busy) scope.launch {
            busy = true
            val track = latest.title
            val other = repo.loadOther(latest.title, latest.artist, latest.durationMs, source, allowMetered)
            busy = false
            if (latest.title != track) return@launch // morceau changé entre-temps
            if (other != null) {
                value = other
            } else {
                nothingOther = true
                delay(3000)
                nothingOther = false
            }
        }
        Unit
    }
    return LyricsCtl(value, busy, nothingOther, switch)
}

@Composable
private fun LyricsPanel(np: NowPlaying, ctl: LyricsCtl, prefs: Prefs, modifier: Modifier) {
    val str = LocalStrings.current
    val pal = LocalPalette.current
    when (val lyrics = ctl.value) {
        is Lyrics.Synced, is Lyrics.Plain -> Column(modifier) {
            val area = Modifier.weight(1f).fillMaxWidth()
            if (lyrics is Lyrics.Synced) {
                val texts = remember(lyrics) { lyrics.lines.map { it.text } }
                LyricLines(texts, rememberSyncedIndex(np, lyrics.lines), synced = true, prefs.lyricsScale, area)
            } else if (lyrics is Lyrics.Plain) {
                // Sans horodatage : défilement estimé au prorata de la durée, et libre au doigt
                LyricLines(
                    lyrics.lines, rememberEstimatedIndex(np, lyrics.lines.size), synced = false, prefs.lyricsScale, area,
                )
            }
            // Source discrète sur sa propre ligne sous les paroles (posée dessus, elle masquait du texte) ;
            // la toucher essaie l'autre (paroles décalées ou fausses)
            val source = (lyrics as? Lyrics.Synced)?.source ?: (lyrics as Lyrics.Plain).source
            Text(
                when {
                    ctl.busy -> "…"
                    ctl.nothingOther -> str.lyricsNothingOther
                    else -> "$source  ⇄"
                },
                fontSize = 12.sp,
                color = pal.faint,
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = ctl.switch)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        else -> Box(modifier, contentAlignment = Alignment.Center) {
            val msg = when {
                np.title.isEmpty() -> ""
                lyrics == Lyrics.Loading -> "…"
                lyrics == Lyrics.Instrumental -> str.lyricsInstrumental
                lyrics == Lyrics.Offline -> str.lyricsOffline
                lyrics == Lyrics.Unavailable -> str.lyricsUnavailable
                lyrics == Lyrics.MobileData -> str.lyricsMobileData
                else -> str.lyricsNotFound
            }
            Text(msg, fontSize = (18f * prefs.lyricsScale).sp, color = pal.faint, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LyricLines(lines: List<String>, current: Int, synced: Boolean, scale: Float, modifier: Modifier) {
    val pal = LocalPalette.current
    val highlight = synced
    val list = rememberLazyListState()
    // Non synchronisées : on peut faire défiler au doigt ; le suivi automatique attend alors 8 s
    val dragged by list.interactionSource.collectIsDraggedAsState()
    var touchedAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(dragged) { if (dragged) touchedAt = SystemClock.elapsedRealtime() }
    BoxWithConstraints(modifier) {
        // La ligne en cours se cale au tiers de la hauteur : on voit un peu de passé, surtout ce qui vient
        val top = maxHeight * 0.3f
        LaunchedEffect(current) {
            if (SystemClock.elapsedRealtime() - touchedAt > 8000) list.animateScrollToItem(current.coerceAtLeast(0))
        }
        LazyColumn(
            state = list,
            // Synchronisées : verrouillées, le geste vertical sert à ouvrir/fermer les paroles
            userScrollEnabled = !synced,
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
            .shared("like")
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
private fun VolumeRow(modifier: Modifier = Modifier) {
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

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { set(vol - 1) }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.VolumeDown, LocalStrings.current.volumeDown, tint = LocalPalette.current.fg2)
        }
        Slider(
            value = vol.toFloat(),
            onValueChange = { set(it.roundToInt()) },
            valueRange = 0f..max.toFloat(),
            colors = noirSliderColors(),
            modifier = Modifier.weight(1f).height(32.dp),
        )
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { set(vol + 1) }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, LocalStrings.current.volumeUp, tint = LocalPalette.current.fg2)
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

/** Heure ou date formatée, mise à jour pile à chaque changement de minute. */
@Composable
fun rememberFormatted(pattern: String, locale: Locale): String {
    var text by remember { mutableStateOf("") }
    LaunchedEffect(pattern, locale) {
        val fmt = SimpleDateFormat(pattern, locale)
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
