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

/** Why this permission, then the few taps to turn it on — readable at a glance. */
@Composable
internal fun PermissionHowTo(why: String, steps: List<String>, note: String? = null) {
    Text(why, style = MaterialTheme.typography.bodyLarge, color = BuddyColors.InkMuted, textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.Top) {
                Text("${i + 1}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = BuddyColors.Violet, modifier = Modifier.width(22.dp))
                Text(step, style = MaterialTheme.typography.bodyLarge, color = BuddyColors.Ink)
            }
        }
    }
    if (note != null) {
        Spacer(Modifier.height(16.dp))
        Text(note, style = MaterialTheme.typography.bodySmall, color = BuddyColors.Mist, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
