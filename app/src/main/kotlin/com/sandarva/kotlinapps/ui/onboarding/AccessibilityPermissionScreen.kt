package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sandarva.kotlinapps.accessibility.AccessibilityController
import com.sandarva.kotlinapps.accessibility.AccessibilitySession
import com.sandarva.kotlinapps.ui.components.AccessibilityGlyph
import com.sandarva.kotlinapps.ui.components.BuddyActionButton
import com.sandarva.kotlinapps.ui.components.BuddyActionStyle
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors

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

    OnboardingScaffold(
        bottom = {
            FadeSlideIn(0) {
                BuddyActionButton("Turn it on", BuddyActionStyle.Start, onClick = { AccessibilityController.requestAccess(context) })
            }
            Spacer(Modifier.height(18.dp))
            QuietTextAction("Not now", onSkipped)
        }
    ) {
        FadeSlideIn(0) { AccessibilityGlyph() }
        Spacer(Modifier.height(24.dp))
        FadeSlideIn(60) {
            Text("Let me tap and\ntype for you.", style = MaterialTheme.typography.displayLarge, color = BuddyColors.Ink, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(14.dp))
        FadeSlideIn(110) {
            Text(
                "This lets me see what's on your screen and press things, the same way your finger does — only when you ask.",
                style = MaterialTheme.typography.bodyLarge,
                color = BuddyColors.InkMuted,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(10.dp))
        FadeSlideIn(150) {
            Text(
                "I never record, store, or send your screen anywhere.",
                style = MaterialTheme.typography.bodySmall,
                color = BuddyColors.Mist,
                textAlign = TextAlign.Center
            )
        }
    }
}
