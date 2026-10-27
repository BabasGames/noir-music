package fr.babas.noir

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * True while the activity is started. Compose keeps the UI alive in the background, so sensors,
 * receivers and loops must check this themselves to stop working when nobody can see them.
 */
val LocalVisible = compositionLocalOf { true }

/**
 * Changes on every minute change, clock or time zone change, and when coming back to the foreground.
 * delay() stops counting while the phone is in deep sleep, so it can't be trusted for wall-clock time.
 */
val LocalTimeTick = compositionLocalOf { 0 }

/** True inside the screen saver (DreamService): no settings, car button or opening other apps there. */
val LocalDream = compositionLocalOf { false }

/** False when animations are off in the settings: transitions jump straight to their end. */
val LocalAnimations = compositionLocalOf { true }

/** The given spec, or an instant jump when animations are off. */
@Composable
fun <T> motion(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (LocalAnimations.current) spec else snap()
