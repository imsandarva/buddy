package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.sandarva.kotlinapps.ui.theme.BuddyMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CursorPhase { Idle, Tap, Lifted, Drifting, Summon, Land }

data class CursorMotionValues(
    val scale: Float,
    val rotation: Float,
    val liftY: Float,
    val glowBoost: Float,
    val ring1: Float,
    val ring2: Float
)

/** Breathing idle, lift levitation, velocity tilt, summon ripples, soft landings. */
@Composable
fun rememberCursorMotion(phase: CursorPhase, dragVelocity: Offset): CursorMotionValues {
    val scale = remember { Animatable(0.78f) }
    val rotation = remember { Animatable(0f) }
    val liftY = remember { Animatable(0f) }
    val glowBoost = remember { Animatable(0f) }
    val ring1 = remember { Animatable(0f) }
    val ring2 = remember { Animatable(0f) }
    var appeared by remember { mutableStateOf(false) }
    val breathe by rememberInfiniteTransition(label = "cursorBreathe").animateFloat(
        initialValue = 1f, targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(4800, easing = BuddyMotion.EnterEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = 340f))
        appeared = true
    }
    LaunchedEffect(phase, appeared) {
        if (!appeared) return@LaunchedEffect
        when (phase) {
            CursorPhase.Idle -> {
                launch { scale.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 280f)) }
                launch { rotation.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 300f)) }
                launch { liftY.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 360f)) }
                launch { glowBoost.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
                launch { ring1.snapTo(0f); ring2.snapTo(0f) }
            }
            CursorPhase.Tap -> {
                launch { scale.snapTo(0.9f); scale.animateTo(1.06f, spring(dampingRatio = 0.48f, stiffness = 680f)); scale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 320f)) }
                launch { glowBoost.animateTo(0.55f, tween(120)); glowBoost.animateTo(0f, tween(480, easing = FastOutSlowInEasing)) }
            }
            CursorPhase.Lifted -> {
                launch { scale.animateTo(1.14f, spring(dampingRatio = 0.52f, stiffness = 520f)) }
                launch { liftY.animateTo(-5f, spring(dampingRatio = 0.62f, stiffness = 440f)) }
                launch { glowBoost.animateTo(0.75f, tween(180, easing = FastOutSlowInEasing)) }
            }
            CursorPhase.Drifting -> {
                launch { scale.animateTo(1.1f, spring(dampingRatio = 0.68f, stiffness = 400f)) }
                launch { liftY.animateTo(-3.5f, spring(dampingRatio = 0.72f, stiffness = 380f)) }
                launch { glowBoost.animateTo(0.6f, tween(120)) }
            }
            CursorPhase.Summon -> {
                launch {
                    scale.snapTo(0.72f)
                    scale.animateTo(1.18f, spring(dampingRatio = 0.42f, stiffness = 620f))
                    scale.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = 340f))
                }
                launch {
                    glowBoost.animateTo(1f, tween(160))
                    ring1.snapTo(0f); ring2.snapTo(0f)
                    ring1.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
                    delay(90)
                    ring2.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
                    delay(280)
                    ring1.snapTo(0f); ring2.snapTo(0f)
                    glowBoost.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
                }
            }
            CursorPhase.Land -> {
                launch { scale.animateTo(0.96f, spring(dampingRatio = 0.55f, stiffness = 640f)); scale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 300f)) }
                launch { liftY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                launch { rotation.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 320f)) }
                launch { glowBoost.animateTo(0.35f, tween(100)); glowBoost.animateTo(0f, tween(380, easing = FastOutSlowInEasing)) }
            }
        }
    }
    LaunchedEffect(phase, dragVelocity) {
        if (phase != CursorPhase.Drifting) return@LaunchedEffect
        val tilt = (dragVelocity.x * 0.09f).coerceIn(-14f, 14f)
        rotation.animateTo(tilt, spring(dampingRatio = 0.55f, stiffness = 520f))
    }
    val pulse = if (phase == CursorPhase.Idle) breathe else 1f
    return CursorMotionValues(scale.value * pulse, rotation.value, liftY.value, glowBoost.value, ring1.value, ring2.value)
}
