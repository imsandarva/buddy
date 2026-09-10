package com.sandarva.kotlinapps.ui.onboarding

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.data.Country
import com.sandarva.kotlinapps.ui.components.LanguagePickerList
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "What language would you like to speak?" — a full-screen searchable flag list, one idea per
 * screen (design idea carried over from a past Flutter nationality step). Tapping a row is the
 * whole interaction: a short beat to see the pick land, then Buddy carries it straight into the
 * first live hello on the next screen.
 */
@Composable
fun LanguageScreen(onSelected: (Country) -> Unit) {
    val scope = rememberCoroutineScope()
    var chosen by remember { mutableStateOf<Country?>(null) }

    fun pick(country: Country) {
        if (chosen != null) return
        chosen = country
        scope.launch {
            delay(600) // let the check land before we carry it into Activation
            onSelected(country)
        }
    }

    OnboardingScaffold {
        FadeSlideIn(0) {
            Text(
                "What language would\nyou like to speak?",
                style = MaterialTheme.typography.displayLarge,
                color = BuddyColors.Ink,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(12.dp))
        FadeSlideIn(60) {
            Text(
                "Pick a country and I'll talk with you in its language — you can change this anytime in Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = BuddyColors.InkMuted,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(22.dp))
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
