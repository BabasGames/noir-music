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

/** Sleep timer: pauses the music at a given time or at the end of the track. */
class SleepTimer {
    /** SystemClock.elapsedRealtime() of the pause */
    var endAt by mutableStateOf<Long?>(null)
        private set
    var endOfTrack by mutableStateOf(false)
        private set
    /** Track playing when "end of track" was chosen */
    private var armedTrack: String? = null

    val active get() = endAt != null || endOfTrack

    fun startMinutes(min: Int) {
        endAt = SystemClock.elapsedRealtime() + min * 60_000L
        endOfTrack = false
    }

    fun startEndOfTrack(np: NowPlaying) {
        endAt = null
        endOfTrack = true
        armedTrack = trackKey(np)
    }

    /** The track changed since arming: the end went by without us being able to pause. */
    fun trackChanged(np: NowPlaying) = armedTrack != null && trackKey(np) != armedTrack

    private fun trackKey(np: NowPlaying) = "${np.controller?.packageName}|${np.artist}|${np.title}"

    fun cancel() {
        endAt = null
        endOfTrack = false
        armedTrack = null
    }
}

/** Place at the very top of the UI: also runs while the settings are open. */
@Composable
fun SleepTimerEffect(timer: SleepTimer, np: NowPlaying) {
    LaunchedEffect(timer.endAt, timer.endOfTrack, np) {
        val tc = np.controller?.transportControls ?: return@LaunchedEffect
        val end = timer.endAt
        // Safety net: the app moved on before our pause (crossfade, misreported
        // position…); we cut right away at the start of the next track rather than never
        if (timer.endOfTrack && timer.trackChanged(np)) {
            tc.pause()
            timer.cancel()
            return@LaunchedEffect
        }
        val wait = when {
            end != null -> end - SystemClock.elapsedRealtime()
            // A little before the very end, otherwise the next track has time to start
            timer.endOfTrack && np.isPlaying && np.durationMs > 0 ->
                ((np.durationMs - np.currentPosition()) / np.speed).toLong() - 400
            else -> return@LaunchedEffect
        }
        delay(wait.coerceAtLeast(0))
        tc.pause()
        timer.cancel()
    }
}

/** "23 min", "end of track", or null if the timer is off; updated each time a new minute starts. */
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
