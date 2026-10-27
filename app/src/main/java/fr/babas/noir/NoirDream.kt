package fr.babas.noir

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.dreams.DreamService
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import fr.babas.noir.i18n.Lang
import fr.babas.noir.i18n.LocalLang
import fr.babas.noir.i18n.LocalStrings

/**
 * Android screen saver ("Screen saver" in the system display settings): the player shows up on its own
 * while the phone charges or sits on a dock. Interactive: the controls work, back or power leaves it.
 * A DreamService isn't an activity, so Compose needs its lifecycle and saved state owners set by hand.
 */
class NoirDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    private lateinit var prefs: Prefs
    private lateinit var media: MediaRepository
    private lateinit var lyrics: LyricsRepository
    private var timeTick by mutableIntStateOf(0)
    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            timeTick++
        }
    }

    override fun onCreate() {
        super.onCreate()
        saved.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
        prefs = Prefs(this)
        media = MediaRepository(this) { prefs.preferredApp }
        lyrics = LyricsRepository(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true
        val view = ComposeView(this)
        window.decorView.setViewTreeLifecycleOwner(this)
        window.decorView.setViewTreeSavedStateRegistryOwner(this)
        view.setContent {
            val lang = Lang.resolve(prefs.language)
            CompositionLocalProvider(
                LocalLang provides lang,
                LocalStrings provides lang.strings,
                LocalTimeTick provides timeTick,
                LocalAnimations provides prefs.animations,
                LocalDream provides true,
            ) {
                MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color.Black)) {
                    val np by media.state.collectAsState()
                    val night = rememberNightActive(prefs)
                    val brightness = when {
                        night && prefs.nightBrightnessOn -> prefs.nightBrightness
                        prefs.customBrightness -> prefs.brightness
                        else -> null
                    }
                    LaunchedEffect(brightness) {
                        window.attributes = window.attributes.apply {
                            screenBrightness = brightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        }
                    }
                    SleepTimerEffect(MainActivity.sleep, np)
                    PlayerScreen(
                        np, prefs, lyrics, night, MainActivity.sleep,
                        sessions = media.sessions.collectAsState().value,
                        onPickSession = media::choose,
                        onOpenSettings = {},
                        onOpenSource = {},
                    )
                }
            }
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        registry.currentState = Lifecycle.State.RESUMED
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
        // Without notification access there's nothing to show: the clock standby takes over
        media.start()
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        unregisterReceiver(timeReceiver)
        if (!MainActivity.sleep.active) media.stop()
        registry.currentState = Lifecycle.State.CREATED
    }

    override fun onDetachedFromWindow() {
        registry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }
}
