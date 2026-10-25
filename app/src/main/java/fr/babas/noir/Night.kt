package fr.babas.noir

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.util.Calendar

/** Le mode nuit (texte et pochette atténués, luminosité de nuit) est-il actif en ce moment ? */
@Composable
fun rememberNightActive(prefs: Prefs): Boolean = when (prefs.nightMode) {
    Prefs.NIGHT_ALWAYS -> true
    Prefs.NIGHT_SCHEDULE -> rememberInSchedule(prefs.nightStart, prefs.nightEnd)
    Prefs.NIGHT_DARK -> rememberDarkRoom()
    else -> false
}

@Composable
private fun rememberInSchedule(start: Int, end: Int): Boolean {
    var inside by remember { mutableStateOf(false) }
    LaunchedEffect(start, end) {
        while (true) {
            val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            // 22 h → 7 h passe minuit : c'est « après le début OU avant la fin »
            inside = if (start <= end) h in start until end else h >= start || h < end
            val now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    return inside
}

/**
 * Capteur de luminosité, avec deux seuils (sombre sous 10 lux, clair au-dessus de 30) et un délai :
 * une main qui passe devant le capteur ou une lampe allumée une seconde ne font pas basculer l'écran.
 */
@Composable
private fun rememberDarkRoom(): Boolean {
    val context = LocalContext.current
    var dark by remember { mutableStateOf(false) }
    // -1 sombre, 0 entre les deux seuils, 1 clair : ne change (et ne recompose) qu'en franchissant un seuil
    var zone by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val lux = e.values[0]
                zone = if (lux < 10f) -1 else if (lux > 30f) 1 else 0
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(listener) }
    }
    LaunchedEffect(zone) {
        val want = when (zone) {
            -1 -> true
            1 -> false
            else -> return@LaunchedEffect
        }
        if (want != dark) {
            delay(4000)
            dark = want
        }
    }
    return dark
}
