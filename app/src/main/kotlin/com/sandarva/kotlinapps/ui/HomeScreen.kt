package com.sandarva.kotlinapps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(isAccessibilityEnabled: Boolean, onOpenAccessibilitySettings: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isAccessibilityEnabled) "Buddy is ready to help you." else "Turn on Buddy to get started.",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = if (isAccessibilityEnabled) "Accessibility access is on." else "Open Accessibility settings, find Buddy Assistant, and switch it on.",
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            if (!isAccessibilityEnabled) Button(onClick = onOpenAccessibilitySettings) { Text("Open Accessibility Settings") }
        }
    }
}
