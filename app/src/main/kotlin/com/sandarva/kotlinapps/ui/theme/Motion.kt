package com.sandarva.kotlinapps.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object BuddyMotion {
    val EnterEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    fun <T> enter(delayMillis: Int = 0) = tween<T>(durationMillis = 780, delayMillis = delayMillis, easing = EnterEasing)
    val Press = spring<Float>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium)
    fun <T> breathe() = tween<T>(durationMillis = 4800, easing = EaseInOutCubic)
}
