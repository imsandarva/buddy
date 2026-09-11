package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.components.BuddyActionButton
import com.sandarva.kotlinapps.ui.components.BuddyActionStyle
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/**
 * Shared paper for the two permission asks — glyph, one headline, a short how-to, one action.
 * Overlay and Accessibility only supply copy and the grant/skip side effects.
 */
@Composable
internal fun PermissionPrimer(
    glyph: @Composable () -> Unit,
    title: String,
    why: String,
    steps: List<String>,
    action: String,
    onAllow: () -> Unit,
    onSkip: () -> Unit,
    note: String? = null
) {
    OnboardingScaffold(
        bottom = {
            FadeSlideIn(0) {
                BuddyActionButton(action, BuddyActionStyle.Start, onClick = onAllow)
            }
            Spacer(Modifier.height(16.dp))
            QuietTextAction("Not now", onSkip)
        }
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FadeSlideIn(0) { glyph() }
                Spacer(Modifier.height(28.dp))
                FadeSlideIn(60, modifier = Modifier.fillMaxWidth()) {
                    Text(title, style = MaterialTheme.typography.displayLarge, color = BuddyColors.Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(16.dp))
                FadeSlideIn(110, modifier = Modifier.fillMaxWidth()) {
                    PermissionHowTo(why, steps, note)
                }
            }
        }
    }
}
