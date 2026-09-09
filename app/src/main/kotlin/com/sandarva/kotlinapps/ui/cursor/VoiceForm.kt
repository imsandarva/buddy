package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/**
 * Buddy "as a mind" — draws only the cues layered on top of the shared material in [BuddyCursor].
 * Pure and stateless: every animated number is computed once, upstream, in the composition layer.
 */
internal object VoiceForm {

    /**
     * @param amplitude 0..1 real loudness while [thinking] is false (listening); ignored while thinking.
     * @param ringPhase 0..1 looping phase driving the amplitude rings outward.
     * @param sheenAngle radians — the slow turn of light inside the marble while composing a reply.
     * @param alpha this form's share of the current morph — extras fade in as buddy becomes a mind.
     */
    fun DrawScope.draw(radiusPx: Float, amplitude: Float, ringPhase: Float, sheenAngle: Float, thinking: Boolean, alpha: Float) {
        if (alpha <= 0.01f) return
        val tint = BuddyColors.CursorHighlight.copy(alpha = BuddyColors.CursorHighlight.alpha * alpha)
        with(CursorEffects) {
            if (thinking) {
                drawSheenSweep(sheenAngle, radiusPx, tint)
            } else {
                drawAmplitudeRings(amplitude * alpha, ringPhase, radiusPx * 0.72f, radiusPx * 0.05f, BuddyColors.CursorRim.copy(alpha = alpha))
            }
        }
    }
}
