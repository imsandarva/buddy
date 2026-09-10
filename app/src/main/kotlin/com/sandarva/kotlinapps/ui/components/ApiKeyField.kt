package com.sandarva.kotlinapps.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/**
 * One clean field for pasting a key — monospace so it's legible at a glance, a show/hide toggle
 * so a paste can be double-checked, and a single "done" affordance so the ritual has a clear end.
 */
@Composable
fun ApiKeyField(value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    var revealed by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(fontFamily = FontFamily.Monospace),
        placeholder = { Text("Paste your key here", color = BuddyColors.Mist, fontFamily = FontFamily.Default) },
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation('•'),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        trailingIcon = {
            TextButton(onClick = { revealed = !revealed }) {
                Text(if (revealed) "Hide" else "Show", color = BuddyColors.Violet, style = MaterialTheme.typography.bodySmall)
            }
        },
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BuddyColors.Violet,
            unfocusedBorderColor = BuddyColors.Line,
            focusedTextColor = BuddyColors.Ink,
            unfocusedTextColor = BuddyColors.Ink,
            focusedContainerColor = BuddyColors.Snow,
            unfocusedContainerColor = BuddyColors.Snow,
            cursorColor = BuddyColors.Violet
        )
    )
}
