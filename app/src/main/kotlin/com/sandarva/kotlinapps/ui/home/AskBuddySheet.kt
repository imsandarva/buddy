package com.sandarva.kotlinapps.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.brain.BrainPhase
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/** Owned panel — not ModalBottomSheet, so dismiss cannot leave a stuck scrim. */
@Composable
fun AskBuddySheet(
    phase: BrainPhase,
    note: String?,
    onDismiss: () -> Unit,
    onAskText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(if (shown) 1f else 0f, tween(420, easing = BuddyMotion.EnterEasing), label = "askEnter")
    Box(modifier.fillMaxSize().graphicsLayer { alpha = 0.4f + 0.6f * progress }) {
        Box(
            Modifier
                .fillMaxSize()
                .background(BuddyColors.Ink.copy(alpha = 0.46f))
                .clickable(
                    enabled = phase != BrainPhase.Thinking,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    BuddyLog.d("AskPanel", "scrim tap → dismiss")
                    onDismiss()
                }
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - progress) * 40f }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(BuddyColors.Ink)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(top = 12.dp, bottom = 28.dp)
                .clickable(remember { MutableInteractionSource() }, null) { }
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.padding(bottom = 16.dp).width(48.dp).height(4.dp).background(BuddyColors.Honey.copy(alpha = 0.55f), RoundedCornerShape(50)))
            }
            AskBuddyBody(phase, note, onAskText)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                QuietTextAction(stringResource(R.string.ask_not_now), onClick = {
                    BuddyLog.d("AskPanel", "not now → dismiss")
                    onDismiss()
                })
            }
        }
    }
}

@Composable
private fun AskBuddyBody(phase: BrainPhase, note: String?, onAskText: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
        val title = when (phase) {
            BrainPhase.Listening, BrainPhase.Live -> R.string.ask_title_listening
            BrainPhase.Thinking -> R.string.ask_title_thinking
            BrainPhase.Idle -> R.string.ask_title_idle
        }
        val body = note ?: stringResource(
            when (phase) {
                BrainPhase.Listening, BrainPhase.Live -> R.string.ask_body_listening
                BrainPhase.Thinking -> R.string.ask_body_thinking
                BrainPhase.Idle -> R.string.ask_body_idle
            }
        )
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, color = BuddyColors.Bone, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = if (note != null) BuddyColors.Honey else BuddyColors.BoneMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.ask_placeholder), color = BuddyColors.Mist) },
            singleLine = true,
            enabled = phase != BrainPhase.Thinking,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                BuddyLog.d("AskPanel", "typed send len=${draft.trim().length}")
                onAskText(draft)
                draft = ""
            }),
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BuddyColors.Honey,
                unfocusedBorderColor = BuddyColors.Sage.copy(alpha = 0.45f),
                focusedTextColor = BuddyColors.Bone,
                unfocusedTextColor = BuddyColors.Bone,
                cursorColor = BuddyColors.Honey
            )
        )
    }
}
