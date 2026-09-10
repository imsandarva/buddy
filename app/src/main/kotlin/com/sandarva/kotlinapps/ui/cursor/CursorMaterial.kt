package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/**
 * The one glassy body both forms share. The launcher logo is this material at held/drag — Action
 * form, full presence, the brightness of a hold-and-drag — on white. See docs/brand.md.
 */
internal object CursorMaterial {

    /** Glow while the person is holding/dragging the point — not idle, not live/ask Voice. */
    const val HELD_GLOW = 0.45f

    /** Action-form density. Voice is 1; the logo is a hand, never a mind. */
    const val ACTION_MORPH = 0f

    fun DrawScope.drawGlow(radiusPx: Float, intensity: Float, spread: Float = GLOW_SPREAD) {
        with(CursorEffects) { drawGlow(radiusPx * spread, intensity) }
    }

    fun DrawScope.drawContactShadow(radiusPx: Float) {
        val shadowCenter = center + Offset(0f, radiusPx * 0.18f)
        drawCircle(
            brush = Brush.radialGradient(0f to BuddyColors.CursorShadow, 1f to Color.Transparent, center = shadowCenter, radius = radiusPx * 1.35f),
            radius = radiusPx * 1.35f,
            center = shadowCenter
        )
    }

    fun DrawScope.drawSharedMaterial(radiusPx: Float, t: Float) {
        val stops = arrayOf(
            0f to lerp(BuddyColors.CursorFocus, BuddyColors.CursorCore, t),
            0.5f to BuddyColors.CursorMid,
            0.82f to lerp(BuddyColors.CursorRim.copy(alpha = 0.95f), BuddyColors.CursorRim.copy(alpha = 0.7f), t),
            1f to BuddyColors.CursorRim.copy(alpha = 0f)
        )
        drawCircle(brush = Brush.radialGradient(*stops, radius = radiusPx), radius = radiusPx)
    }

    fun DrawScope.drawLightRim(radiusPx: Float) {
        drawCircle(color = BuddyColors.CursorLightRim, radius = radiusPx, style = Stroke(width = radiusPx * 0.035f))
    }

    /** Specular catch so a still frame of glass still reads as glass — the live sheen can't play on a bitmap. */
    fun DrawScope.drawLogoSheen(radiusPx: Float) {
        val catch = center + Offset(-radiusPx * 0.28f, -radiusPx * 0.32f)
        drawCircle(
            brush = Brush.radialGradient(0f to BuddyColors.CursorHighlight.copy(alpha = 0.42f), 1f to Color.Transparent, center = catch, radius = radiusPx * 0.42f),
            radius = radiusPx,
            center = center
        )
    }

    private const val GLOW_SPREAD = 2.4f

    /** Tighter bloom for the still logo so the halo stays on the white plate (matches the 46/28 adaptive-icon foreground). */
    const val LOGO_GLOW_SPREAD = 46f / 28f
}
