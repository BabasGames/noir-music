package fr.babas.noir

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
