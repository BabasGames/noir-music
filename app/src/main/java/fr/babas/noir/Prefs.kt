package fr.babas.noir

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Réglages persistés dans SharedPreferences et observables par Compose. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("noir", Context.MODE_PRIVATE)

    // Disposition
    var showArt by bool("show_art", false)
    var swapArt by bool("swap_art", false)
    var grayArt by bool("gray_art", false)
    var orientation by int("orientation", ORIENT_AUTO)

    // Affichage
    var clockSize by int("clock_size", CLOCK_BIG)
    var textScale by float("text_scale", 1f)
    var dimText by bool("dim_text", false)
    var showSource by bool("show_source", true)
    var showProgress by bool("show_progress", true)
    var showVolume by bool("show_volume", true)

    // Écran
    var keepOnMode by int("keep_on_mode", KEEP_ON_PLAYING)
    var lowRefresh by bool("low_refresh", true)
    var antiBurn by bool("anti_burn", true)
    /** Index dans BURN_INTERVALS / BURN_AMPLITUDES */
    var burnInterval by int("burn_interval", 1)
    var burnAmplitude by int("burn_amplitude", 1)
    var customBrightness by bool("custom_brightness", false)
    var brightness by float("brightness", 0.3f)

    // Musique
    /** Paquet à suivre en priorité ("" = automatique) */
    var preferredApp by string("preferred_app", "")

    // Paroles
    var lyrics by bool("lyrics", true)
    var lyricsOnMobile by bool("lyrics_mobile", false)

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

        /** Décalage anti-marquage : période en secondes, amplitude en dp */
        val BURN_INTERVALS = listOf(30 to "30 s", 60 to "1 min", 300 to "5 min")
        val BURN_AMPLITUDES = listOf(5 to "Faible", 10 to "Moyen", 20 to "Fort")
    }
}
