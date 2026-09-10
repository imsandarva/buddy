package com.sandarva.kotlinapps.ui.onboarding

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.brain.live.LiveHello
import com.sandarva.kotlinapps.ui.components.BuddyActionButton
import com.sandarva.kotlinapps.ui.components.BuddyActionStyle
import com.sandarva.kotlinapps.ui.components.BuddyMark
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

private const val GREETING = "Hey — I'm buddy."
private const val REVEAL_HEADLINE = "I can also act on\nyour screen."
private const val REVEAL_BODY = "Tap, type, scroll — not just talk. Want to see how?"

/**
 * First conversation, then the capability reveal, in one continuous beat (design spec §7–8).
 * The hello is Gemini Live's own voice — one shot, then hang up. No device TTS, no lingering talk.
 */
@Composable
fun ActivationScreen(onShowMe: () -> Unit, onMaybeLater: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    var revealing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        LiveHello.play(app) { revealing = true }
        onDispose { LiveHello.cancel() }
    }

    OnboardingScaffold(
        alive = true,
        bottom = {
            AnimatedVisibility(revealing, enter = fadeIn(BuddyMotion.crossfade()), exit = fadeOut(BuddyMotion.crossfade())) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BuddyActionButton("Show me", BuddyActionStyle.Start, onClick = { LiveHello.cancel(); onShowMe() })
                    Spacer(Modifier.height(18.dp))
                    QuietTextAction("Maybe later", onClick = { LiveHello.cancel(); onMaybeLater() })
                }
            }
        }
    ) {
        FadeSlideIn(0) { BuddyMark(alive = true) }
        Spacer(Modifier.height(40.dp))
        AnimatedContent(
            targetState = revealing,
            transitionSpec = { fadeIn(BuddyMotion.crossfade()) togetherWith fadeOut(BuddyMotion.crossfade()) },
            label = "activationCopy"
        ) { onReveal ->
            if (onReveal) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(REVEAL_HEADLINE, style = MaterialTheme.typography.displayLarge, color = BuddyColors.Ink, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Text(REVEAL_BODY, style = MaterialTheme.typography.bodyLarge, color = BuddyColors.InkMuted, textAlign = TextAlign.Center)
                }
            } else {
                Text(GREETING, style = MaterialTheme.typography.displayLarge, color = BuddyColors.Ink, textAlign = TextAlign.Center)
            }
        }
    }
}
