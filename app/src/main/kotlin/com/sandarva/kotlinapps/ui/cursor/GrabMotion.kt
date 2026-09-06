package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

class GrabMotion(val scale: Float, val rotation: Float)

/** Pickup bounce + a short damped jiggle, then a lifted rest scale while held. */
@Composable
fun rememberGrabMotion(held: Boolean): GrabMotion {
    val scale = remember { Animatable(0.84f) }
    val rotation = remember { Animatable(0f) }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 380f))
        appeared = true
    }
    LaunchedEffect(held, appeared) {
        if (!appeared) return@LaunchedEffect
        if (held) {
            launch { scale.animateTo(1.16f, spring(dampingRatio = 0.42f, stiffness = 720f)); scale.animateTo(1.08f, spring(dampingRatio = 0.72f, stiffness = 420f)) }
            launch { floatArrayOf(10f, -8f, 5.5f, -3f, 0f).forEach { rotation.animateTo(it, spring(dampingRatio = 0.36f, stiffness = 920f)) } }
        } else {
            launch { scale.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 320f)) }
            launch { rotation.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 320f)) }
        }
    }
    return GrabMotion(scale.value, rotation.value)
}
