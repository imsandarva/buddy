package com.sandarva.kotlinapps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sandarva.kotlinapps.session.BuddySessionViewModel
import com.sandarva.kotlinapps.ui.cursor.BuddyCursorOverlay
import com.sandarva.kotlinapps.ui.home.HomeScreen
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyTheme

/** App root — wires theme, session, home, and the BuddyCursor overlay. */
@Composable
fun BuddyApp(session: BuddySessionViewModel = viewModel()) {
    val cursor by session.cursor.collectAsStateWithLifecycle()
    BuddyTheme {
        Box(Modifier.fillMaxSize().background(BuddyColors.Ink)) {
            HomeScreen(onStartBuddy = session::startBuddy)
            BuddyCursorOverlay(cursor)
        }
    }
}
