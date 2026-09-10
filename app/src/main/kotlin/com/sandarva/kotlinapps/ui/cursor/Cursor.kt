package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sandarva.kotlinapps.overlay.CursorMoodSignals
import com.sandarva.kotlinapps.ui.theme.CursorMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Bounds shared with the overlay window — see docs/cursor.md §12. */
object Cursor {
    val voiceDiameter: Dp = 60.dp
    val actionDiameter: Dp = 24.dp

    /**
     * Generous, fixed hit-region (§7 touch-target note) — it never resizes as buddy morphs, so
     * the OS window never relayouts mid-animation; only the drawn content inside it scales.
     */
    val touchWidth: Dp = 116.dp
    val touchHeight: Dp = 116.dp

    /** The contact point is always this box's exact center, in both forms — no arrow, no offset tip. */
    fun tipOffsetPx(density: Float): Pair<Float, Float> {
        val half = touchWidth.value * density / 2f
        return half to half
    }
}

/**
 * The whole being. Wires every animated number the design calls for and composes the shared
 * glassy material plus whichever form's extras apply. See docs/cursor.md. The actual legibility
 * cues are pure draw functions in [VoiceForm], [ActionForm], and [CursorEffects] — this file only
 * decides *when* they play.
 */
@Composable
fun BuddyCursor(modifier: Modifier = Modifier, mood: CursorMood, contentDescription: String) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    // §4 — one entity reshaping. A single continuously-eased number drives size, density, and
    // which form's extras are audible; nothing here ever swaps between two separate assets.
    val morph by animateFloatAsState(if (mood.form == CursorForm.Voice) 1f else 0f, CursorMotion.morph(), label = "cursorMorph")

    val infinite = rememberInfiniteTransition(label = "cursorLoop")
    val breathePhase by infinite.animateFloat(0f, 1f, infiniteRepeatable(CursorMotion.breathe(), RepeatMode.Reverse), label = "breathe")
    val ringPhase by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "ringPhase")
    val sheenAngle by infinite.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "sheen")

    val amplitude by animateFloatAsState((mood as? CursorMood.Listening)?.amplitude ?: 0f, CursorMotion.quick(), label = "amplitude")
    val velocityPx by CursorMoodSignals.velocity.collectAsStateWithLifecycle()

    // Edge-triggered one-shots: each runs to completion in its own job so a fast mood change can
    // never cut a ripple or a hold-ring mid-animation (§6 — "never a cut").
    val ripple = remember { Animatable(-1f) }
    LaunchedEffect(mood is CursorMood.Acting) {
        if (mood is CursorMood.Acting) {
            // The only haptic buddy's own hand ever fires — exactly at real contact, so it stays meaningful (§10).
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            scope.launch { ripple.snapTo(0f); ripple.animateTo(1f, CursorMotion.ripple()); ripple.snapTo(-1f) }
        }
    }
    val holdRing = remember { Animatable(-1f) }
    LaunchedEffect(mood is CursorMood.Holding) {
        if (mood is CursorMood.Holding) scope.launch { holdRing.snapTo(0f); holdRing.animateTo(1f, tween(CursorMotion.HOLD_VISUAL_MS)) }
        else holdRing.snapTo(-1f)
    }
    val wobble = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        CursorMoodSignals.uncertainPulses.collect {
            scope.launch { wobble.snapTo(0f); wobble.animateTo(1f, tween(CursorMotion.UNCERTAIN_MS.toInt(), easing = LinearEasing)) }
        }
    }

    val targetPresence = presenceFor(mood)
    val presenceScale by animateFloatAsState(targetPresence.scale, CursorMotion.quick(), label = "presenceScale")
    val presenceAlpha by animateFloatAsState(targetPresence.alpha, CursorMotion.quick(), label = "presenceAlpha")
    val radiusDp = lerpDp(Cursor.actionDiameter, Cursor.voiceDiameter, morph) / 2f
    val glow by animateFloatAsState(glowIntensity(mood, amplitude), CursorMotion.quick(), label = "glow")
    val breathe = 1f + sin(breathePhase * PI.toFloat()) * BREATHE_SCALE * morph

    Canvas(
        modifier
            .size(Cursor.touchWidth, Cursor.touchHeight)
            .semantics { this.contentDescription = contentDescription }
            .graphicsLayer {
                val squeeze = sin(morph.coerceIn(0f, 1f) * PI.toFloat()) * SQUEEZE_AMOUNT
                scaleX = presenceScale * breathe * (1f - squeeze)
                scaleY = presenceScale * breathe * (1f + squeeze)
                alpha = presenceAlpha
                translationX = CursorEffects.uncertainOffset(wobble.value, 3.dp.toPx()).x
            }
    ) {
        val radiusPx = radiusDp.toPx()
        val velocity = Offset(velocityPx.first, velocityPx.second)
        val considering = mood is CursorMood.Considering
        val dragging = mood is CursorMood.Dragging
        val armed = (mood as? CursorMood.Dragging)?.armedToDismiss == true

        with(CursorMaterial) {
            drawGlow(radiusPx, glow)
            drawContactShadow(radiusPx)
            drawSharedMaterial(radiusPx, morph)
        }
        with(VoiceForm) { draw(radiusPx, amplitude, ringPhase, sheenAngle, thinking = mood is CursorMood.Thinking, alpha = morph) }
        with(ActionForm) { draw(radiusPx, velocity, ripple.value, holdRing.value, dragging, armed, considering, sheenAngle, alpha = 1f - morph) }
        with(CursorMaterial) { drawLightRim(radiusPx) }
    }
}

private data class Presence(val scale: Float, val alpha: Float)

/** Idle recedes into peripheral vision; every active state reads at full presence (§8). */
private fun presenceFor(mood: CursorMood): Presence = when (mood) {
    CursorMood.Idle -> Presence(0.82f, 0.5f)
    CursorMood.Paused -> Presence(1f, 0.62f)
    CursorMood.Targeting -> Presence(1.1f, 1f) // brightening + a slight scale-up before contact (§5)
    else -> Presence(1f, 1f)
}

/** Glow is a scarce signal, never wallpaper (§4) — idle stays quiet so a real cue has somewhere to go. */
private fun glowIntensity(mood: CursorMood, amplitude: Float): Float = when (mood) {
    CursorMood.Idle -> 0.1f
    is CursorMood.Listening -> 0.32f + amplitude * 0.4f
    CursorMood.Thinking -> 0.4f
    CursorMood.Considering -> 0.3f
    CursorMood.Traveling -> 0.3f
    CursorMood.Targeting -> 0.58f
    CursorMood.Acting -> 0.7f
    is CursorMood.Holding -> 0.5f
    is CursorMood.Dragging -> CursorMaterial.HELD_GLOW
    CursorMood.Paused -> 0.2f
}

private const val BREATHE_SCALE = 0.03f
private const val SQUEEZE_AMOUNT = 0.12f
