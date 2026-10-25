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

/** Is night mode (dimmed text and artwork, night brightness) active right now? */
@Composable
fun rememberNightActive(prefs: Prefs): Boolean = when (prefs.nightMode) {
    Prefs.NIGHT_ALWAYS -> true
    Prefs.NIGHT_SCHEDULE -> rememberInSchedule(prefs.nightStart, prefs.nightEnd)
    Prefs.NIGHT_DARK -> rememberDarkRoom()
    else -> false
}

/** Computed right away (no bright first frame at launch), then again on each minute tick. */
@Composable
private fun rememberInSchedule(start: Int, end: Int): Boolean = remember(start, end, LocalTimeTick.current) {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    // 22 h → 7 h crosses midnight: it's "after the start OR before the end"
    if (start <= end) h in start until end else h >= start || h < end
}

/** Last known state, kept for the whole process: a recreated screen doesn't start bright again. */
private var lastDark = false

/**
 * Light sensor, with two thresholds (dark below 10 lux, bright above 30) and a delay:
 * a hand passing over the sensor or a lamp switched on for a second won't flip the screen.
 * The first reading applies at once, so a dark room doesn't get a bright screen at launch.
 * The sensor is only listened to while the app is on screen.
 */
@Composable
private fun rememberDarkRoom(): Boolean {
    val context = LocalContext.current
    val visible = LocalVisible.current
    var dark by remember { mutableStateOf(lastDark) }
    // -1 dark, 0 between the thresholds, 1 bright: only changes (and recomposes) when a threshold is crossed
    var zone by remember { mutableIntStateOf(0) }

    DisposableEffect(visible) {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (!visible || sensor == null) return@DisposableEffect onDispose {}
        var first = true
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val lux = e.values[0]
                if (first) {
                    first = false
                    // No hysteresis yet: split in the middle of the two thresholds
                    dark = lux < 20f
                    lastDark = dark
                }
                zone = if (lux < 10f) -1 else if (lux > 30f) 1 else 0
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
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
            lastDark = want
        }
    }
    return dark
}
