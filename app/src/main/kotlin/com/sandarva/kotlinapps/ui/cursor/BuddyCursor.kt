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

private val CursorWidth = 40.dp
private val CursorHeight = 52.dp
private val HotspotX = 13.dp
private val HotspotY = 11.dp

/** Mouse-style pointer. Layout origin is the hotspot (the tip), so placement APIs aim at what it points to. */
@Composable
fun BuddyCursor(modifier: Modifier = Modifier, held: Boolean = false, contentDescription: String) {
    Canvas(
        modifier
            .size(CursorWidth, CursorHeight)
            .offset(-HotspotX, -HotspotY)
            .semantics { this.contentDescription = contentDescription }
    ) {
        val tip = Offset(HotspotX.toPx(), HotspotY.toPx())
        val path = arrowPath(tip, size.maxDimension)
        val glowR = if (held) 26.dp.toPx() else 18.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(listOf(BuddyColors.Glow, BuddyColors.Ink.copy(alpha = 0f)), tip, glowR),
            radius = glowR,
            center = tip
        )
        drawPath(path, BuddyColors.Honey)
        drawPath(path, BuddyColors.OnHoney, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(BuddyColors.Bone.copy(alpha = 0.55f), radius = 1.6.dp.toPx(), center = tip + Offset(3.2.dp.toPx(), 4.2.dp.toPx()))
    }
}

private fun arrowPath(tip: Offset, scale: Float): Path {
    val u = scale / 52f
    return Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(tip.x, tip.y + 29f * u)
        lineTo(tip.x + 7.4f * u, tip.y + 22.2f * u)
        lineTo(tip.x + 14.8f * u, tip.y + 36.4f * u)
        lineTo(tip.x + 18.8f * u, tip.y + 34.4f * u)
        lineTo(tip.x + 11.6f * u, tip.y + 20.2f * u)
        lineTo(tip.x + 21.2f * u, tip.y + 19.6f * u)
        close()
    }
}
