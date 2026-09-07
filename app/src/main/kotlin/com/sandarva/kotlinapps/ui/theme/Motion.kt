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
