package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import kotlin.math.hypot
import kotlin.math.min

/** Bounds and draw constants — keep in sync with `render-cursor.kt`. */
object Cursor {
    val size: Dp = 28.dp
    val pad: Dp = 16.dp
    val stroke: Dp = 1.6.dp
    val touchWidth: Dp = size + pad * 2
    val touchHeight: Dp = size + pad * 2

    fun tipOffsetPx(density: Float): Pair<Float, Float> {
        val p = pad.value * density
        return p to p
    }
}

private const val CORNER_RADIUS = 2f // 24-unit space — matches render-cursor.kt
private const val NOTCH_X = 5.5f
private const val NOTCH_Y = 13.5f
private const val SHAFT_BOTTOM = 20f

@Composable
fun BuddyCursor(modifier: Modifier = Modifier, contentDescription: String) {
    Canvas(
        modifier
            .size(Cursor.touchWidth, Cursor.touchHeight)
            .semantics { this.contentDescription = contentDescription }
    ) {
        val glyph = Cursor.size.toPx()
        val stroke = Cursor.stroke.toPx()
        val inset = stroke / 2f
        val path = buddyArrowPath(glyph - stroke)
        translate(Cursor.pad.toPx() + inset, Cursor.pad.toPx() + inset) {
            drawPath(path, BuddyColors.CursorBlue)
            drawPath(path, Color.Black, style = Stroke(width = stroke, join = StrokeJoin.Round))
        }
    }
}

/** Arrow pointer — left shaft, sloped inner leg, horizontal shelf, outer diagonal. */
private fun buddyArrowPath(size: Float): Path = Path().apply {
    val u = size / 24f
    val pts = listOf(
        Offset(0f, 0f),
        Offset(0f, SHAFT_BOTTOM * u),
        Offset(NOTCH_X * u, NOTCH_Y * u),
        Offset(NOTCH_Y * u, NOTCH_Y * u)
    )
    roundedPolygon(pts, CORNER_RADIUS * u, sharpCorners = setOf(0))
}

private fun Path.roundedPolygon(pts: List<Offset>, radius: Float, sharpCorners: Set<Int>) {
    val n = pts.size
    for (i in 0 until n) {
        val prev = pts[(i - 1 + n) % n]
        val curr = pts[i]
        val next = pts[(i + 1) % n]
        val inVec = curr - prev
        val outVec = next - curr
        val inLen = hypot(inVec.x, inVec.y)
        val outLen = hypot(outVec.x, outVec.y)
        val trim = if (i in sharpCorners) 0f else min(radius, min(inLen * 0.42f, outLen * 0.42f))
        val entry = curr - inVec / inLen * trim
        val exit = curr + outVec / outLen * trim
        if (i == 0) moveTo(entry.x, entry.y) else lineTo(entry.x, entry.y)
        if (i in sharpCorners) lineTo(curr.x, curr.y) else quadraticTo(curr.x, curr.y, exit.x, exit.y)
    }
    close()
}
