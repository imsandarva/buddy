package com.sandarva.kotlinapps.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.data.ApiKeyValidator
import com.sandarva.kotlinapps.ui.components.ApiKeyField
import com.sandarva.kotlinapps.ui.components.BuddyActionButton
import com.sandarva.kotlinapps.ui.components.BuddyActionStyle
import com.sandarva.kotlinapps.ui.components.BuddyMark
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class KeyCheck { Idle, Checking, Valid, Invalid }

/**
 * "Give buddy a brain" — turns a dry technical chore into a small ritual. Waking the dormant orb
 * is the reward for a correct key, caused directly by what the person just did (design spec §6).
 * One flex spacer: at rest it sits between the field and the CTA; while they type it sits above
 * the field, so the field and **Wake buddy up** hug the keyboard like any standard form.
 */
@Composable
fun ApiKeyScreen(onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var check by remember { mutableStateOf(KeyCheck.Idle) }
    var reason by remember { mutableStateOf<String?>(null) }
    val compact = keyboardOpen()

    fun runCheck() {
        val key = draft.trim()
        if (key.isBlank() || check == KeyCheck.Checking) return
        check = KeyCheck.Checking
        reason = null
        scope.launch {
            when (val result = ApiKeyValidator.check(context, key)) {
                ApiKeyValidator.Result.Valid -> {
                    check = KeyCheck.Valid
                    delay(1100) // let the wake-up beat land before we move on
                    onSaved(key)
                }
                is ApiKeyValidator.Result.Invalid -> {
                    check = KeyCheck.Invalid
                    reason = result.reason
                }
            }
        }
    }

    OnboardingScaffold {
        ApiKeyHero(alive = check == KeyCheck.Valid, compact = compact)
        if (compact) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(32.dp))
        FadeSlideIn(160) {
            ApiKeyField(
                value = draft,
                onValueChange = { draft = it; if (check == KeyCheck.Invalid) check = KeyCheck.Idle },
                onSubmit = ::runCheck,
                enabled = check != KeyCheck.Checking && check != KeyCheck.Valid
            )
        }
        SoftHide(visible = !compact) {
            Spacer(Modifier.height(14.dp))
            QuietTextAction("Don't have one? Get a free key", onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GET_KEY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            })
        }
        Spacer(Modifier.height(12.dp))
        KeyCheckNote(check, reason)
        if (!compact) Spacer(Modifier.weight(1f))
        ApiKeyWakeBar(check, compact, ::runCheck)
    }
}

@Composable
private fun ApiKeyHero(alive: Boolean, compact: Boolean) {
    val mark by animateDpAsState(if (compact) 48.dp else 76.dp, BuddyMotion.crossfade(), label = "mark")
    val afterMark by animateDpAsState(if (compact) 14.dp else 32.dp, BuddyMotion.crossfade(), label = "afterMark")
    FadeSlideIn(0) { BuddyMark(alive = alive, modifier = Modifier.size(mark)) }
    Spacer(Modifier.height(afterMark))
    FadeSlideIn(60) {
        Text("Give me a brain.", style = MaterialTheme.typography.displayLarge, color = BuddyColors.Ink, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(12.dp))
    FadeSlideIn(110) {
        Text(
            "Paste in a Gemini key and I'll come to life. It's free to get, and only takes a minute.",
            style = MaterialTheme.typography.bodyLarge,
            color = BuddyColors.InkMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun KeyCheckNote(check: KeyCheck, reason: String?) {
    AnimatedContent(
        targetState = check,
        transitionSpec = { fadeIn(BuddyMotion.crossfade()) togetherWith fadeOut(BuddyMotion.crossfade()) },
        label = "keyStatus"
    ) { state ->
        val message = when (state) {
            KeyCheck.Checking -> "Checking…" to BuddyColors.InkMuted
            KeyCheck.Valid -> "There you go — I'm awake." to BuddyColors.Violet
            KeyCheck.Invalid -> (reason ?: "That key didn't work.") to BuddyColors.DismissFillArmed
            KeyCheck.Idle -> "" to BuddyColors.Mist
        }
        if (message.first.isNotBlank()) Text(message.first, style = MaterialTheme.typography.bodySmall, color = message.second, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ApiKeyWakeBar(check: KeyCheck, compact: Boolean, onWake: () -> Unit) {
    FadeSlideIn(0) {
        BuddyActionButton(
            label = if (check == KeyCheck.Checking) "Checking…" else "Wake buddy up",
            style = BuddyActionStyle.Start,
            onClick = onWake
        )
    }
    SoftHide(visible = !compact) {
        Spacer(Modifier.height(18.dp))
        Text(
            "Your key stays on this device only — it's never sent anywhere but Google.",
            style = MaterialTheme.typography.bodySmall,
            color = BuddyColors.Mist,
            textAlign = TextAlign.Center
        )
    }
}

/** Footnotes and extra links step aside while they type; the title line always stays. */
@Composable
private fun SoftHide(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(BuddyMotion.crossfade()) + expandVertically(),
        exit = fadeOut(BuddyMotion.crossfade()) + shrinkVertically()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = { content() })
    }
}

private const val GET_KEY_URL = "https://aistudio.google.com/apikey"
