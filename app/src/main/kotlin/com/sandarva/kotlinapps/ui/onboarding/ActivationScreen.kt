package com.sandarva.kotlinapps.ui.onboarding

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.brain.BuddyVoice
import com.sandarva.kotlinapps.ui.components.BuddyActionButton
import com.sandarva.kotlinapps.ui.components.BuddyActionStyle
import com.sandarva.kotlinapps.ui.components.BuddyMark
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

private val GREETING = listOf("Hey — I'm buddy.", "Nice to meet you.")
private const val REVEAL_HEADLINE = "I can also act on\nyour screen."
private const val REVEAL_BODY = "Tap, type, scroll — not just talk. Want to see how?"

/**
 * First conversation, then the capability reveal, in one continuous beat (design spec §7–8). Talk
 * needs nothing but the key already given — zero permission risk, buddy's first proof of life.
 */
@Composable
fun ActivationScreen(onShowMe: () -> Unit, onMaybeLater: () -> Unit) {
    val context = LocalContext.current
    val voice = remember { BuddyVoice(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { voice.release() } }
    var step by remember { mutableIntStateOf(0) }
    val revealing = step >= GREETING.size

    LaunchedEffect(step) {
        if (step < GREETING.size) voice.speak(GREETING[step]) { step += 1 }
        else if (step == GREETING.size) voice.speak("$REVEAL_HEADLINE $REVEAL_BODY".replace("\n", " "))
    }

    OnboardingScaffold(
        alive = true,
        bottom = {
            AnimatedVisibility(revealing, enter = fadeIn(BuddyMotion.crossfade()), exit = fadeOut(BuddyMotion.crossfade())) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BuddyActionButton("Show me", BuddyActionStyle.Start, onClick = { voice.cancelAll(); onShowMe() })
                    Spacer(Modifier.height(18.dp))
                    QuietTextAction("Maybe later", onClick = { voice.cancelAll(); onMaybeLater() })
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
                Text(GREETING.getOrElse(step) { GREETING.last() }, style = MaterialTheme.typography.displayLarge, color = BuddyColors.Ink, textAlign = TextAlign.Center)
            }
        }
    }
}
