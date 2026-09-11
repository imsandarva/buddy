package com.sandarva.kotlinapps.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/**
 * A line to type on — no box, no fill, no chrome. The underline is the only edge; it darkens
 * when the field is focused so the hand always knows where the words will land.
 */
@Composable
fun UnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Search
) {
    var focused by remember { mutableStateOf(false) }
    val line by animateColorAsState(
        if (!enabled) BuddyColors.Line else if (focused) BuddyColors.Ink else BuddyColors.Line,
        BuddyMotion.crossfade(),
        label = "underline"
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .onFocusChanged { focused = it.isFocused },
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (enabled) BuddyColors.Ink else BuddyColors.Mist),
        cursorBrush = SolidColor(BuddyColors.Violet),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        decorationBox = { inner ->
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = BuddyColors.Mist)
                    inner()
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(line))
            }
        }
    )
}
