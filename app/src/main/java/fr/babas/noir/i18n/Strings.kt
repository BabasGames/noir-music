package fr.babas.noir.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/** UI languages, in picker order; each name is written in the language itself. */
enum class Lang(val code: String, val nativeName: String, val strings: Strings) {
    FR("fr", "Français", Fr),
    EN("en", "English", En),
    ES("es", "Español", Es),
    DE("de", "Deutsch", De),
    IT("it", "Italiano", It),
    PT("pt", "Português", Pt),
    NL("nl", "Nederlands", Nl),
    PL("pl", "Polski", Pl),
    TR("tr", "Türkçe", Tr),
    RU("ru", "Русский", Ru),
    UK("uk", "Українська", Uk),
    JA("ja", "日本語", Ja),
    ZH("zh", "中文（简体）", Zh),
    KO("ko", "한국어", Ko);

    val locale: Locale get() = Locale.forLanguageTag(code)

    companion object {
        /** "" = phone language if it's translated, otherwise English. */
        fun resolve(pref: String): Lang {
            val code = pref.ifEmpty { Locale.getDefault().language }
            return entries.firstOrNull { it.code == code } ?: EN
        }
    }
}

val LocalStrings = staticCompositionLocalOf<Strings> { Fr }
val LocalLang = staticCompositionLocalOf { Lang.FR }

/**
 * All visible texts. One language = one object implementing this interface:
 * a missing text is a compile error, not a hole in the UI.
 */
interface Strings {
    // ── Player ───
    val nothingPlaying: String
    val startMusicHint: String
    val settings: String
    val back: String
    val previous: String
    val next: String
    val play: String
    val pause: String
    val volumeDown: String
    val volumeUp: String
    val lyrics: String
    val carMode: String
    val charging: String
    val sleepTimer: String
    val like: String

    // ── Lyrics (messages) ───
    val lyricsOffline: String
    val lyricsUnavailable: String
    val lyricsMobileData: String
    val lyricsInstrumental: String
    val lyricsNotFound: String

    // ── Timer and source ────
    val sleepTimerTitle: String
    fun minutes(n: Int): String
    val oneHour: String
    val endOfTrack: String
    val stopTimer: String
    val sourceTitle: String
    val noSession: String
    val playing: String
    val paused: String
    fun openApp(name: String): String

    // ── Access screen ──
    val accessText: String
    val openSetting: String

    // ── Settings: language and layout ───────
    val language: String
    val languageAuto: String
    val sectionLayout: String
    val showArt: String
    val swapArt: String
    val swapArtSub: String
    val grayArt: String
    val orientation: String
    val orientAuto: String
    val orientPortrait: String
    val orientLandscape: String
    val carModeSub: String

    // ── Settings: display ───────
    val sectionDisplay: String
    val clock: String
    val clockHidden: String
    val clockMedium: String
    val clockBig: String
    val clockFont: String
    val clockFormat: String
    val formatAuto: String
    val format24: String
    val format12: String
    val dateFormat: String
    val dateShort: String
    val dateLong: String
    val dateNumeric: String
    val fontSystem: String
    val fontSerif: String
    val fontMono: String
    val fontPickFile: String
    fun fontFile(name: String): String
    val fontFileError: String
    val textSize: String
    val sizeSmall: String
    val sizeNormal: String
    val sizeLarge: String
    val sizeHuge: String
    val dateBattery: String
    val dateBatterySub: String
    val sourceName: String
    val sourceNameSub: String
    val progressBar: String
    val volume: String

    // ── Settings: night ────
    val sectionNight: String
    val nightMode: String
    val off: String
    val always: String
    val schedule: String
    val dark: String
    val nightSubDefault: String
    val nightSubSchedule: String
    val nightSubDark: String
    val start: String
    val end: String
    fun hour(h: Int): String
    val minus: String
    val plus: String
    val nightBrightness: String
    val nightBrightnessSub: String

    // ── Settings: screen ────
    val sectionScreen: String
    val keepOn: String
    val keepOnPlaying: String
    val lowRefresh: String
    fun refreshInfo(current: Int, available: String): String
    val burnShift: String
    val burnShiftSub: String
    val frequency: String
    val amplitude: String
    val ampLow: String
    val ampMid: String
    val ampHigh: String
    val standby: String
    val standbyClock: String
    val standbyBlack: String
    val after: String
    val customBrightness: String
    val customBrightnessSub: String

    // ── Settings: music ───────
    val sectionMusic: String
    val gestures: String
    val gesturesSub: String
    val preferredApp: String
    val preferredAppSub: String
    val automaticPlaying: String

    // ── Settings: lyrics ──────
    val lyricsSub: String
    val onMobile: String
    val onMobileSub: String
    val lyricsSize: String
    val notificationAccess: String
    val replayTutorial: String

    // ── Tutorial ──
    val skip: String
    val nextPage: String
    val letsGo: String
    val tutoLanguageTitle: String
    val welcome1: String
    val welcome2: String
    val accessGranted: String
    val allowAccess: String
    val gestureSwipeH: String
    val gestureSwipeV: String
    val gestureTapTitle: String
    val gestureTapTitleAction: String
    val gestureDoubleTap: String
    val gestureDoubleTapAction: String
    val gestureLongPress: String
    val gestureLongPressAction: String
    val gestureTapClock: String
    val gestureTapClockAction: String
    val gestureTapApp: String
    val gestureTapAppAction: String
    val tutoLyrics: String
    val downloadOnMobile: String
    val tutoCarTitle: String
    val tutoCar1: String
    val tutoCar2: String
    val tutoScreenTitle: String
    val tutoScreen1: String
    val tutoScreen2: String
    val tutoScreen3: String

    // ── Additions: time position, lyrics reliability ─────────
    val clockSide: String
    val sideLeft: String
    val sideRight: String
    val clockSideSub: String
    val lyricsDisclaimer: String
    val lyricsNothingOther: String
    val camera: String
    val center: String
    val cameraSub: String
    val tutoOpenTitle: String
    val tutoOpen1: String
    val tutoOpen2: String
    val openGithub: String

    // ── Lyrics retention ──────────
    val lyricsKeep: String
    val lyricsKeepSub: String
    val keepWeek: String
    val keepMonth: String
    val keepHalfYear: String
    val keepYear: String
    val keepForever: String
    fun lyricsCacheInfo(count: Int, size: String): String
    val clearLyrics: String
}
