package fr.babas.noir

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.produceState
import android.text.format.Formatter
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.babas.noir.i18n.Lang
import fr.babas.noir.i18n.LocalLang
import fr.babas.noir.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: Prefs,
    lyricsRepo: LyricsRepository,
    displayInfo: Pair<Int, String>,
    candidateApps: List<Pair<String, String>>,
    onOpenAccess: () -> Unit,
    onTutorial: () -> Unit,
    onPreviewBrightness: (Float?) -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    var languageSheet by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // Fixed header: "← Settings" stays reachable without scrolling all the way up
            Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, s.back, tint = Color.White)
                }
                Text(s.settings, fontSize = 22.sp, color = Color.White)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                LanguageRow(prefs) { languageSheet = true }

                Section(s.sectionLayout)
                Toggle(s.carMode, s.carModeSub, prefs.carMode) { prefs.carMode = it }
                Toggle(s.showArt, null, prefs.showArt) { prefs.showArt = it }
                Toggle(s.swapArt, s.swapArtSub, prefs.swapArt, enabled = prefs.showArt) { prefs.swapArt = it }
                Toggle(s.grayArt, null, prefs.grayArt, enabled = prefs.showArt) { prefs.grayArt = it }
                Choice(s.orientation, listOf(s.orientAuto, s.orientPortrait, s.orientLandscape), prefs.orientation) {
                    prefs.orientation = it
                }

                Section(s.sectionDisplay)
                Choice(s.clock, listOf(s.clockHidden, s.clockMedium, s.clockBig), prefs.clockSize) { prefs.clockSize = it }
                Choice(s.clockFormat, listOf(s.formatAuto, s.format24, s.format12), prefs.clockFormat) {
                    prefs.clockFormat = it
                }
                Choice(s.camera, listOf(s.formatAuto, s.sideLeft, s.center, s.sideRight), prefs.cameraPos) {
                    prefs.cameraPos = it
                }
                Text(s.cameraSub, fontSize = 13.sp, color = Gray)
                Choice(s.clockSide, listOf(s.formatAuto, s.sideLeft, s.sideRight), prefs.clockSide) { prefs.clockSide = it }
                Text(s.clockSideSub, fontSize = 13.sp, color = Gray)
                ClockFontPicker(prefs)
                Choice(
                    s.textSize,
                    textScaleLabels(),
                    TEXT_SCALES.indexOfFirst { it == prefs.textScale }.coerceAtLeast(0),
                ) { prefs.textScale = TEXT_SCALES[it] }
                Toggle(s.dateBattery, s.dateBatterySub, prefs.showDate) { prefs.showDate = it }
                if (prefs.showDate) {
                    Choice(s.dateFormat, listOf(s.dateShort, s.dateLong, s.dateNumeric), prefs.dateFormat) {
                        prefs.dateFormat = it
                    }
                    Text(
                        rememberFormatted(datePattern(prefs), LocalLang.current.locale),
                        fontSize = 15.sp,
                        color = Gray,
                    )
                }
                Toggle(s.sourceName, s.sourceNameSub, prefs.showSource) { prefs.showSource = it }
                Toggle(s.progressBar, null, prefs.showProgress) { prefs.showProgress = it }
                Toggle(s.volume, null, prefs.showVolume) { prefs.showVolume = it }

                Section(s.sectionNight)
                Choice(s.nightMode, listOf(s.off, s.always, s.schedule, s.dark), prefs.nightMode) { prefs.nightMode = it }
                Text(
                    when (prefs.nightMode) {
                        Prefs.NIGHT_SCHEDULE -> s.nightSubSchedule
                        Prefs.NIGHT_DARK -> s.nightSubDark
                        else -> s.nightSubDefault
                    },
                    fontSize = 13.sp,
                    color = Gray,
                )
                if (prefs.nightMode == Prefs.NIGHT_SCHEDULE) {
                    HourStepper(s.start, prefs.nightStart) { prefs.nightStart = it }
                    HourStepper(s.end, prefs.nightEnd) { prefs.nightEnd = it }
                }
                Toggle(
                    s.nightBrightness,
                    s.nightBrightnessSub,
                    prefs.nightBrightnessOn,
                    enabled = prefs.nightMode != Prefs.NIGHT_OFF,
                ) { prefs.nightBrightnessOn = it }
                if (prefs.nightBrightnessOn && prefs.nightMode != Prefs.NIGHT_OFF) {
                    // While dragging, the screen takes this brightness so the result can be judged
                    BrightnessSlider(prefs.nightBrightness, 0.01f..0.5f, onPreviewBrightness) { prefs.nightBrightness = it }
                }

                Section(s.sectionScreen)
                Choice(s.keepOn, listOf(s.off, s.keepOnPlaying, s.always), prefs.keepOnMode) { prefs.keepOnMode = it }
                Toggle(
                    s.lowRefresh,
                    s.refreshInfo(displayInfo.first, displayInfo.second),
                    prefs.lowRefresh,
                ) { prefs.lowRefresh = it }
                Toggle(s.burnShift, s.burnShiftSub, prefs.antiBurn) { prefs.antiBurn = it }
                if (prefs.antiBurn) {
                    Choice(s.frequency, Prefs.BURN_INTERVALS.map { it.second }, prefs.burnInterval) { prefs.burnInterval = it }
                    Choice(
                        s.amplitude,
                        listOf(s.ampLow, s.ampMid, s.ampHigh).zip(Prefs.BURN_AMPLITUDES) { label, (dp, _) -> "$label (±$dp)" },
                        prefs.burnAmplitude,
                    ) { prefs.burnAmplitude = it }
                }
                Choice(s.standby, listOf(s.off, s.standbyClock, s.standbyBlack), prefs.standbyMode) {
                    prefs.standbyMode = it
                }
                if (prefs.standbyMode != Prefs.STANDBY_OFF) {
                    Choice(s.after, Prefs.STANDBY_DELAYS.map { it.second }, prefs.standbyDelay) { prefs.standbyDelay = it }
                }
                Toggle(s.customBrightness, s.customBrightnessSub, prefs.customBrightness) {
                    prefs.customBrightness = it
                }
                if (prefs.customBrightness) {
                    BrightnessSlider(prefs.brightness, 0.01f..1f, onPreviewBrightness) { prefs.brightness = it }
                }

                Section(s.sectionMusic)
                Toggle(s.gestures, s.gesturesSub, prefs.gestures) { prefs.gestures = it }
                Text(s.preferredApp, fontSize = 17.sp, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                Text(s.preferredAppSub, fontSize = 13.sp, color = Gray)
                Spacer(Modifier.height(4.dp))
                val apps = listOf("" to s.automaticPlaying) + candidateApps
                apps.forEach { (pkg, label) ->
                    Radio(label, selected = prefs.preferredApp == pkg) { prefs.preferredApp = pkg }
                }
                // App picked earlier but no longer detected: keep it visible so it can be removed
                if (prefs.preferredApp.isNotEmpty() && apps.none { it.first == prefs.preferredApp }) {
                    Radio(prefs.preferredApp, selected = true) {}
                }

                Section(s.lyrics)
                Toggle(s.lyrics, s.lyricsSub, prefs.lyrics) { prefs.lyrics = it }
                Text(s.lyricsDisclaimer, fontSize = 13.sp, color = Gray)
                Toggle(s.onMobile, s.onMobileSub, prefs.lyricsOnMobile, enabled = prefs.lyrics) { prefs.lyricsOnMobile = it }
                Toggle(s.lyricsFallback, s.lyricsFallbackSub, prefs.lyricsFallback, enabled = prefs.lyrics) {
                    prefs.lyricsFallback = it
                }
                LyricsStorage(prefs, lyricsRepo)
                Choice(
                    s.lyricsSize,
                    textScaleLabels(),
                    TEXT_SCALES.indexOfFirst { it == prefs.lyricsScale }.coerceAtLeast(0),
                ) { prefs.lyricsScale = TEXT_SCALES[it] }

                Spacer(Modifier.height(24.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenAccess) { Text(s.notificationAccess, color = Color.White) }
                    OutlinedButton(onClick = onTutorial) { Text(s.replayTutorial, color = Color.White) }
                    val uri = LocalUriHandler.current
                    OutlinedButton(onClick = { uri.openUri(GITHUB_URL) }) { Text(s.openGithub, color = Color.White) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        if (languageSheet) {
            Sheet(s.language, onDismiss = { languageSheet = false }) {
                LanguageItems(prefs) { languageSheet = false }
            }
        }
    }
}

val TEXT_SCALES = listOf(0.85f, 1f, 1.25f, 1.5f)

/** Lyrics retention period, space used, and deletion of everything saved. */
@Composable
private fun LyricsStorage(prefs: Prefs, repo: LyricsRepository) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableStateOf(0) }
    val stats by produceState(0 to 0L, prefs.lyricsKeep, refresh) {
        // The purge for the new setting runs in parallel: let it finish before counting
        delay(300)
        value = repo.stats()
    }
    Choice(
        s.lyricsKeep,
        listOf(s.keepWeek, s.keepMonth, s.keepHalfYear, s.keepYear, s.keepForever),
        prefs.lyricsKeep,
    ) { prefs.lyricsKeep = it }
    Text(s.lyricsKeepSub, fontSize = 13.sp, color = Gray)
    Text(
        s.lyricsCacheInfo(stats.first, Formatter.formatShortFileSize(context, stats.second)),
        fontSize = 13.sp,
        color = Gray,
        modifier = Modifier.padding(top = 8.dp),
    )
    OutlinedButton(
        onClick = {
            scope.launch {
                repo.clear()
                refresh++
            }
        },
        modifier = Modifier.padding(top = 4.dp),
    ) { Text(s.clearLyrics, color = Color.White) }
}

