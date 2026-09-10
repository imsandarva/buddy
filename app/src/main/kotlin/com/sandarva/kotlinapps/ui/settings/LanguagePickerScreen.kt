package com.sandarva.kotlinapps.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.data.Country
import com.sandarva.kotlinapps.ui.components.LanguagePickerList
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/** Same searchable list as onboarding's [com.sandarva.kotlinapps.ui.onboarding.LanguageScreen], with a back arrow instead of auto-advance. */
@Composable
fun LanguagePickerScreen(selectedCode: String, onSelected: (Country) -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(BuddyColors.Paper)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = BuddyColors.Ink)
                }
                Text("Language", style = MaterialTheme.typography.titleMedium, color = BuddyColors.Ink)
            }
            LanguagePickerList(
                selectedCode = selectedCode,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                onSelected = onSelected
            )
        }
    }
}
