package fr.babas.noir

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import fr.babas.noir.i18n.Lang
import fr.babas.noir.i18n.LocalLang
import fr.babas.noir.i18n.LocalStrings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private lateinit var prefs: Prefs
    private lateinit var media: MediaRepository
    private lateinit var lyrics: LyricsRepository
    private var hasAccess by mutableStateOf(false)
    private var onScreen by mutableStateOf(false)
    private var timeTick by mutableIntStateOf(0)
    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            timeTick++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)
        media = MediaRepository(this) { prefs.preferredApp }
        lyrics = LyricsRepository(this)

        // The media sessions stay followed in the background while the sleep timer runs; let go once it's done
        lifecycleScope.launch {
            snapshotFlow { sleep.active }.collect { active ->
                if (!active && !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) media.stop()
            }
        }

        setContent {
            val lang = Lang.resolve(prefs.language)
            CompositionLocalProvider(
                LocalLang provides lang,
                LocalStrings provides lang.strings,
                LocalVisible provides onScreen,
                LocalTimeTick provides timeTick,
                LocalAnimations provides prefs.animations,
            ) {
            MaterialTheme(colorScheme = NoirColors) {
                val np by media.state.collectAsState()
                var showSettings by rememberSaveable { mutableStateOf(false) }

                val keepOn = when (prefs.keepOnMode) {
                    Prefs.KEEP_ON_ALWAYS -> true
                    Prefs.KEEP_ON_PLAYING -> np.isPlaying
                    else -> false
                }
                val night = rememberNightActive(prefs)
                // Preview while a brightness slider is being dragged in the settings
                var preview by remember { mutableStateOf<Float?>(null) }
                val brightness = preview ?: when {
                    night && prefs.nightBrightnessOn -> prefs.nightBrightness
                    prefs.customBrightness -> prefs.brightness
                    else -> null
                }
                // Reduced refresh rate only on the player: tutorial and settings scroll at the normal rate
                val onPlayer = prefs.tutorialDone && hasAccess && !showSettings
                val lowRefresh = prefs.lowRefresh && onPlayer
                LaunchedEffect(keepOn, lowRefresh, brightness) {
                    applyWindow(keepOn, lowRefresh, brightness)
                }
                LaunchedEffect(prefs.orientation) {
                    requestedOrientation = when (prefs.orientation) {
                        Prefs.ORIENT_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        Prefs.ORIENT_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                    }
                }
                LaunchedEffect(prefs.preferredApp) { media.refresh() }
                LaunchedEffect(prefs.showWhenLocked) { setShowWhenLocked(prefs.showWhenLocked) }
                LaunchedEffect(prefs.lyricsKeep) {
                    // Also daily: the app can stay open for days
                    while (true) {
                        lyrics.purge(Prefs.LYRICS_KEEP_DAYS.getOrNull(prefs.lyricsKeep))
                        delay(24 * 3600_000L)
                    }
                }
                SleepTimerEffect(sleep, np)

                when {
                    !prefs.tutorialDone -> TutorialScreen(prefs, hasAccess, ::openListenerSettings) {
                        prefs.tutorialDone = true
                    }
                    !hasAccess -> AccessScreen(onGrant = ::openListenerSettings)
                    showSettings -> {
                        BackHandler { showSettings = false }
                        SettingsScreen(
                            prefs = prefs,
                            lyricsRepo = lyrics,
                            // What the player will use (the settings always run at full refresh rate)
                            displayInfo = rememberDisplayInfo().let { (cur, rates) ->
                                (if (prefs.lowRefresh) lowestMode()?.refreshRate?.let(Math::round) ?: cur else cur) to rates
                            },
                            candidateApps = remember { media.candidateApps() },
                            onOpenAccess = ::openListenerSettings,
                            onTutorial = {
                                showSettings = false
                                prefs.tutorialDone = false
                            },
                            onPreviewBrightness = { preview = it },
                            onBack = { showSettings = false },
                        )
                    }
                    else -> PlayerScreen(
                        np, prefs, lyrics, night, sleep,
                        sessions = media.sessions.collectAsState().value,
                        onPickSession = media::choose,
                        onOpenSettings = { whenUnlocked { showSettings = true } },
                        onOpenSource = { np.controller?.packageName?.let { pkg -> whenUnlocked { openApp(pkg) } } },
                    )
                }
            }
            }
        }
    }

    companion object {
        /** Process-wide: survives the activity being recreated (system language change…), shared with the screen saver. */
        val sleep = SleepTimer()
    }

    /** Over the lock screen, settings and other apps first ask to unlock. */
    private fun whenUnlocked(action: () -> Unit) {
        val km = getSystemService(KeyguardManager::class.java)
        if (!km.isKeyguardLocked) return action()
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = action()
        })
    }

    private fun openApp(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let(::startActivity)
    }

    override fun onStart() {
        super.onStart()
        onScreen = true
        // The screen may have been off for a while: catch up with the time right away
        timeTick++
        ContextCompat.registerReceiver(
            this,
            timeReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStop() {
        super.onStop()
        onScreen = false
        unregisterReceiver(timeReceiver)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        connect(attempt = 0)
    }

    /** Right after access is granted, the system may refuse for a few moments: retry. */
    private fun connect(attempt: Int) {
        val granted = packageName in NotificationManagerCompat.getEnabledListenerPackages(this)
        hasAccess = granted && media.start()
        if (granted && !hasAccess && attempt < 10) {
            window.decorView.postDelayed({ if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) connect(attempt + 1) }, 300)
        }
    }

    override fun onPause() {
        super.onPause()
        // "End of track" must keep seeing track changes while another app is in front
        if (!sleep.active) media.stop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun openListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun applyWindow(keepOn: Boolean, lowRefresh: Boolean, brightness: Float?) {
        if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val low = if (lowRefresh) lowestMode() else null
        window.attributes = window.attributes.apply {
            screenBrightness = brightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            // 0 = let the system choose
            preferredDisplayModeId = low?.modeId ?: 0
        }
        // The mode alone is only a starting point: SurfaceFlinger goes back up to 60 Hz as long as no
        // view votes for less. Android 15+: each view can vote for its own frame rate.
        if (Build.VERSION.SDK_INT >= 35) {
            val rate = low?.refreshRate ?: Float.NaN // NaN = no preference
            window.decorView.post { setFrameRateVote(window.decorView, rate) }
        }
    }

    @RequiresApi(35)
    private fun setFrameRateVote(view: View, rate: Float) {
        view.requestedFrameRate = rate
        if (view is ViewGroup) for (i in 0 until view.childCount) setFrameRateVote(view.getChildAt(i), rate)
    }

    // Activity.getDisplay() only exists from Android 11 (API 30); calling it on Android 10 crashes
    @Suppress("DEPRECATION")
    private fun currentDisplay(): Display =
        if (Build.VERSION.SDK_INT >= 30) display ?: windowManager.defaultDisplay else windowManager.defaultDisplay

    /** Slowest mode at the current resolution (changing resolution would make the screen flicker). */
    private fun lowestMode(): Display.Mode? {
        val d = currentDisplay()
        val cur = d.mode
        return d.supportedModes
            .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .minByOrNull { it.refreshRate }
    }

    /**
     * Current refresh rate and list of supported rates ("30, 60, 90, 120").
     * Tracked live: the mode change lands after the fact, so a one-off read showed the old value.
     */
    @Composable
    private fun rememberDisplayInfo(): Pair<Int, String> {
        val rates = remember {
            currentDisplay().supportedModes.map { Math.round(it.refreshRate) }.distinct().sorted().joinToString(", ")
        }
        var current by remember { mutableIntStateOf(Math.round(currentDisplay().refreshRate)) }
        DisposableEffect(Unit) {
            val dm = getSystemService(DisplayManager::class.java)
            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayChanged(id: Int) {
                    current = Math.round(currentDisplay().refreshRate)
                }
                override fun onDisplayAdded(id: Int) {}
                override fun onDisplayRemoved(id: Int) {}
            }
            dm.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
            onDispose { dm.unregisterDisplayListener(listener) }
        }
        return current to rates
    }
}

private val NoirColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFF9A9A9A),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF9A9A9A),
    outline = Color(0xFF333333),
)