@Composable
private fun textScaleLabels(): List<String> {
    val s = LocalStrings.current
    return listOf(s.sizeSmall, s.sizeNormal, s.sizeLarge, s.sizeHuge)
}

/** First settings row: the current language, tap it to change. */
@Composable
private fun LanguageRow(prefs: Prefs, onClick: () -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(s.language, fontSize = 17.sp, color = Color.White, modifier = Modifier.weight(1f))
        Text(
            if (prefs.language.isEmpty()) s.languageAuto else LocalLang.current.nativeName,
            fontSize = 15.sp,
            color = Gray,
        )
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Gray)
    }
}

/** List of languages, each written in its own language; used by the settings and the tutorial. */
@Composable
fun LanguageItems(prefs: Prefs, onPicked: () -> Unit = {}) {
    val s = LocalStrings.current
    SheetItem(s.languageAuto, selected = prefs.language.isEmpty()) {
        prefs.language = ""
        onPicked()
    }
    Lang.entries.forEach { lang ->
        SheetItem(lang.nativeName, selected = prefs.language == lang.code) {
            prefs.language = lang.code
            onPicked()
        }
    }
}

/** Preview of the time in the chosen font, then the list of fonts, each shown in its own style. */
@Composable
private fun ClockFontPicker(prefs: Prefs) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { ClockFonts.import(context, uri) }
            error = !ok
            if (ok) {
                prefs.clockFontName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "?"
                prefs.clockFont = ClockFonts.CUSTOM
            }
        }
    }
    val ids = remember(prefs.clockFontName) { ClockFonts.ids(context) }

    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(s.clockFont, fontSize = 17.sp, color = Color.White)
        val style = rememberClockStyle(prefs)
        Text(
            rememberFormatted(clockPattern(prefs), LocalLang.current.locale),
            fontSize = 72.sp,
            lineHeight = 80.sp,
            fontFamily = style.family,
            fontWeight = style.weight,
            color = Color.White,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        ids.forEach { id ->
            val st = remember(id, prefs.clockFontName) { ClockFonts.style(context, id, prefs.clockFontName) }
            Radio(
                ClockFonts.label(id, s, prefs.clockFontName),
                selected = prefs.clockFont == id,
                fontFamily = st.family,
            ) { prefs.clockFont = id }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { picker.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream")) }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Rounded.Add, null, tint = Gray, modifier = Modifier.size(20.dp))
            Text(s.fontPickFile, fontSize = 16.sp, color = Gray)
        }
        if (error) Text(s.fontFileError, fontSize = 13.sp, color = Color(0xFFB0B0B0))
    }
}

