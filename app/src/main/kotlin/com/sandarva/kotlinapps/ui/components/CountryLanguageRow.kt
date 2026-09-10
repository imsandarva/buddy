package com.sandarva.kotlinapps.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * One row in the language picker — flag, country name, and the language it speaks. Same idea as
 * a past Flutter nationality row (flag pulse + name weight lift + check fading in on select),
 * rebuilt here with Compose animations and Buddy's own violet wash instead of a straight port.
 */
@Composable
fun CountryLanguageRow(country: Country, selected: Boolean, enabled: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    val flagScale = remember { Animatable(1f) }

    LaunchedEffect(selected) {
        if (selected) {
            flagScale.animateTo(1.26f, tween(180, easing = BuddyMotion.EnterEasing))
            flagScale.animateTo(1f, tween(260, easing = BuddyMotion.EnterEasing))
        } else {
            flagScale.snapTo(1f)
        }
    }

    val wash by animateFloatAsState(if (selected) 1f else 0f, tween(280, easing = BuddyMotion.EnterEasing), label = "rowWash")
    val checkProgress by animateFloatAsState(if (selected) 1f else 0f, tween(320, easing = BuddyMotion.EnterEasing), label = "rowCheck")
    val nameWeight by animateIntAsState(if (selected) 600 else 400, tween(320), label = "rowWeight")

    Row(
        modifier
            .fillMaxWidth()
            .background(BuddyColors.GlowSoft.copy(alpha = BuddyColors.GlowSoft.alpha * wash), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onTap()
            }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            country.flag,
            fontSize = 22.sp,
            modifier = Modifier.graphicsLayer { scaleX = flagScale.value; scaleY = flagScale.value }
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                country.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(nameWeight)),
                color = BuddyColors.Ink
            )
            Text(country.language, style = MaterialTheme.typography.bodySmall, color = BuddyColors.InkMuted)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.size(20.dp)) {
            if (checkProgress > 0f) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = BuddyColors.Violet,
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(checkProgress.coerceIn(0f, 1f))
                        .graphicsLayer {
                            val s = 0.72f + 0.28f * checkProgress
                            scaleX = s; scaleY = s
                        }
                )
            }
        }
    }
}
