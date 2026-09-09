package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/**
 * Buddy "as a hand" — draws only the cues layered on top of the shared material in [BuddyCursor].
 * Pure and stateless: every animated number is computed once, upstream, in the composition layer.
 */
internal object ActionForm {

    /**
     * @param velocity current window motion in px/frame — drives the comet trail and drag ribbon.
     * @param rippleProgress 0..1, active only right after a real tap contact; -1 when idle.
     * @param holdProgress 0..1 clockwise ring fill while a long-press is in flight; -1 when idle.
     * @param dragging buddy's own stroke or the person repositioning the point.
     * @param armedToDismiss releasing now would stop Buddy — the ribbon should read as a warning.
     * @param sheenAngle reused from [CursorEffects.drawSheenSweep] while quietly considering a move.
     * @param alpha this form's share of the current morph — extras fade in as buddy becomes a hand.
     */
    fun DrawScope.draw(
        radiusPx: Float,
        velocity: Offset,
        rippleProgress: Float,
        holdProgress: Float,
        dragging: Boolean,
        armedToDismiss: Boolean,
        considering: Boolean,
        sheenAngle: Float,
        alpha: Float
    ) {
        if (alpha <= 0.01f) return
        with(CursorEffects) {
            val ribbonColor = if (armedToDismiss) BuddyColors.CursorGlow.copy(alpha = 0.9f * alpha) else BuddyColors.CursorFocus.copy(alpha = 0.55f * alpha)
            if (dragging) drawMotionTrail(velocity, ribbonColor, radiusPx * 5f, radiusPx * 0.6f)
            else drawMotionTrail(velocity, BuddyColors.CursorRim.copy(alpha = 0.6f * alpha), radiusPx * 3.2f, radiusPx * 0.42f)
            if (rippleProgress in 0f..1f) drawContactRipple(rippleProgress, radiusPx * 2.6f, radiusPx * 0.1f, BuddyColors.CursorFocus.copy(alpha = alpha))
            if (holdProgress in 0f..1f) drawHoldRing(holdProgress, radiusPx * 1.7f, radiusPx * 0.16f, BuddyColors.CursorFocus.copy(alpha = alpha))
            if (considering) drawSheenSweep(sheenAngle, radiusPx, BuddyColors.CursorHighlight.copy(alpha = 0.5f * alpha))
        }
    }
}
