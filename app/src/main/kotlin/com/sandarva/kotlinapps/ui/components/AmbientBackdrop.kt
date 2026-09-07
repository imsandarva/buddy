package com.sandarva.kotlinapps.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/** Slow iris bloom on paper — drawn once per frame, no child recomposition. */
@Composable
fun AmbientBackdrop(alive: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "ambient")
    val pulse by infinite.animateFloat(0.82f, 1f, infiniteRepeatable(BuddyMotion.breathe(), RepeatMode.Reverse), label = "pulse")
    val drift by infinite.animateFloat(0f, 1f, infiniteRepeatable(BuddyMotion.breathe(), RepeatMode.Reverse), label = "drift")
    val bloom = if (alive) 0.78f else 0.58f
    Canvas(modifier) {
        val iris = Offset(size.width * (0.5f + drift * 0.03f), size.height * 0.28f)
        val lilac = Offset(size.width * 0.82f, size.height * (0.72f - drift * 0.025f))
        val wash = Offset(size.width * 0.18f, size.height * 0.88f)
        drawRect(BuddyColors.Paper)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(BuddyColors.Glow.copy(alpha = bloom), BuddyColors.GlowSoft, BuddyColors.Paper.copy(alpha = 0f)),
                iris,
                size.minDimension * 0.72f * pulse
            )
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(BuddyColors.GlowLilac, BuddyColors.Paper.copy(alpha = 0f)), lilac, size.minDimension * 0.48f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(BuddyColors.GlowSoft, BuddyColors.Paper.copy(alpha = 0f)), wash, size.minDimension * 0.4f)
        )
    }
}
