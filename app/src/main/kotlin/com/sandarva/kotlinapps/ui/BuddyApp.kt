package com.sandarva.kotlinapps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sandarva.kotlinapps.session.BuddySessionViewModel
import com.sandarva.kotlinapps.ui.cursor.BuddyCursorLayer
import com.sandarva.kotlinapps.ui.home.HomeScreen
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyTheme

/** App root — wires theme, session, home, and the BuddyCursor overlay. */
@Composable
fun BuddyApp(session: BuddySessionViewModel = viewModel()) {
    BuddyTheme {
        Box(Modifier.fillMaxSize().background(BuddyColors.Ink)) {
            HomeScreen(onStartBuddy = session::startBuddy)
            BuddyCursorLayer(session)
        }
    }
}
