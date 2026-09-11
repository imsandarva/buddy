package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.data.Country
import com.sandarva.kotlinapps.ui.components.LanguagePickerList
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * First-run language — a short ask, a line to type on, and the countries. Tapping a row is the
 * whole interaction: a brief beat so the pick lands, then Activation speaks in that language.
 */
@Composable
fun LanguageScreen(onSelected: (Country) -> Unit) {
    val scope = rememberCoroutineScope()
    var chosen by remember { mutableStateOf<Country?>(null) }
    val compact = keyboardOpen()

    fun pick(country: Country) {
        if (chosen != null) return
        chosen = country
        scope.launch {
            delay(520)
            onSelected(country)
        }
    }

    OnboardingScaffold {
        LanguageHero(compact)
        Spacer(Modifier.height(if (compact) 18.dp else 28.dp))
        FadeSlideIn(110, modifier = Modifier.fillMaxWidth().weight(1f)) {
            LanguagePickerList(
                selectedCode = chosen?.code,
                enabled = chosen == null,
                modifier = Modifier.fillMaxWidth(),
                onSelected = ::pick
            )
        }
    }
}

@Composable
private fun LanguageHero(compact: Boolean) {
    FadeSlideIn(0) {
        Text(
            "Choose your language",
            modifier = Modifier.fillMaxWidth(),
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displayLarge,
            color = BuddyColors.Ink,
            textAlign = TextAlign.Start
        )
    }
    AnimatedVisibility(
        visible = !compact,
        enter = fadeIn(BuddyMotion.crossfade()) + expandVertically(),
        exit = fadeOut(BuddyMotion.crossfade()) + shrinkVertically()
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Spacer(Modifier.height(12.dp))
            Text(
                "You can choose a country to speak in the language of that country.",
                style = MaterialTheme.typography.bodyLarge,
                color = BuddyColors.InkMuted,
                textAlign = TextAlign.Start
            )
        }
    }
}
