package com.sandarva.kotlinapps.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/** "You are always in control" — unambiguous, reachable, never buried (design §13). */
@Composable
fun ResetSection(isRunning: Boolean, onStopBuddy: () -> Unit, onReset: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    SettingsSection(title = "Start over") {
        Column(Modifier.fillMaxWidth()) {
            if (isRunning) {
                QuietTextAction("Stop buddy", onStopBuddy)
                Spacer(Modifier.height(16.dp))
            }
            Text(
                "This clears your key you'll start again from the beginning.",
                style = MaterialTheme.typography.bodySmall,
                color = BuddyColors.Mist
            )
            Spacer(Modifier.height(12.dp))
            QuietTextAction("Reset buddy", onClick = { confirming = true })
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Reset buddy?") },
            text = { Text("This removes your key and every setting. You'll go through setup again from scratch.") },
            confirmButton = { TextButton(onClick = { confirming = false; onReset() }) { Text("Reset", color = BuddyColors.DismissFillArmed) } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } }
        )
    }
}
