package fr.babas.noir

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private lateinit var prefs: Prefs
    private lateinit var media: MediaRepository
    private var hasAccess by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)
        media = MediaRepository(this)

        setContent {
            MaterialTheme(colorScheme = NoirColors) {
                val np by media.state.collectAsState()
                var showSettings by rememberSaveable { mutableStateOf(false) }

                val keepOn = prefs.keepOn && np.isPlaying
                val brightness = if (prefs.customBrightness) prefs.brightness else null
                LaunchedEffect(keepOn, prefs.lowRefresh, brightness) {
                    applyWindow(keepOn, prefs.lowRefresh, brightness)
                }

                when {
                    !hasAccess -> AccessScreen(onGrant = ::openListenerSettings)
                    showSettings -> {
                        BackHandler { showSettings = false }
                        SettingsScreen(
                            prefs = prefs,
                            displayInfo = displayInfo(),
                            onOpenAccess = ::openListenerSettings,
                            onBack = { showSettings = false },
                        )
                    }
                    else -> PlayerScreen(np, prefs, onOpenSettings = { showSettings = true })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        connect(attempt = 0)
    }

    /** Juste après l'octroi de l'accès, le système peut refuser quelques instants : on réessaie. */
    private fun connect(attempt: Int) {
        val granted = packageName in NotificationManagerCompat.getEnabledListenerPackages(this)
        hasAccess = granted && media.start()
        if (granted && !hasAccess && attempt < 10) {
            window.decorView.postDelayed({ if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) connect(attempt + 1) }, 300)
        }
    }

    override fun onPause() {
        super.onPause()
        media.stop()
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
            // 0 = laisser le système choisir
            preferredDisplayModeId = low?.modeId ?: 0
        }
        // Le mode seul n'est qu'un point de départ : SurfaceFlinger remonte à 60 Hz tant qu'aucune
        // vue ne vote pour moins. Android 15+ : chaque vue peut voter sa fréquence d'images.
        if (Build.VERSION.SDK_INT >= 35) {
            val rate = low?.refreshRate ?: Float.NaN // NaN = pas de préférence
            window.decorView.post { setFrameRateVote(window.decorView, rate) }
        }
    }

    @RequiresApi(35)
    private fun setFrameRateVote(view: View, rate: Float) {
        view.requestedFrameRate = rate
        if (view is ViewGroup) for (i in 0 until view.childCount) setFrameRateVote(view.getChildAt(i), rate)
    }

    @Suppress("DEPRECATION")
    private fun currentDisplay(): Display = display ?: windowManager.defaultDisplay

    /** Mode le plus lent à la résolution actuelle (changer de résolution ferait clignoter l'écran). */
    private fun lowestMode(): Display.Mode? {
        val d = currentDisplay()
        val cur = d.mode
        return d.supportedModes
            .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .minByOrNull { it.refreshRate }
    }

    private fun displayInfo(): String {
        val d = currentDisplay()
        val rates = d.supportedModes.map { Math.round(it.refreshRate) }.distinct().sorted()
        return "Actuel : ${Math.round(d.refreshRate)} Hz · disponibles : ${rates.joinToString(", ")} Hz"
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