@Composable
private fun BrightnessSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onPreview: (Float?) -> Unit,
    onChange: (Float) -> Unit,
) {
    Slider(
        value = value,
        onValueChange = {
            onChange(it)
            onPreview(it)
        },
        onValueChangeFinished = { onPreview(null) },
        valueRange = range,
        colors = noirSliderColors(),
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(title.uppercase(), fontSize = 12.sp, letterSpacing = 1.5.sp, color = Gray)
    Spacer(Modifier.height(4.dp))
}

/** Row of mutually exclusive choices, segmented-button style. */
@Composable
private fun Choice(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(title, fontSize = 17.sp, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Dim, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50)),
        ) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                Box(
                    Modifier
                        .weight(1f)
                        .background(if (on) Color.White else Color.Black)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        color = if (on) Color.Black else Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun Radio(label: String, selected: Boolean, fontFamily: FontFamily? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .border(2.dp, if (selected) Color.White else Dim, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else Color.Transparent),
        )
        Text(label, fontSize = 16.sp, color = if (selected) Color.White else Gray, fontFamily = fontFamily)
    }
}

/** Hour from 0 to 23 set with − / +, which wraps around (23 h + 1 = 0 h). */
@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    val s = LocalStrings.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, color = Color.White, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((hour + 23) % 24) }) { Icon(Icons.Rounded.Remove, s.minus, tint = Gray) }
        Text(s.hour(hour), fontSize = 17.sp, color = Color.White, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onChange((hour + 1) % 24) }) { Icon(Icons.Rounded.Add, s.plus, tint = Gray) }
    }
}

@Composable
fun Toggle(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = if (enabled) Color.White else Dim)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Gray,
                uncheckedTrackColor = Color.Black,
                uncheckedBorderColor = Dim,
            ),
        )
    }
}

@Composable
fun AccessScreen(onGrant: () -> Unit) {
    val s = LocalStrings.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Noir Music", fontSize = 40.sp, fontWeight = FontWeight.Thin, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text(s.accessText, color = Gray, fontSize = 16.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onGrant) { Text(s.openSetting, color = Color.White) }
    }
}
