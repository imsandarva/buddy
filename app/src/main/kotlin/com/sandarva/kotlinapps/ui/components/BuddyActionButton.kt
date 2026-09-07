package com.sandarva.kotlinapps.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.motion.pressScale
import com.sandarva.kotlinapps.ui.theme.BuddyColors

enum class BuddyActionStyle { Start, Stop }

@Composable
fun BuddyActionButton(label: String, style: BuddyActionStyle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val view = LocalView.current
    val shape = RoundedCornerShape(28.dp)
    val fill = when (style) {
        BuddyActionStyle.Start -> if (pressed) BuddyColors.VioletDeep else BuddyColors.Violet
        BuddyActionStyle.Stop -> BuddyColors.Snow
    }
    val text = if (style == BuddyActionStyle.Start) BuddyColors.OnViolet else BuddyColors.Ink
    Box(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .height(56.dp)
            .pressScale(pressed)
            .drawBehind {
                if (style == BuddyActionStyle.Start) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(BuddyColors.Glow, Color.Transparent), Offset(size.width / 2f, size.height / 2f), size.width * 0.62f),
                        radius = size.width * 0.52f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
            .background(fill, shape)
            .then(if (style == BuddyActionStyle.Stop) Modifier.border(1.dp, BuddyColors.Line, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = text) }
}
