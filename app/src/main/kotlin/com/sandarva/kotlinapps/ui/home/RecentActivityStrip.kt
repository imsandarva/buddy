package com.sandarva.kotlinapps.ui.home

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.data.ActivityEntry
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/**
 * A short, glanceable trail — not a dashboard, just enough that the last few things buddy did can
 * be repeated with a tap instead of re-explained by voice (design §12). Collapsed by default so
 * the home screen stays quiet; an intentional line replaces the usual blank "no data" hole (§16).
 */
@Composable
fun RecentActivityStrip(entries: List<ActivityEntry>, onRepeat: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val view = LocalView.current
    Column(modifier.fillMaxWidth().animateContentSize(BuddyMotion.crossfade()), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                expanded = !expanded
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Recent", style = MaterialTheme.typography.bodySmall, color = BuddyColors.Mist)
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = BuddyColors.Mist,
                modifier = Modifier.height(16.dp)
            )
        }
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            if (entries.isEmpty()) {
                Text(
                    "Nothing yet — ask me something and it'll show up here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BuddyColors.Mist,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(Modifier.height((entries.size * 40).coerceAtMost(160).dp)) {
                    items(entries) { entry ->
                        Text(
                            entry.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = BuddyColors.Violet,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(remember { MutableInteractionSource() }, indication = null) { onRepeat(entry.text) }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
