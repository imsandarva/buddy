package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Classic arrow pointer — tip anchor, vertical left edge, diagonal right edge, chevron notch.
 * Coordinates are tip-relative so the full silhouette always fits inside the view.
 */
object CursorGeometry {
    val shapeWidth: Dp = 21.dp
    val shapeHeight: Dp = 22.5.dp
    val notchX: Dp = 5.5.dp
    val notchY: Dp = 13.dp
    val rightDrop: Dp = 0.5.dp
    val strokePad: Dp = 4.dp
    val extentLeft: Dp = strokePad
    val extentTop: Dp = strokePad
    val extentRight: Dp = shapeWidth + strokePad
    val extentBottom: Dp = shapeHeight + strokePad + rightDrop
    val width: Dp = extentLeft + extentRight
    val height: Dp = extentTop + extentBottom
    val touchPad: Dp = 8.dp
    val touchWidth: Dp = width + touchPad * 2
    val touchHeight: Dp = height + touchPad * 2
    val outlineWidth: Dp = 2.75.dp

    fun tipInView(density: Density): Offset = with(density) {
        Offset((extentLeft + touchPad).toPx(), (extentTop + touchPad).toPx())
    }

    fun tipOffsetPx(density: Float): Pair<Float, Float> {
        val pad = touchPad.value * density
        return (extentLeft.value * density + pad) to (extentTop.value * density + pad)
    }

    fun tipFractionX(): Float = (extentLeft + touchPad).value / touchWidth.value
    fun tipFractionY(): Float = (extentTop + touchPad).value / touchHeight.value

    /** Tip → left edge → notch → bottom-right → close along the diagonal. */
    fun pointerPath(tip: Offset, density: Density): Path = with(density) {
        val h = shapeHeight.toPx()
        val w = shapeWidth.toPx()
        val nx = notchX.toPx()
        val ny = notchY.toPx()
        val drop = rightDrop.toPx()
        Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(tip.x, tip.y + h)
            lineTo(tip.x + nx, tip.y + ny)
            lineTo(tip.x + w, tip.y + h + drop)
            close()
        }
    }
}
