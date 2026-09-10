package com.sandarva.kotlinapps.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/**
 * Custom, single-weight line glyphs for the two permission-priming screens — never a stock icon
 * pack for something this central. Both read as a plain phone outline with buddy's own point
 * doing the thing being explained, so the picture teaches the idea before a single word does.
 */
@Composable
fun OverlayGlyph(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "overlayGlyph")
    val float by infinite.animateFloat(0f, 1f, infiniteRepeatable(BuddyMotion.breathe(), RepeatMode.Reverse), label = "float")
    Canvas(modifier.size(148.dp)) {
        val phoneW = size.width * 0.56f
        val phoneH = size.height * 0.72f
        val topLeft = Offset((size.width - phoneW) / 2f, size.height * 0.30f)
        drawRoundRect(
            color = BuddyColors.Line,
            topLeft = topLeft,
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(phoneW * 0.16f),
            style = Stroke(width = 2.2f * density, cap = StrokeCap.Round)
        )
        val dotCenter = Offset(size.width / 2f, topLeft.y - 10.dp.toPx() - float * 6.dp.toPx())
        drawCircle(BuddyColors.Glow, radius = 20.dp.toPx(), center = dotCenter)
        drawCircle(BuddyColors.Violet, radius = 7.dp.toPx(), center = dotCenter)
        drawLine(
            color = BuddyColors.Lilac.copy(alpha = 0.5f),
            start = Offset(dotCenter.x, dotCenter.y + 9.dp.toPx()),
            end = Offset(dotCenter.x, topLeft.y - 2.dp.toPx()),
            strokeWidth = 1.4f * density,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx()))
        )
    }
}

@Composable
fun AccessibilityGlyph(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "accessGlyph")
    val ripple by infinite.animateFloat(0f, 1f, infiniteRepeatable(BuddyMotion.crossfade<Float>(), RepeatMode.Restart), label = "ripple")
    Canvas(modifier.size(148.dp)) {
        val phoneW = size.width * 0.56f
        val phoneH = size.height * 0.72f
        val topLeft = Offset((size.width - phoneW) / 2f, size.height * 0.14f)
        drawRoundRect(
            color = BuddyColors.Line,
            topLeft = topLeft,
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(phoneW * 0.16f),
            style = Stroke(width = 2.2f * density, cap = StrokeCap.Round)
        )
        val tapPoint = Offset(topLeft.x + phoneW * 0.62f, topLeft.y + phoneH * 0.64f)
        drawCircle(BuddyColors.Violet.copy(alpha = (1f - ripple) * 0.5f), radius = (10f + ripple * 16f) * density, center = tapPoint)
        drawCircle(BuddyColors.Violet, radius = 6.dp.toPx(), center = tapPoint)
        // two short lines standing for legible rows the buddy can read
        drawLine(BuddyColors.Line, Offset(topLeft.x + phoneW * 0.18f, topLeft.y + phoneH * 0.30f), Offset(topLeft.x + phoneW * 0.7f, topLeft.y + phoneH * 0.30f), 2.dp.toPx(), StrokeCap.Round)
        drawLine(BuddyColors.Line, Offset(topLeft.x + phoneW * 0.18f, topLeft.y + phoneH * 0.44f), Offset(topLeft.x + phoneW * 0.5f, topLeft.y + phoneH * 0.44f), 2.dp.toPx(), StrokeCap.Round)
    }
}
