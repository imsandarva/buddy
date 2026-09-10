package com.sandarva.kotlinapps.ui.home

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.motion.pressScale
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/** A blank ask box is a dead end — a few tappable examples solve that without a manual (design §12). */
val SUGGESTED_PROMPTS = listOf("What's my battery at?", "Open Spotify", "Turn up the volume")

@Composable
fun PromptChips(onTap: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
        items(SUGGESTED_PROMPTS) { prompt -> PromptChip(prompt) { onTap(prompt) } }
    }
}

@Composable
private fun PromptChip(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val view = LocalView.current
    Text(
        label,
        style = MaterialTheme.typography.bodySmall,
        color = BuddyColors.Violet,
        modifier = Modifier
            .pressScale(pressed, down = 0.96f)
            .background(if (pressed) BuddyColors.GlowSoft else BuddyColors.Snow, RoundedCornerShape(50))
            .border(1.dp, BuddyColors.Line, RoundedCornerShape(50))
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}
