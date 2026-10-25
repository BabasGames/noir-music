package fr.babas.noir

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.babas.noir.i18n.LocalLang
import fr.babas.noir.i18n.LocalStrings
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Fenêtre centrée par-dessus le lecteur ; toucher à côté ou « retour » la ferme. */
@Composable
fun Sheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(Color(0xFF0C0C0C))
                .border(1.dp, Color(0xFF242424), shape)
                // Toucher dans la fenêtre ne doit pas la fermer
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            Text(title.uppercase(), fontSize = 13.sp, letterSpacing = 1.5.sp, color = Gray, modifier = Modifier.padding(20.dp, 8.dp))
            content()
        }
    }
}

@Composable
fun SheetItem(
    label: String,
    sub: String? = null,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(22.dp), tint = if (selected) Color.White else Gray)
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 18.sp, color = if (selected) Color.White else Color(0xFFCCCCCC), maxLines = 2)
            if (!sub.isNullOrEmpty()) {
                Text(sub, fontSize = 14.sp, color = Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (selected) Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Color.White))
    }
}

@Composable
fun SleepSheet(timer: SleepTimer, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    Sheet(s.sleepTimerTitle, onDismiss) {
        listOf(15, 30, 45, 60).forEach { min ->
            SheetItem(if (min == 60) s.oneHour else s.minutes(min)) {
                timer.startMinutes(min)
                onDismiss()
            }
        }
        SheetItem(s.endOfTrack, selected = timer.endOfTrack) {
            timer.startEndOfTrack()
            onDismiss()
        }
        if (timer.active) {
            SheetItem(s.stopTimer, icon = Icons.Rounded.Close) {
                timer.cancel()
                onDismiss()
            }
        }
    }
}

@Composable
fun SourceSheet(
    sessions: List<SessionInfo>,
    currentPkg: String?,
    currentName: String,
    onPick: (String) -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val str = LocalStrings.current
    Sheet(str.sourceTitle, onDismiss) {
        if (sessions.isEmpty()) {
            Text(str.noSession, color = Gray, fontSize = 16.sp, modifier = Modifier.padding(20.dp, 12.dp))
        }
        sessions.forEach { s ->
            SheetItem(
                s.appName,
                sub = listOf(if (s.isPlaying) str.playing else str.paused, s.title).filter { it.isNotEmpty() }.joinToString(" · "),
                icon = if (s.isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.Pause,
                selected = s.pkg == currentPkg,
            ) {
                onPick(s.pkg)
                onDismiss()
            }
        }
        if (currentPkg != null) {
            Spacer(Modifier.height(4.dp))
            SheetItem(str.openApp(currentName), icon = Icons.AutoMirrored.Rounded.OpenInNew) {
                onOpen()
                onDismiss()
            }
        }
    }
}

/**
 * Veille : tout noir, éventuellement une horloge grise qui change de place chaque minute.
 * Le premier toucher ne fait que réveiller, il n'actionne rien en dessous.
 */
@Composable
fun StandbyScreen(withClock: Boolean, pattern: String, style: ClockStyle, onWake: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onWake)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(32.dp),
    ) {
        if (!withClock) return@Box
        var bx by remember { mutableFloatStateOf(0f) }
        var by by remember { mutableFloatStateOf(-0.3f) }
        LaunchedEffect(Unit) {
            while (true) {
                val now = SystemClock.elapsedRealtime()
                delay(60_000 - now % 60_000)
                bx = Random.nextFloat() * 2 - 1
                by = Random.nextFloat() * 2 - 1
            }
        }
        Text(
            rememberFormatted(pattern, LocalLang.current.locale),
            fontSize = 72.sp,
            fontFamily = style.family,
            fontWeight = style.weight,
            color = Color(0xFF5A5A5A),
            modifier = Modifier.align(BiasAlignment(bx, by)),
        )
    }
}
