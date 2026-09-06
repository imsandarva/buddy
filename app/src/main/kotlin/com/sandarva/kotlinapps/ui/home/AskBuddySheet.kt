package com.sandarva.kotlinapps.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.brain.BrainPhase
import com.sandarva.kotlinapps.ui.theme.BuddyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskBuddySheet(
    phase: BrainPhase,
    note: String?,
    onDismiss: () -> Unit,
    onAskText: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BuddyColors.Ink,
        contentColor = BuddyColors.Bone,
        scrimColor = BuddyColors.Ink.copy(alpha = 0.46f),
        dragHandle = { SheetHandle() }
    ) {
        AskBuddyBody(phase, note, onAskText, Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 36.dp))
    }
}

@Composable
private fun SheetHandle() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Box(Modifier.padding(bottom = 8.dp).width(48.dp).height(4.dp).background(BuddyColors.Honey.copy(alpha = 0.55f), RoundedCornerShape(50)))
    }
}

@Composable
private fun AskBuddyBody(phase: BrainPhase, note: String?, onAskText: (String) -> Unit, modifier: Modifier = Modifier) {
    var draft by remember { mutableStateOf("") }
    val title = when (phase) {
        BrainPhase.Listening -> R.string.ask_title_listening
        BrainPhase.Thinking -> R.string.ask_title_thinking
        BrainPhase.Idle -> R.string.ask_title_idle
    }
    val body = note ?: stringResource(
        when (phase) {
            BrainPhase.Listening -> R.string.ask_body_listening
            BrainPhase.Thinking -> R.string.ask_body_thinking
            BrainPhase.Idle -> R.string.ask_body_idle
        }
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
            keyboardActions = KeyboardActions(onSend = { onAskText(draft); draft = "" }),
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
