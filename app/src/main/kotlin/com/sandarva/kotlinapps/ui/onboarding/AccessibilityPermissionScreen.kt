package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sandarva.kotlinapps.accessibility.AccessibilityController
import com.sandarva.kotlinapps.accessibility.AccessibilitySession
import com.sandarva.kotlinapps.ui.components.AccessibilityGlyph

/**
 * The single highest-stakes screen in the app (design spec §10). Android's own dialog will use
 * generic, alarming language; this is the only chance to say plainly what it enables and, just as
 * plainly, what it never does, before that dialog appears.
 */
@Composable
fun AccessibilityPermissionScreen(onGranted: () -> Unit, onSkipped: () -> Unit) {
    val context = LocalContext.current
    val enabled by AccessibilitySession.enabled.collectAsStateWithLifecycle()

    LaunchedEffect(enabled) { if (enabled) onGranted() }

    PermissionPrimer(
        glyph = { AccessibilityGlyph() },
        title = "Let me do things\non your screen.",
        why = "I'll tap, type, and change settings for you, but I need to see the screen to do that. Only when you ask.",
        steps = listOf(
            "Tap Turn it on below.",
            "Open Installed apps.",
            "Tap Buddy Assistant.",
            "Turn the switch on."
        ),
        note = "I don't record your screen, or send it anywhere.",
        action = "Turn it on",
        onAllow = { AccessibilityController.requestAccess(context) },
        onSkip = onSkipped
    )
}
