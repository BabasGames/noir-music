package fr.babas.noir

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.babas.noir.i18n.LocalStrings
import kotlinx.coroutines.delay

/** Minuteur de sommeil : met la musique en pause à une heure donnée ou à la fin du morceau. */
class SleepTimer {
    /** SystemClock.elapsedRealtime() de la mise en pause */
    var endAt by mutableStateOf<Long?>(null)
        private set
    var endOfTrack by mutableStateOf(false)
        private set

    val active get() = endAt != null || endOfTrack

    fun startMinutes(min: Int) {
        endAt = SystemClock.elapsedRealtime() + min * 60_000L
        endOfTrack = false
    }

    fun startEndOfTrack() {
        endAt = null
        endOfTrack = true
    }

    fun cancel() {
        endAt = null
        endOfTrack = false
    }
}

/** À placer tout en haut de l'interface : tourne aussi pendant que les réglages sont ouverts. */
@Composable
fun SleepTimerEffect(timer: SleepTimer, np: NowPlaying) {
    LaunchedEffect(timer.endAt, timer.endOfTrack, np) {
        val tc = np.controller?.transportControls ?: return@LaunchedEffect
        val end = timer.endAt
        val wait = when {
            end != null -> end - SystemClock.elapsedRealtime()
            // Un peu avant la toute fin, sinon le morceau suivant a le temps de démarrer
            timer.endOfTrack && np.isPlaying && np.durationMs > 0 ->
                ((np.durationMs - np.currentPosition()) / np.speed).toLong() - 400
            else -> return@LaunchedEffect
        }
        delay(wait.coerceAtLeast(0))
        tc.pause()
        timer.cancel()
    }
}

/** « 23 min », « fin du morceau », ou null si le minuteur est arrêté ; mis à jour à chaque minute entamée. */
@Composable
fun rememberSleepLabel(timer: SleepTimer): String? {
    val str = LocalStrings.current
    val end = timer.endAt
    var label by remember(end) { mutableStateOf("") }
    LaunchedEffect(end, str) {
        while (end != null) {
            val left = end - SystemClock.elapsedRealtime()
            if (left <= 0) break
            label = str.minutes(((left + 59_999) / 60_000).toInt())
            delay((left - 1) % 60_000 + 1)
        }
    }
    return when {
        timer.endOfTrack -> str.endOfTrack
        end != null -> label
        else -> null
    }
}
