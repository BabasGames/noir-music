package fr.babas.noir

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Settings persisted in SharedPreferences and observable by Compose. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("noir", Context.MODE_PRIVATE)

    /** Language code ("" = phone language) */
    var language by string("language", "")
    var carMode by bool("car_mode", false)

    // Layout
    var showArt by bool("show_art", false)
    var swapArt by bool("swap_art", false)
    var grayArt by bool("gray_art", false)
    var orientation by int("orientation", ORIENT_AUTO)

    // Display
    var clockSize by int("clock_size", CLOCK_BIG)
    /** "", "serif", "mono", "sys:<file>" or "custom" (see ClockFonts) */
    var clockFont by string("clock_font", "")
    /** Display name of the imported file */
    var clockFontName by string("clock_font_name", "")
    /** Bumped on every import, which reloads the font even when the new file has the same name */
    var clockFontVersion by int("clock_font_version", 0)
    var clockFormat by int("clock_format", FORMAT_AUTO)
    var dateFormat by int("date_format", DATE_SHORT)
    var clockSide by int("clock_side", SIDE_AUTO)
    /** Time beside the camera hole or below it (auto: beside unless it would get too small) */
    var clockPlace by int("clock_place", PLACE_AUTO)
    /** Camera hole: position reported by Android, or set by hand when Android gets it wrong */
    var cameraPos by int("camera_pos", CAM_AUTO)
    var textScale by float("text_scale", 1f)
    var showDate by bool("show_date", true)
    var showSource by bool("show_source", true)
    var showProgress by bool("show_progress", true)
    var showVolume by bool("show_volume", true)

    // Screen
    var keepOnMode by int("keep_on_mode", KEEP_ON_PLAYING)
    var lowRefresh by bool("low_refresh", true)
    var antiBurn by bool("anti_burn", true)
    /** Index into BURN_INTERVALS / BURN_AMPLITUDES */
    var burnInterval by int("burn_interval", 1)
    var burnAmplitude by int("burn_amplitude", 1)
    /** Shown over the lock screen (controls without unlocking) */
    var showWhenLocked by bool("show_when_locked", false)
    var customBrightness by bool("custom_brightness", false)
    var brightness by float("brightness", 0.3f)

    // Night (the old "dimmed text" switch becomes "always")
    var nightMode by int("night_mode", if (sp.getBoolean("dim_text", false)) NIGHT_ALWAYS else NIGHT_OFF)
    var nightStart by int("night_start", 22)
    var nightEnd by int("night_end", 7)
    var nightBrightnessOn by bool("night_brightness_on", false)
    var nightBrightness by float("night_brightness", 0.05f)

    // Standby
    var standbyMode by int("standby_mode", STANDBY_OFF)
    /** Index into STANDBY_DELAYS */
    var standbyDelay by int("standby_delay", 1)

    // Music
    var gestures by bool("gestures", true)
    /** Package to follow first ("" = automatic) */
    var preferredApp by string("preferred_app", "")

    // Lyrics
    var lyrics by bool("lyrics", true)
    var lyricsOnMobile by bool("lyrics_mobile", false)
    /** NetEase as a fallback source: off by default, it's a non-free service */
    var lyricsFallback by bool("lyrics_fallback", false)
    var lyricsScale by float("lyrics_scale", 1f)
    /** Index into LYRICS_KEEP_DAYS: lyrics deleted if the track isn't played again within that time */
    var lyricsKeep by int("lyrics_keep", 2)
    /** Lyrics open: remembered from one track to the next, and across launches */
    var lyricsOpen by bool("lyrics_open", false)

    var tutorialDone by bool("tutorial_done", false)

    private fun <T> pref(initial: T, write: (T) -> Unit) = object : ReadWriteProperty<Any?, T> {
        var state by mutableStateOf(initial)
        override fun getValue(thisRef: Any?, property: KProperty<*>) = state
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            state = value
            write(value)
        }
    }

    private fun bool(key: String, default: Boolean) =
        pref(sp.getBoolean(key, default)) { sp.edit().putBoolean(key, it).apply() }

    private fun int(key: String, default: Int) =
        pref(sp.getInt(key, default)) { sp.edit().putInt(key, it).apply() }

    private fun float(key: String, default: Float) =
        pref(sp.getFloat(key, default)) { sp.edit().putFloat(key, it).apply() }

    private fun string(key: String, default: String) =
        pref(sp.getString(key, default) ?: default) { sp.edit().putString(key, it).apply() }

    companion object {
        const val ORIENT_AUTO = 0
        const val ORIENT_PORTRAIT = 1
        const val ORIENT_LANDSCAPE = 2

        const val CLOCK_HIDDEN = 0
        const val CLOCK_MEDIUM = 1
        const val CLOCK_BIG = 2

        const val KEEP_ON_NEVER = 0
        const val KEEP_ON_PLAYING = 1
        const val KEEP_ON_ALWAYS = 2

        const val FORMAT_AUTO = 0
        const val FORMAT_24 = 1
        const val FORMAT_12 = 2

        const val CAM_AUTO = 0
        const val CAM_LEFT = 1
        const val CAM_CENTER = 2
        const val CAM_RIGHT = 3

        const val SIDE_AUTO = 0
        const val SIDE_LEFT = 1
        const val SIDE_RIGHT = 2

        const val PLACE_AUTO = 0
        const val PLACE_BESIDE = 1
        const val PLACE_BELOW = 2

        const val DATE_SHORT = 0
        const val DATE_LONG = 1
        const val DATE_NUMERIC = 2

        const val NIGHT_OFF = 0
        const val NIGHT_ALWAYS = 1
        const val NIGHT_SCHEDULE = 2
        const val NIGHT_DARK = 3

        const val STANDBY_OFF = 0
        const val STANDBY_CLOCK = 1
        const val STANDBY_BLACK = 2
        /** Seconds */
        val STANDBY_DELAYS = listOf(60, 300, 900)

        /** Lyrics retention periods, in days (null = forever) */
        val LYRICS_KEEP_DAYS = listOf(7, 30, 182, 365, null)

        /** Anti-burn-in shift: period in seconds, amplitude in dp */
        val BURN_INTERVALS = listOf(30, 60, 300)
        val BURN_AMPLITUDES = listOf(5, 10, 20)
    }
}
