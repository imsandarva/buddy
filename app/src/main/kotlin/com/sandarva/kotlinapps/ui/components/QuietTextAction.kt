package com.sandarva.kotlinapps.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import com.sandarva.kotlinapps.ui.motion.pressScale
import com.sandarva.kotlinapps.ui.theme.BuddyColors

@Composable
fun QuietTextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val view = LocalView.current
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (pressed) BuddyColors.VioletDeep else BuddyColors.Violet,
        textAlign = TextAlign.Center,
        modifier = modifier
            .pressScale(pressed, down = 0.98f)
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
    )
}
