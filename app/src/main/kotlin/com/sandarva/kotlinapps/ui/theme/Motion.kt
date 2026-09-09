package com.sandarva.kotlinapps.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object BuddyMotion {
    val EnterEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    fun <T> enter(delayMillis: Int = 0) = tween<T>(durationMillis = 620, delayMillis = delayMillis, easing = EnterEasing)
    fun <T> crossfade() = tween<T>(durationMillis = 380, easing = EnterEasing)
    val Press = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
    fun <T> breathe() = tween<T>(durationMillis = 5400, easing = EaseInOutCubic)
}

/**
 * BuddyCursor's own timing table — one place for every duration/easing named in the design
 * spec (docs/cursor.md §6) so no state's motion drifts from another's by accident.
 */
object CursorMotion {
    val Ease = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EaseInOut = EaseInOutCubic
    fun <T> morph() = tween<T>(durationMillis = 360, easing = Ease) // one entity reshaping, never a cut
    fun <T> breathe() = tween<T>(durationMillis = 3600, easing = EaseInOut) // voice form idle inhale/exhale
    fun <T> settle(distancePx: Float) = tween<T>(durationMillis = travelMs(distancePx), easing = Ease)
    fun <T> quick() = tween<T>(durationMillis = 220, easing = Ease)
    fun <T> ripple() = tween<T>(durationMillis = 380, easing = EaseInOut) // tap contact ring
    fun <T> cometFade() = tween<T>(durationMillis = 150, easing = EaseInOut)
    val Squash = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessHigh) // settle, never overshoot
    const val TARGETING_SETTLE_MS = 180L // anticipation pause once magnetized, before a tap fires
    const val HOLD_VISUAL_MS = 650 // ring-fill duration approximating the real long-press dispatch
    const val UNCERTAIN_MS = 900L // brief honest "I'm confused" wobble
    const val PAUSED_HOLD_MS = 700L // how long the stop acknowledgement stays visible before idling
    private fun travelMs(distancePx: Float): Int = (200 + (distancePx / 1400f).coerceIn(0f, 1f) * 300).toInt()
}
