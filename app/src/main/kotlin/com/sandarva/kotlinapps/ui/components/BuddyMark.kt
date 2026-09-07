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

/** Quiet living point — a companion, not a mascot. */
@Composable
fun BuddyMark(alive: Boolean = false, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "mark")
    val lo = if (alive) 0.96f else 0.92f
    val hi = if (alive) 1.12f else 1.06f
    val breathe by infinite.animateFloat(lo, hi, infiniteRepeatable(BuddyMotion.breathe(), RepeatMode.Reverse), label = "breathe")
    Canvas(modifier.size(76.dp)) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            Brush.radialGradient(listOf(BuddyColors.Glow, BuddyColors.GlowSoft, BuddyColors.Paper.copy(alpha = 0f)), c, r * breathe),
            r * breathe,
            c
        )
        drawCircle(color = BuddyColors.Violet.copy(alpha = 0.28f), radius = r * 0.4f, center = c, style = Stroke(width = 1.1f * density, cap = StrokeCap.Round))
        drawCircle(color = BuddyColors.Violet, radius = r * (if (alive) 0.18f else 0.15f) * breathe, center = c)
        drawCircle(color = BuddyColors.Snow.copy(alpha = 0.85f), radius = r * 0.05f, center = c + Offset(-r * 0.045f, -r * 0.055f))
    }
}
