package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sandarva.kotlinapps.overlay.BuddyOverlayController
import com.sandarva.kotlinapps.overlay.OverlaySession
import com.sandarva.kotlinapps.ui.components.OverlayGlyph
import kotlinx.coroutines.delay

/**
 * The "easy yes" — appear-on-top is a permission most people already recognize from chat-head
 * apps. Granting it earns a concrete, immediate reward: buddycursor actually settles onto their
 * real screen right there (design spec §9), which makes the harder ask next feel closer, not fresh.
 */
@Composable
fun OverlayPermissionScreen(onGranted: () -> Unit, onSkipped: () -> Unit) {
    val context = LocalContext.current
    val active by OverlaySession.active.collectAsStateWithLifecycle()

    LaunchedEffect(active) {
        if (active) {
            delay(1100) // let them actually watch it settle before we move on
            onGranted()
        }
    }

    PermissionPrimer(
        glyph = { OverlayGlyph() },
        title = "Let me stay on\nyour screen.",
        why = "I'll float above whatever you're doing, so I'm always a tap away.",
        steps = listOf(
            "Tap Allow it below.",
            "Turn on Appear on top for Buddy."
        ),
        note = "To send me away later, hold me and drag to the X.",
        action = "Allow it",
        onAllow = { BuddyOverlayController.requestStart(context) },
        onSkip = onSkipped
    )
}
