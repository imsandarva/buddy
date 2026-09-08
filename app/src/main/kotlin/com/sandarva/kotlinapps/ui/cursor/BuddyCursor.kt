package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/** Asset-backed pointer — summon ripples at the tip; scale/tilt/lift live on the handle. */
@Composable
fun BuddyCursor(
    modifier: Modifier = Modifier,
    motion: CursorMotionValues,
    contentDescription: String
) {
    val density = LocalDensity.current
    val bitmap = rememberBuddyCursorBitmap()
    val tip = CursorGeometry.tipInView(density)
    val iconPx = with(density) { CursorGeometry.iconSize.toPx() }
    Box(
        modifier
            .size(CursorGeometry.touchWidth, CursorGeometry.touchHeight)
            .semantics { this.contentDescription = contentDescription }
    ) {
        if (motion.ring1 > 0f || motion.ring2 > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                listOf(motion.ring1 to 0.14f, motion.ring2 to 0.1f).forEach { (ring, alphaScale) ->
                    if (ring <= 0f) return@forEach
                    drawCircle(
                        color = BuddyColors.CursorBlue.copy(alpha = alphaScale * (1f - ring * 0.65f)),
                        radius = iconPx * (0.42f + ring * 0.55f),
                        center = tip
                    )
                }
            }
        }
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .offset(CursorGeometry.touchPad, CursorGeometry.touchPad)
                .size(CursorGeometry.iconSize)
        )
    }
}
