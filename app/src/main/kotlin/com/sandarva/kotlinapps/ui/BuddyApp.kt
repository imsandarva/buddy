package com.sandarva.kotlinapps.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sandarva.kotlinapps.ui.onboarding.OnboardingRouter
import com.sandarva.kotlinapps.ui.settings.SettingsScreen
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion
import com.sandarva.kotlinapps.ui.theme.BuddyTheme

/**
 * App root — three destinations only: the one-time onboarding funnel, home, and settings. A
 * thin composition layer: it wires state and callbacks, and owns none of the logic itself. The
 * cursor lives in the overlay service, not here. See `docs/onboarding.md` and `docs/ui.md`.
 */
@Composable
fun BuddyApp(session: BuddySessionViewModel = viewModel()) {
    val isRunning by session.isRunning.collectAsStateWithLifecycle()
    val awaitingPermission by session.awaitingPermission.collectAsStateWithLifecycle()
    val canSeeScreen by session.canSeeScreen.collectAsStateWithLifecycle()
    val awaitingAccess by session.awaitingAccess.collectAsStateWithLifecycle()
    val brainPhase by session.brainPhase.collectAsStateWithLifecycle()
    val hasKey by session.hasKey.collectAsStateWithLifecycle()
    val keyInvalid by session.keyInvalid.collectAsStateWithLifecycle()
    val hasLanguage by session.hasLanguage.collectAsStateWithLifecycle()
    val languageCode by session.languageCode.collectAsStateWithLifecycle()
    val introSeen by session.introSeen.collectAsStateWithLifecycle()
    val activationDone by session.activationDone.collectAsStateWithLifecycle()
    val sessionOpens by session.sessionOpens.collectAsStateWithLifecycle()
    val recentActivity by session.recentActivity.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        BuddyLog.d("BuddyApp.mic", "granted=$granted")
        if (granted) session.listenToAsk()
    }
    HostResumeHook()
    BuddyTheme {
        Box(Modifier.fillMaxSize().background(BuddyColors.Paper)) {
            Crossfade(!activationDone, animationSpec = BuddyMotion.crossfade(), label = "appStage") { onboarding ->
                if (onboarding) {
                    OnboardingRouter(
                        introSeen = introSeen,
                        hasKey = hasKey,
                        hasLanguage = hasLanguage,
                        onIntroSeen = session::markIntroSeen,
                        onKeySaved = session::saveApiKey,
                        onLanguageSaved = session::saveLanguage,
                        onActivationDone = session::markActivationDone
                    )
                } else if (showSettings) {
                    SettingsScreen(
                        hasKey = hasKey,
                        keyInvalid = keyInvalid,
                        overlayGranted = isRunning,
                        accessibilityGranted = canSeeScreen,
                        isRunning = isRunning,
                        languageCountryCode = languageCode,
                        onBack = { showSettings = false },
                        onSaveKey = session::saveApiKey,
                        onClearKeyInvalid = session::clearApiKeyInvalid,
                        onSaveLanguage = session::saveLanguage,
                        onFixOverlay = session::startBuddy,
                        onFixAccessibility = session::requestScreenAccess,
                        onStopBuddy = session::stopBuddy,
                        onReset = session::resetBuddy
                    )
                } else {
                    LaunchedEffect(Unit) { session.noteHomeOpened() }
                    HomeScreen(
                        isRunning = isRunning,
                        awaitingPermission = awaitingPermission,
                        canSeeScreen = canSeeScreen,
                        awaitingAccess = awaitingAccess,
                        listening = brainPhase == BrainPhase.Listening || brainPhase == BrainPhase.Live,
                        thinking = brainPhase == BrainPhase.Thinking,
                        live = brainPhase == BrainPhase.Live,
                        working = brainPhase == BrainPhase.Working,
                        keyInvalid = keyInvalid,
                        sessionOpens = sessionOpens,
                        recentActivity = recentActivity,
                        onStartBuddy = session::startBuddy,
                        onStopBuddy = session::stopBuddy,
                        onWatchMove = session::watchBuddyMove,
                        onRequestAccess = session::requestScreenAccess,
                        onPointAtControl = session::pointAtControl,
                        onAskText = session::askWithText,
                        onOpenSettings = { showSettings = true },
                        onAskBuddy = {
                            BuddyLog.d("BuddyApp.askTap", "canSee=$canSeeScreen")
                            if (!canSeeScreen) session.requestScreenAccess()
                            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) session.listenToAsk()
                            else mic.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }
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
