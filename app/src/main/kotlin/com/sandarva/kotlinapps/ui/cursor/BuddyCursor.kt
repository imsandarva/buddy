package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/** Classic arrow — solid blue fill, thick dark outline, rounded stroke joins. */
@Composable
fun BuddyCursor(
    modifier: Modifier = Modifier,
    motion: CursorMotionValues,
    contentDescription: String
) {
    val density = LocalDensity.current
    val tip = CursorGeometry.tipInView(density)
    val outline = with(density) { CursorGeometry.outlineWidth.toPx() }
    Canvas(
        modifier
            .size(CursorGeometry.touchWidth, CursorGeometry.touchHeight)
            .semantics { this.contentDescription = contentDescription }
    ) {
        val path = CursorGeometry.pointerPath(tip, density)
        val ringScale = 1f + motion.ring1 * 0.08f + motion.ring2 * 0.05f
        if (motion.ring1 > 0f || motion.ring2 > 0f) {
            scale(ringScale, ringScale, tip) {
                drawPath(path, BuddyColors.CursorBlue.copy(alpha = 0.12f), style = Stroke(outline * 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        drawPath(path, BuddyColors.CursorBlue)
        drawPath(path, BuddyColors.CursorOutline, style = Stroke(outline, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
