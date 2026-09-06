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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/** Quiet cursor-like mark — a living point of light, not a mascot. */
@Composable
fun BuddyMark(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "mark")
    val breathe by infinite.animateFloat(0.92f, 1.08f, infiniteRepeatable(BuddyMotion.breathe(), RepeatMode.Reverse), label = "breathe")
    Canvas(modifier.size(72.dp)) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Brush.radialGradient(listOf(BuddyColors.Glow, BuddyColors.GlowSoft, BuddyColors.Ink.copy(alpha = 0f)), c, r * breathe), r * breathe, c)
        drawCircle(color = BuddyColors.Honey.copy(alpha = 0.38f), radius = r * 0.42f, center = c, style = Stroke(width = 1.2f * density, cap = StrokeCap.Round))
        drawCircle(color = BuddyColors.Honey, radius = r * 0.16f * breathe, center = c)
        drawCircle(color = BuddyColors.Bone.copy(alpha = 0.7f), radius = r * 0.055f, center = c + Offset(-r * 0.05f, -r * 0.06f))
    }
}
