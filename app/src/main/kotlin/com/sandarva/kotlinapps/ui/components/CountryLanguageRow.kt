package com.sandarva.kotlinapps.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandarva.kotlinapps.data.Country
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/**
 * One country in the list — flag, name, the language Buddy will speak. Selected is a quieter
 * weight and a check, never a washed box; the flag lifts once so the tap feels received.
 */
@Composable
fun CountryLanguageRow(country: Country, selected: Boolean, enabled: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    val flagScale = remember { Animatable(1f) }

    LaunchedEffect(selected) {
        if (selected) {
            flagScale.animateTo(1.18f, tween(160, easing = BuddyMotion.EnterEasing))
            flagScale.animateTo(1f, tween(240, easing = BuddyMotion.EnterEasing))
        } else {
            flagScale.snapTo(1f)
        }
    }

    val check by animateFloatAsState(if (selected) 1f else 0f, tween(280, easing = BuddyMotion.EnterEasing), label = "rowCheck")
    val nameWeight by animateIntAsState(if (selected) 500 else 400, tween(280), label = "rowWeight")

    Row(
        modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onTap()
            }
            .padding(horizontal = 2.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(country.flag, fontSize = 20.sp, modifier = Modifier.graphicsLayer { scaleX = flagScale.value; scaleY = flagScale.value })
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(country.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(nameWeight)), color = BuddyColors.Ink)
            Text(country.language, style = MaterialTheme.typography.bodySmall, color = BuddyColors.InkMuted)
        }
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = BuddyColors.Ink,
            modifier = Modifier
                .size(18.dp)
                .alpha(check)
                .graphicsLayer {
                    val s = 0.86f + 0.14f * check
                    scaleX = s; scaleY = s
                }
        )
    }
}
