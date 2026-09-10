package com.sandarva.kotlinapps.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/** Plain granted / not-granted, each with a one-tap way back to the priming screen (design §13). */
@Composable
fun PermissionStatusSection(overlayGranted: Boolean, accessibilityGranted: Boolean, onFixOverlay: () -> Unit, onFixAccessibility: () -> Unit) {
    SettingsSection(title = "Permissions") {
        PermissionRow("Appear on screen", overlayGranted, onFixOverlay)
        Spacer(Modifier.height(16.dp))
        PermissionRow("Buddy Assistant", accessibilityGranted, onFixAccessibility)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = BuddyColors.Ink)
            Text(if (granted) "On" else "Off", style = MaterialTheme.typography.bodySmall, color = if (granted) BuddyColors.Violet else BuddyColors.Mist)
        }
        if (!granted) QuietTextAction("Turn on", onFix)
    }
}
