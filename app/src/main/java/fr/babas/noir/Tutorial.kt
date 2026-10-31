package fr.babas.noir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.babas.noir.i18n.Lang
import fr.babas.noir.i18n.LocalLang
import fr.babas.noir.i18n.LocalStrings
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow

const val GITHUB_URL = "https://github.com/BabasGames/noir-music"

private enum class Page { LANGUAGE, ACCESS, CUSTOMIZE, CLOCK, LOOK, GESTURES, LYRICS, CAR, SCREEN, OPEN_SOURCE }

/**
 * First launch (or "Replay the tutorial"): language and access first (the app needs them), then an offer to
 * set up the look right away (optional, the defaults stay as they are), then gestures, lyrics, car, screen.
 */
@Composable
fun TutorialScreen(prefs: Prefs, np: NowPlaying, hasAccess: Boolean, onGrant: () -> Unit, onDone: () -> Unit) {
    val s = LocalStrings.current
    var customize by rememberSaveable { mutableStateOf(false) }
    val pages = Page.entries.filter { customize || (it != Page.CLOCK && it != Page.LOOK) }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val anim = LocalAnimations.current
    val goNext = {
        scope.launch {
            val next = (pager.currentPage + 1).coerceAtMost(pages.size - 1)
            if (anim) pager.animateScrollToPage(next) else pager.scrollToPage(next)
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
    ) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                Column(Modifier.widthIn(max = 520.dp).align(Alignment.CenterHorizontally)) {
                    when (pages.getOrNull(page)) {
                        Page.LANGUAGE -> LanguagePage(prefs)
                        Page.ACCESS -> Welcome(hasAccess, onGrant)
                        Page.CUSTOMIZE -> CustomizePage(
                            onYes = { customize = true; goNext() },
                            onLater = { customize = false; goNext() },
                        )
                        Page.CLOCK -> ClockPage(prefs)
                        Page.LOOK -> LookPage(prefs, np)
                        Page.GESTURES -> Gestures()
                        Page.LYRICS -> LyricsPage(prefs)
                        Page.CAR -> CarPage()
                        Page.SCREEN -> ScreenPage()
                        Page.OPEN_SOURCE, null -> OpenSourcePage(laterHint = !customize)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDone) { Text(s.skip, color = Gray) }
            Spacer(Modifier.weight(1f))
            repeat(pages.size) { i ->
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) Color.White else Dim),
                )
            }
            Spacer(Modifier.weight(1f))
            val last = pager.currentPage == pages.size - 1
            OutlinedButton(onClick = { if (last) onDone() else goNext() }) {
                Text(if (last) s.letsGo else s.nextPage, color = Color.White)
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Thin, color = Color.White)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, fontSize = 17.sp, lineHeight = 25.sp, color = Gray)
    Spacer(Modifier.height(12.dp))
}

/** First page: the language, before anything else (the rest of the tutorial switches language right away). */
@Composable
private fun LanguagePage(prefs: Prefs) {
    Column {
        Title(LocalStrings.current.tutoLanguageTitle)
        // French is the original: every other language was translated automatically
        if (LocalLang.current != Lang.FR) Body(LocalStrings.current.machineTranslation)
        LanguageItems(prefs)
    }
}

@Composable
private fun Welcome(hasAccess: Boolean, onGrant: () -> Unit) {
    val s = LocalStrings.current
    Column {
        Title("Noir Music")
        Body(s.welcome1)
        Body(s.welcome2)
        Spacer(Modifier.height(8.dp))
        if (hasAccess) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(s.accessGranted, color = Color.White, fontSize = 17.sp)
            }
        } else {
            OutlinedButton(onClick = onGrant) { Text(s.allowAccess, color = Color.White) }
        }
    }
}

@Composable
private fun Gestures() {
    val s = LocalStrings.current
    Column {
        Title(s.gestures)
        listOf(
            "← →" to s.gestureSwipeH,
            "↑ ↓" to s.gestureSwipeV,
            s.gestureTapTitle to s.gestureTapTitleAction,
            s.gestureDoubleTap to s.gestureDoubleTapAction,
            s.gestureLongPress to s.gestureLongPressAction,
            s.gestureTapClock to s.gestureTapClockAction,
            s.gestureTapApp to s.gestureTapAppAction,
        ).forEach { (gesture, action) ->
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(gesture, color = Color.White, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Text(action, color = Gray, fontSize = 17.sp, modifier = Modifier.weight(1.2f))
            }
        }
    }
}

@Composable
private fun LyricsPage(prefs: Prefs) {
    val s = LocalStrings.current
    Column {
        Title(s.lyrics)
        Body(s.tutoLyrics)
        Body(s.lyricsDisclaimer)
        Toggle(s.downloadOnMobile, s.onMobileSub, prefs.lyricsOnMobile) { prefs.lyricsOnMobile = it }
        Toggle(s.lyricsFallback, s.lyricsFallbackSub, prefs.lyricsFallback) { prefs.lyricsFallback = it }
    }
}

