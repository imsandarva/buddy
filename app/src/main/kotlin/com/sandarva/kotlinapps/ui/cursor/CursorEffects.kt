package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Small, stateless Canvas primitives shared by [VoiceForm] and [ActionForm]. Each one draws a
 * single legibility cue named in docs/cursor.md §5 — nothing here is decoration on its own.
 */
internal object CursorEffects {

    /** Soft diffusing bloom behind the shape — the scarce resource from §4; callers scale [intensity]. */
    fun DrawScope.drawGlow(radiusPx: Float, intensity: Float) {
        if (intensity <= 0.001f) return
        drawCircle(
            brush = Brush.radialGradient(
                0f to BuddyColors.CursorGlow.copy(alpha = BuddyColors.CursorGlow.alpha * intensity),
                0.55f to BuddyColors.CursorGlowFaint.copy(alpha = BuddyColors.CursorGlowFaint.alpha * intensity),
                1f to Color.Transparent,
                radius = radiusPx
            ),
            radius = radiusPx
        )
    }

    /** A directional smear opposite [velocity] — the comet trail while traveling, or a drag's ribbon. */
    fun DrawScope.drawMotionTrail(velocity: Offset, color: Color, maxLengthPx: Float, widthPx: Float) {
        val speed = hypot(velocity.x, velocity.y)
        if (speed < 0.5f) return
        val length = (speed * 2.2f).coerceIn(0f, maxLengthPx)
        val dir = Offset(-velocity.x / speed, -velocity.y / speed)
        val tail = center + dir * length
        drawLine(
            brush = Brush.linearGradient(listOf(color.copy(alpha = color.alpha), Color.Transparent), start = center, end = tail),
            start = center,
            end = tail,
            strokeWidth = widthPx,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }

    /** Ring expanding outward from the exact point of contact, fading as it grows — the tap "did it land" cue. */
    fun DrawScope.drawContactRipple(progress: Float, maxRadiusPx: Float, strokeWidthPx: Float, color: Color) {
        if (progress <= 0f || progress >= 1f) return
        drawCircle(color = color.copy(alpha = color.alpha * (1f - progress)), radius = maxRadiusPx * progress, style = Stroke(width = strokeWidthPx))
    }

    /** Clockwise fill so a hold's duration is visible mid-gesture, not just at the end. */
    fun DrawScope.drawHoldRing(progress: Float, radiusPx: Float, strokeWidthPx: Float, color: Color) {
        val diameter = radiusPx * 2f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = center - Offset(radiusPx, radiusPx),
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }

    /** Concentric rings tied to real loudness — buddy visibly "hearing" sound, not looping a canned pulse. */
    fun DrawScope.drawAmplitudeRings(amplitude: Float, phase: Float, baseRadiusPx: Float, strokeWidthPx: Float, color: Color) {
        if (amplitude <= 0.03f) return
        val rings = 2
        for (i in 0 until rings) {
            val t = ((phase + i / rings.toFloat()) % 1f)
            val radius = baseRadiusPx * (1f + t * 1.1f * amplitude)
            val alpha = (1f - t) * amplitude * 0.5f
            if (alpha <= 0.01f) continue
            drawCircle(color = color.copy(alpha = alpha), radius = radius, style = Stroke(width = strokeWidthPx))
        }
    }

    /** A single sheen turning inside the material — light catching the inside of a glass marble. Doubles as "considering." */
    fun DrawScope.drawSheenSweep(angle: Float, radiusPx: Float, color: Color) {
        val sweepCenter = center + Offset(cos(angle) * radiusPx * 0.35f, sin(angle) * radiusPx * 0.35f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to color.copy(alpha = 0.55f),
                1f to Color.Transparent,
                center = sweepCenter,
                radius = radiusPx * 0.7f
            ),
            radius = radiusPx,
            center = center
        )
    }

    /** A gentle honest wobble applied on top of whatever mood is showing — "I couldn't find that." */
    fun uncertainOffset(progress: Float, amplitudePx: Float): Offset {
        val swing = sin(progress * Math.PI.toFloat() * 3f)
        return Offset(swing * amplitudePx, 0f)
    }
}
