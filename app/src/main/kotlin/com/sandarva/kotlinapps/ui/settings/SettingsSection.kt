package com.sandarva.kotlinapps.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.theme.BuddyColors

private val CardShape = RoundedCornerShape(24.dp)

/** One quiet frosted card per topic — the same soft-glass material as everywhere else, at rest. */
@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(10.dp, CardShape, ambientColor = BuddyColors.Ink.copy(alpha = 0.06f), spotColor = BuddyColors.Violet.copy(alpha = 0.05f))
            .background(BuddyColors.Snow, CardShape)
            .padding(20.dp)
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = BuddyColors.Mist)
        Spacer(Modifier.height(12.dp))
        content()
    }
}