@Composable
private fun CarPage() {
    val s = LocalStrings.current
    Column {
        Icon(Icons.Rounded.DirectionsCar, null, Modifier.size(48.dp), tint = Color.White)
        Spacer(Modifier.height(12.dp))
        Title(s.tutoCarTitle)
        Body(s.tutoCar1)
        Body(s.tutoCar2)
    }
}

/** Last page: the app is in development, bugs and ideas go to GitHub, and where to look for updates. */
@Composable
private fun OpenSourcePage(laterHint: Boolean) {
    val s = LocalStrings.current
    val uri = LocalUriHandler.current
    Column {
        Title(s.tutoOpenTitle)
        Body(s.tutoOpen1)
        Body(s.tutoOpen2)
        Body(s.tutoOpen3)
        if (laterHint) Body(s.tutoLater)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { uri.openUri("$GITHUB_URL/issues") }) { Text(s.openGithub, color = Color.White) }
    }
}

@Composable
private fun ScreenPage() {
    val s = LocalStrings.current
    Column {
        Title(s.tutoScreenTitle)
        Body(s.tutoScreen1)
        Body(s.tutoScreen2)
        Body(s.tutoScreen3)
    }
}

/** Set up the look now or later; "later" keeps the defaults, so nothing changes for those who skip. */
@Composable
private fun CustomizePage(onYes: () -> Unit, onLater: () -> Unit) {
    val s = LocalStrings.current
    Column {
        Title(s.tutoCustomizeTitle)
        Body(s.tutoCustomizeBody)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onYes,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            ) { Text(s.customize) }
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onLater) { Text(s.later, color = Gray) }
        }
    }
}

@Composable
private fun ClockPage(prefs: Prefs) {
    val s = LocalStrings.current
    Column {
        Title(s.clock)
        Choice(s.clock, listOf(s.clockHidden, s.clockMedium, s.clockBig), prefs.clockSize) { prefs.clockSize = it }
        Choice(s.clockFormat, listOf(s.formatAuto, s.format24, s.format12), prefs.clockFormat) { prefs.clockFormat = it }
        // Has its own preview of the time in the chosen font
        ClockFontPicker(prefs)
    }
}

@Composable
private fun LookPage(prefs: Prefs, np: NowPlaying) {
    val s = LocalStrings.current
    Column {
        Title(s.sectionDisplay)
        LookPreview(prefs, np, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(12.dp))
        Toggle(s.showArt, null, prefs.showArt) { prefs.showArt = it }
        Toggle(s.grayArt, null, prefs.grayArt, enabled = prefs.showArt) { prefs.grayArt = it }
        Choice(
            s.textSize,
            textScaleLabels(),
            TEXT_SCALES.indexOfFirst { it == prefs.textScale }.coerceAtLeast(0),
        ) { prefs.textScale = TEXT_SCALES[it] }
    }
}

/** Small sketch of the player that follows the choices: clock, artwork, title, buttons. */
@Composable
private fun LookPreview(prefs: Prefs, np: NowPlaying, modifier: Modifier) {
    val s = LocalStrings.current
    val shape = RoundedCornerShape(18.dp)
    val style = rememberClockStyle(prefs)
    val time = rememberFormatted(clockPattern(prefs), LocalLang.current.locale)
    val scale = prefs.textScale
    Column(
        modifier
            .width(190.dp)
            .height(340.dp)
            .clip(shape)
            .border(1.dp, Dim, shape)
            .background(Color.Black)
            .padding(14.dp),
        verticalArrangement = if (prefs.showArt) Arrangement.Top else Arrangement.Center,
    ) {
        if (prefs.clockSize != Prefs.CLOCK_HIDDEN) {
            Text(
                time,
                fontSize = if (prefs.clockSize == Prefs.CLOCK_BIG) 40.sp else 26.sp,
                lineHeight = if (prefs.clockSize == Prefs.CLOCK_BIG) 44.sp else 30.sp,
                fontFamily = style.family,
                fontWeight = style.weight,
                color = Color.White,
            )
        }
        if (prefs.showArt) {
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0C0C0C)),
                    contentAlignment = Alignment.Center,
                ) {
                    val img = remember(np.art) { np.art?.asImageBitmap() }
                    if (img != null) {
                        Image(
                            img, null, Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            colorFilter = if (prefs.grayArt) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                        )
                    } else {
                        Icon(Icons.Rounded.MusicNote, null, Modifier.size(32.dp), tint = Dim)
                    }
                }
            }
        } else {
            Spacer(Modifier.height(18.dp))
        }
        Text(
            np.title.ifEmpty { s.nothingPlaying },
            fontSize = (13f * scale).sp, fontWeight = FontWeight.Light, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            np.artist.ifEmpty { "Noir Music" },
            fontSize = (9f * scale).sp, color = Gray, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(Dim))
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.SkipPrevious, null, Modifier.size(18.dp), tint = Color.White)
            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(26.dp), tint = Color.White)
            Icon(Icons.Rounded.SkipNext, null, Modifier.size(18.dp), tint = Color.White)
        }
    }
}
