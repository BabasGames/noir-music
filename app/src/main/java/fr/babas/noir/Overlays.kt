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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

/** Dialog centered over the player; tapping outside or "back" closes it. */
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
                // Tapping inside the dialog must not close it
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
fun SleepSheet(timer: SleepTimer, np: NowPlaying, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    Sheet(s.sleepTimerTitle, onDismiss) {
        listOf(15, 30, 45, 60).forEach { min ->
            SheetItem(if (min == 60) s.oneHour else s.minutes(min)) {
                timer.startMinutes(min)
                onDismiss()
            }
        }
        SheetItem(s.endOfTrack, selected = timer.endOfTrack) {
            timer.startEndOfTrack(np)
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
 * Standby: all black, optionally a grey clock at the exact center of the screen, which shifts by
 * a few dp like the rest of the app ("Pixel shift" setting: amplitude and interval).
 * The first tap only wakes the screen, it doesn't trigger anything underneath.
 */
@Composable
fun StandbyScreen(
    withClock: Boolean,
    pattern: String,
    style: ClockStyle,
    shiftDp: Int,
    shiftEveryMs: Long,
    onWake: () -> Unit,
) {
    // Full screen, without reserving the camera strip: otherwise the "center" drops by half that strip
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onWake),
        contentAlignment = Alignment.Center,
    ) {
        if (!withClock) return@Box
        var tx by remember { mutableFloatStateOf(0f) }
        var ty by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(shiftDp, shiftEveryMs) {
            tx = 0f
            ty = 0f
            while (shiftDp > 0) {
                val now = SystemClock.elapsedRealtime()
                delay(shiftEveryMs - now % shiftEveryMs)
                tx = Random.nextInt(-shiftDp, shiftDp + 1).toFloat()
                ty = Random.nextInt(-shiftDp, shiftDp + 1).toFloat()
            }
        }
        val dx by animateFloatAsState(tx, motion(tween(2500)), label = "standby x")
        val dy by animateFloatAsState(ty, motion(tween(2500)), label = "standby y")

        val density = LocalDensity.current
        val size = 96.sp
        // A text's box isn't its ink: room below the baseline (for "p", "g"…),
        // digit side bearings (some fonts draw the "1" off-center in its cell). We compare the
        // actual layout with the exact outline of the displayed digits, and recenter the ink.
        val text = rememberFormatted(pattern, LocalLang.current.locale)
        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
        val ink = remember(text, style, density) {
            val paint = Paint().apply {
                typeface = style.typeface
                textSize = with(density) { size.toPx() }
            }
            android.graphics.Rect().also { paint.getTextBounds(text, 0, text.length, it) }
        }
        Text(
            text,
            fontSize = size,
            fontFamily = style.family,
            fontWeight = style.weight,
            color = Color(0xFF5A5A5A),
            onTextLayout = { layout = it },
            modifier = Modifier.graphicsLayer {
                val l = layout
                val inkX = if (l == null) 0f else l.size.width / 2f - (ink.left + ink.right) / 2f
                val inkY = if (l == null) 0f else l.size.height / 2f - (l.firstBaseline + (ink.top + ink.bottom) / 2f)
                translationX = dx.dp.toPx() + inkX
                translationY = dy.dp.toPx() + inkY
            },
        )
    }
}
