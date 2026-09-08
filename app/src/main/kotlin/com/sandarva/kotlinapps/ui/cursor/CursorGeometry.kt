package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Bounds and tip anchor for `buddycursor_icon.png` — overlay math stays tip-relative. */
object CursorGeometry {
    const val ASSET_PATH = "buddycursor_icon.png"
    private const val SOURCE_SIZE_PX = 500
    private const val TIP_X_PX = 155
    private const val TIP_Y_PX = 50

    val iconSize: Dp = 56.dp
    val touchPad: Dp = 8.dp
    val touchWidth: Dp = iconSize + touchPad * 2
    val touchHeight: Dp = iconSize + touchPad * 2
    val tipInImageX: Float = TIP_X_PX / SOURCE_SIZE_PX.toFloat()
    val tipInImageY: Float = TIP_Y_PX / SOURCE_SIZE_PX.toFloat()

    fun tipInView(density: Density): Offset = with(density) {
        Offset((touchPad + iconSize * tipInImageX).toPx(), (touchPad + iconSize * tipInImageY).toPx())
    }

    fun tipOffsetPx(density: Float): Pair<Float, Float> {
        val pad = touchPad.value * density
        val icon = iconSize.value * density
        return (pad + icon * tipInImageX) to (pad + icon * tipInImageY)
    }

    fun tipFractionX(): Float = (touchPad + iconSize * tipInImageX).value / touchWidth.value
    fun tipFractionY(): Float = (touchPad + iconSize * tipInImageY).value / touchHeight.value
}
