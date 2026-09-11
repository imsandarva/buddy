package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/**
 * Why this permission, then the few taps to turn it on. Owns its own column so it can sit
 * inside [com.sandarva.kotlinapps.ui.motion.FadeSlideIn] (a Box) without the why, steps, and
 * note painting on top of each other.
 */
@Composable
internal fun PermissionHowTo(why: String, steps: List<String>, note: String? = null) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            why,
            style = MaterialTheme.typography.bodyLarge,
            color = BuddyColors.InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            steps.forEachIndexed { i, step -> HowToStep(index = i + 1, text = step) }
        }
        if (note != null) {
            Spacer(Modifier.height(22.dp))
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = BuddyColors.Mist,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HowToStep(index: Int, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            "$index",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = BuddyColors.Violet,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = BuddyColors.Ink,
            modifier = Modifier.weight(1f)
        )
    }
}
