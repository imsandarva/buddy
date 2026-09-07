package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.theme.BuddyColors

private val CursorWidth = 48.dp
private val CursorHeight = 56.dp
private val HotspotX = 10.dp
private val HotspotY = 9.dp

/** Soft rounded pointer — layout origin is the tip so placement APIs aim at what it points to. */
@Composable
fun BuddyCursor(
    modifier: Modifier = Modifier,
    motion: CursorMotionValues,
    contentDescription: String
) {
    Canvas(
        modifier
            .size(CursorWidth, CursorHeight)
            .offset(-HotspotX, -HotspotY)
            .semantics { this.contentDescription = contentDescription }
    ) {
        val tip = Offset(HotspotX.toPx(), HotspotY.toPx())
        val path = softPointerPath(tip, size.maxDimension)
        val glowR = (22.dp.toPx() + motion.glowBoost * 14.dp.toPx()) * motion.scale.coerceAtMost(1.08f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(BuddyColors.CursorGlowBright.copy(alpha = 0.55f + motion.glowBoost * 0.25f), BuddyColors.CursorGlow.copy(alpha = 0f)),
                tip, glowR
            ),
            radius = glowR, center = tip
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(BuddyColors.CursorGlow.copy(alpha = 0.35f + motion.glowBoost * 0.2f), BuddyColors.Paper.copy(alpha = 0f)),
                tip, glowR * 1.6f
            ),
            radius = glowR * 1.6f, center = tip
        )
        if (motion.ring1 > 0f) {
            val r1 = 8.dp.toPx() + motion.ring1 * 28.dp.toPx()
            drawCircle(BuddyColors.CursorBlueLight.copy(alpha = (1f - motion.ring1) * 0.5f), r1, tip, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        }
        if (motion.ring2 > 0f) {
            val r2 = 12.dp.toPx() + motion.ring2 * 34.dp.toPx()
            drawCircle(BuddyColors.CursorBlue.copy(alpha = (1f - motion.ring2) * 0.38f), r2, tip, style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
        }
        drawPath(
            path,
            brush = Brush.linearGradient(
                listOf(BuddyColors.CursorBlueLight, BuddyColors.CursorBlue, BuddyColors.CursorBlueDeep),
                start = tip + Offset(0f, 6.dp.toPx()),
                end = tip + Offset(18.dp.toPx(), 38.dp.toPx())
            )
        )
        drawPath(path, BuddyColors.CursorRim, style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(
            BuddyColors.Snow.copy(alpha = 0.82f),
            radius = 2.1.dp.toPx(),
            center = tip + Offset(4.dp.toPx(), 5.dp.toPx())
        )
    }
}

private fun softPointerPath(tip: Offset, scale: Float): Path {
    val u = scale / 56f
    return Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(tip.x - 0.5f * u, tip.y + 8f * u, tip.x - 1f * u, tip.y + 26f * u, tip.x, tip.y + 34f * u)
        cubicTo(tip.x + 1.5f * u, tip.y + 39f * u, tip.x + 9f * u, tip.y + 38f * u, tip.x + 13f * u, tip.y + 31f * u)
        cubicTo(tip.x + 16f * u, tip.y + 26f * u, tip.x + 18.5f * u, tip.y + 33f * u, tip.x + 23f * u, tip.y + 35f * u)
        cubicTo(tip.x + 27f * u, tip.y + 36.5f * u, tip.x + 25.5f * u, tip.y + 28f * u, tip.x + 20f * u, tip.y + 23f * u)
        cubicTo(tip.x + 28f * u, tip.y + 21f * u, tip.x + 34f * u, tip.y + 14f * u, tip.x + 24f * u, tip.y + 8f * u)
        cubicTo(tip.x + 16f * u, tip.y + 3f * u, tip.x + 6f * u, tip.y + 1.5f * u, tip.x, tip.y)
        close()
    }
}
