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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.babas.noir.i18n.LocalStrings
import kotlinx.coroutines.launch

private const val PAGES = 6

/** Premier lancement (ou « Revoir le tuto ») : langue d'abord, puis accès, gestes, paroles, voiture, écran. */
@Composable
fun TutorialScreen(prefs: Prefs, hasAccess: Boolean, onGrant: () -> Unit, onDone: () -> Unit) {
    val s = LocalStrings.current
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
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
                    when (page) {
                        0 -> LanguagePage(prefs)
                        1 -> Welcome(hasAccess, onGrant)
                        2 -> Gestures()
                        3 -> LyricsPage(prefs)
                        4 -> CarPage()
                        else -> ScreenPage()
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDone) { Text(s.skip, color = Gray) }
            Spacer(Modifier.weight(1f))
            repeat(PAGES) { i ->
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) Color.White else Dim),
                )
            }
            Spacer(Modifier.weight(1f))
            val last = pager.currentPage == PAGES - 1
            OutlinedButton(onClick = {
                if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }) {
                Text(if (last) s.letsGo else s.nextPage, color = Color.White)
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, fontSize = 40.sp, fontWeight = FontWeight.Thin, color = Color.White)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, fontSize = 17.sp, lineHeight = 25.sp, color = Gray)
    Spacer(Modifier.height(12.dp))
}

/** Première page : la langue, avant tout le reste (la suite du tuto change aussitôt de langue). */
@Composable
private fun LanguagePage(prefs: Prefs) {
    Column {
        Title(LocalStrings.current.tutoLanguageTitle)
        LanguageItems(prefs)
    }
}

@Composable
private fun Welcome(hasAccess: Boolean, onGrant: () -> Unit) {
    val s = LocalStrings.current
    Column {
        Title("Noir")
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
