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

    var showArt by bool("show_art", false)
    var grayArt by bool("gray_art", false)
    var keepOn by bool("keep_on", true)
    var lowRefresh by bool("low_refresh", true)
    var antiBurn by bool("anti_burn", true)
    var customBrightness by bool("custom_brightness", false)
    var brightness by float("brightness", 0.3f)

    private fun bool(key: String, default: Boolean) = object : ReadWriteProperty<Any?, Boolean> {
        var state by mutableStateOf(sp.getBoolean(key, default))
        override fun getValue(thisRef: Any?, property: KProperty<*>) = state
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            state = value
            sp.edit().putBoolean(key, value).apply()
        }
    }

    private fun float(key: String, default: Float) = object : ReadWriteProperty<Any?, Float> {
        var state by mutableStateOf(sp.getFloat(key, default))
        override fun getValue(thisRef: Any?, property: KProperty<*>) = state
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            state = value
            sp.edit().putFloat(key, value).apply()
        }
    }
}
