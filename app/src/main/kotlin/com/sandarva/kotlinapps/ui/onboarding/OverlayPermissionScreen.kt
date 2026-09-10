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
import com.sandarva.kotlinapps.overlay.BuddyOverlayController
import com.sandarva.kotlinapps.overlay.OverlaySession
import com.sandarva.kotlinapps.ui.components.BuddyActionButton
import com.sandarva.kotlinapps.ui.components.BuddyActionStyle
import com.sandarva.kotlinapps.ui.components.OverlayGlyph
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors
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

    OnboardingScaffold(
        bottom = {
            FadeSlideIn(0) {
                BuddyActionButton("Allow it", BuddyActionStyle.Start, onClick = { BuddyOverlayController.requestStart(context) })
            }
            Spacer(Modifier.height(18.dp))
            QuietTextAction("Not now", onSkipped)
        }
    ) {
        FadeSlideIn(0) { OverlayGlyph() }
        Spacer(Modifier.height(32.dp))
        FadeSlideIn(60) {
            Text("Let me stay on\nyour screen.", style = MaterialTheme.typography.displayLarge, color = BuddyColors.Ink, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(14.dp))
        FadeSlideIn(110) {
            Text(
                "This lets me float above whatever you're doing, so I'm always one tap away.",
                style = MaterialTheme.typography.bodyLarge,
                color = BuddyColors.InkMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
