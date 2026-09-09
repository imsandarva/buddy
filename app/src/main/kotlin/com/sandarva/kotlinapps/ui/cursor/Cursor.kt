package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Classic desktop arrow — white fill, black outline, tip at the top-left of the glyph. */
object Cursor {
    val size: Dp = 24.dp
    val pad: Dp = 16.dp
    val touchWidth: Dp = size + pad * 2
    val touchHeight: Dp = size + pad * 2

    fun tipOffsetPx(density: Float): Pair<Float, Float> {
        val p = pad.value * density
        return p to p
    }
}

@Composable
fun BuddyCursor(modifier: Modifier = Modifier, contentDescription: String) {
    Canvas(
        modifier
            .size(Cursor.touchWidth, Cursor.touchHeight)
            .semantics { this.contentDescription = contentDescription }
    ) {
        val glyph = Cursor.size.toPx()
        val stroke = 1.6.dp.toPx()
        val inset = stroke / 2f
        val path = desktopArrowPath(glyph - stroke)
        translate(Cursor.pad.toPx() + inset, Cursor.pad.toPx() + inset) {
            drawPath(path, Color.White)
            drawPath(path, Color.Black, style = Stroke(width = stroke, join = StrokeJoin.Miter))
        }
    }
}

/** Standard desktop pointer polygon, in a 14×24 unit box scaled to [size]. */
private fun desktopArrowPath(size: Float): Path = Path().apply {
    val u = size / 24f
    moveTo(0f, 0f)
    lineTo(0f, 20f * u)
    lineTo(4.5f * u, 15.5f * u)
    lineTo(8.5f * u, 24f * u)
    lineTo(11.5f * u, 22.8f * u)
    lineTo(7.5f * u, 14f * u)
    lineTo(14f * u, 14f * u)
    close()
}
