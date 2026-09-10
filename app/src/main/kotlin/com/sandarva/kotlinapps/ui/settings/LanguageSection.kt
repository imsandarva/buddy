package com.sandarva.kotlinapps.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandarva.kotlinapps.data.Country
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/** Boring on purpose, like [ApiKeySection]: view the current pick, change it with the full picker. */
@Composable
fun LanguageSection(current: Country, onOpenPicker: () -> Unit, modifier: Modifier = Modifier) {
    SettingsSection(title = "Language") {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(current.flag, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(current.language, style = MaterialTheme.typography.bodyLarge, color = BuddyColors.Ink)
                    Text(current.name, style = MaterialTheme.typography.bodySmall, color = BuddyColors.Mist)
                }
            }
            QuietTextAction("Change", onClick = onOpenPicker)
        }
    }
}
