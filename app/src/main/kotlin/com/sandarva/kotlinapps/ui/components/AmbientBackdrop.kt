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

/** Slow-breathing light behind the hero — drawn once per frame, no child recomposition. */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "ambient")
    val pulse by infinite.animateFloat(0.78f, 1f, infiniteRepeatable(BuddyMotion.breathe(), RepeatMode.Reverse), label = "pulse")
    val drift by infinite.animateFloat(0f, 1f, infiniteRepeatable(BuddyMotion.breathe(), RepeatMode.Reverse), label = "drift")
    Canvas(modifier) {
        val honey = Offset(size.width * (0.48f + drift * 0.04f), size.height * 0.32f)
        val sage = Offset(size.width * 0.78f, size.height * (0.68f - drift * 0.03f))
        val hearth = Offset(size.width * 0.5f, size.height * 0.9f)
        drawCircle(
            brush = Brush.radialGradient(listOf(BuddyColors.Glow, BuddyColors.GlowSoft, BuddyColors.Ink.copy(alpha = 0f)), honey, size.minDimension * 0.64f * pulse)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(BuddyColors.GlowSage, BuddyColors.Ink.copy(alpha = 0f)), sage, size.minDimension * 0.5f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(BuddyColors.GlowSoft, BuddyColors.Ink.copy(alpha = 0f)), hearth, size.minDimension * 0.42f)
        )
    }
}
