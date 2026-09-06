package com.sandarva.kotlinapps.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sandarva.kotlinapps.accessibility.AccessibilityController
import com.sandarva.kotlinapps.brain.BrainPhase
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyOverlayController
import com.sandarva.kotlinapps.session.BuddySessionViewModel
import com.sandarva.kotlinapps.ui.home.HomeScreen
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyTheme

/** App root — wires theme, session, and home. The cursor itself lives in the overlay service. */
@Composable
fun BuddyApp(session: BuddySessionViewModel = viewModel()) {
    val isRunning by session.isRunning.collectAsStateWithLifecycle()
    val awaitingPermission by session.awaitingPermission.collectAsStateWithLifecycle()
    val canSeeScreen by session.canSeeScreen.collectAsStateWithLifecycle()
    val awaitingAccess by session.awaitingAccess.collectAsStateWithLifecycle()
    val brainPhase by session.brainPhase.collectAsStateWithLifecycle()
    val askOpen by session.askOpen.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        BuddyLog.d("BuddyApp.mic", "granted=$granted")
        if (granted) session.listenToAsk()
    }
    HostResumeHook()
    BuddyTheme {
        Box(Modifier.fillMaxSize().background(BuddyColors.Ink)) {
            HomeScreen(
                isRunning = isRunning,
                awaitingPermission = awaitingPermission,
                canSeeScreen = canSeeScreen,
                awaitingAccess = awaitingAccess,
                listening = brainPhase == BrainPhase.Listening,
                thinking = brainPhase == BrainPhase.Thinking,
                onStartBuddy = session::startBuddy,
                onStopBuddy = session::stopBuddy,
                onWatchMove = session::watchBuddyMove,
                onRequestAccess = session::requestScreenAccess,
                onPointAtControl = session::pointAtControl,
                onAskBuddy = {
                    BuddyLog.d("BuddyApp.askTap", "canSee=$canSeeScreen askOpen=$askOpen")
                    if (!canSeeScreen) session.requestScreenAccess()
                    else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) session.listenToAsk()
                    else mic.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }
    }
}

@Composable
private fun HostResumeHook() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                BuddyOverlayController.onHostResumed(context)
                AccessibilityController.onHostResumed(context)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
