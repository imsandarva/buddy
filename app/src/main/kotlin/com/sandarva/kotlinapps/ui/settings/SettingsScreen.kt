package com.sandarva.kotlinapps.ui.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.data.CountryData
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/**
 * Deliberately boring, in contrast to onboarding's emotional beats — this is where things are
 * managed, not felt (design §13). A thin composition layer: each section owns its own logic.
 */
@Composable
fun SettingsScreen(
    hasKey: Boolean,
    keyInvalid: Boolean,
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    isRunning: Boolean,
    languageCountryCode: String,
    onBack: () -> Unit,
    onSaveKey: (String) -> Unit,
    onClearKeyInvalid: () -> Unit,
    onSaveLanguage: (String) -> Unit,
    onFixOverlay: () -> Unit,
    onFixAccessibility: () -> Unit,
    onStopBuddy: () -> Unit,
    onReset: () -> Unit
) {
    var pickingLanguage by remember { mutableStateOf(false) }
    val currentLanguage = remember(languageCountryCode) { CountryData.find(languageCountryCode) ?: CountryData.DEFAULT }

    Box(Modifier.fillMaxSize().background(BuddyColors.Paper)) {
        Crossfade(pickingLanguage, animationSpec = BuddyMotion.crossfade(), label = "settingsStage") { picking ->
            if (picking) {
                LanguagePickerScreen(
                    selectedCode = currentLanguage.code,
                    onSelected = { country -> onSaveLanguage(country.code); pickingLanguage = false },
                    onBack = { pickingLanguage = false }
                )
            } else {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    SettingsTopBar(onBack)
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
                        ApiKeySection(hasKey, keyInvalid, onSaved = { key -> onSaveKey(key); onClearKeyInvalid() }, onCleared = onClearKeyInvalid)
                        LanguageSection(currentLanguage, onOpenPicker = { pickingLanguage = true })
                        PermissionStatusSection(overlayGranted, accessibilityGranted, onFixOverlay, onFixAccessibility)
                        ResetSection(isRunning, onStopBuddy, onReset)
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = BuddyColors.Ink)
        }
        Text("Settings", style = MaterialTheme.typography.titleMedium, color = BuddyColors.Ink)
    }
}
