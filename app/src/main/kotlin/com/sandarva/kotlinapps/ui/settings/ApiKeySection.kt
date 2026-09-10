package com.sandarva.kotlinapps.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.data.ApiKeyStore
import com.sandarva.kotlinapps.data.ApiKeyValidator
import com.sandarva.kotlinapps.ui.components.ApiKeyField
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion
import kotlinx.coroutines.launch

private enum class Mode { Summary, Editing, Checking }

/** Boring on purpose (design §13): view masked, replace with the same live-check as first setup. */
@Composable
fun ApiKeySection(hasKey: Boolean, keyInvalid: Boolean, onSaved: (String) -> Unit, onCleared: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(if (hasKey && !keyInvalid) Mode.Summary else Mode.Editing) }
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(keyInvalid) { if (keyInvalid) mode = Mode.Editing }

    fun submit() {
        val key = draft.trim()
        if (key.isBlank()) return
        mode = Mode.Checking
        error = null
        scope.launch {
            when (val result = ApiKeyValidator.check(context, key)) {
                ApiKeyValidator.Result.Valid -> { onSaved(key); draft = ""; mode = Mode.Summary }
                is ApiKeyValidator.Result.Invalid -> { error = result.reason; mode = Mode.Editing }
            }
        }
    }

    SettingsSection(title = "Your key") {
        AnimatedContent(mode, transitionSpec = { fadeIn(BuddyMotion.crossfade()) togetherWith fadeOut(BuddyMotion.crossfade()) }, label = "apiKeyMode") { state ->
            when (state) {
                Mode.Summary -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(ApiKeyStore.masked(), style = MaterialTheme.typography.bodyLarge, color = BuddyColors.Ink)
                    QuietTextAction("Replace", onClick = { mode = Mode.Editing })
                }
                Mode.Editing, Mode.Checking -> Column(Modifier.fillMaxWidth()) {
                    if (keyInvalid) {
                        Text("Your key stopped working — add a new one.", style = MaterialTheme.typography.bodySmall, color = BuddyColors.DismissFillArmed)
                        Spacer(Modifier.height(10.dp))
                    }
                    ApiKeyField(draft, { draft = it; error = null }, ::submit, enabled = state != Mode.Checking)
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = BuddyColors.DismissFillArmed)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (hasKey && !keyInvalid) {
                            QuietTextAction("Cancel", onClick = { mode = Mode.Summary })
                            Spacer(Modifier.width(24.dp))
                        }
                        QuietTextAction(if (state == Mode.Checking) "Checking…" else "Save", onClick = ::submit)
                    }
                }
            }
        }
    }
}
