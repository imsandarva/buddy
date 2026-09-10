package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/**
 * Owns the one-time first-run funnel end to end (design spec §3). A thin composition layer only —
 * every screen it shows is a self-contained file; this is purely the order they play in and what
 * a "yes" or an honest "not now" does next. [onActivationDone] fires exactly once, however the
 * funnel ends, so a returning user is never asked any of this again.
 */
@Composable
fun OnboardingRouter(
    introSeen: Boolean,
    hasKey: Boolean,
    hasLanguage: Boolean,
    onIntroSeen: () -> Unit,
    onKeySaved: (String) -> Unit,
    onLanguageSaved: (String) -> Unit,
    onActivationDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stage by remember { mutableStateOf(OnboardingStage.Launch) }

    Crossfade(stage, modifier = modifier, animationSpec = BuddyMotion.crossfade(), label = "onboardingStage") { current ->
        when (current) {
            OnboardingStage.Launch -> LaunchScreen(onFinished = { stage = if (introSeen) afterIntro(hasKey, hasLanguage) else OnboardingStage.Intro })
            OnboardingStage.Intro -> IntroScreen(onFinished = { onIntroSeen(); stage = afterIntro(hasKey, hasLanguage) })
            OnboardingStage.ApiKey -> ApiKeyScreen(onSaved = { key -> onKeySaved(key); stage = if (hasLanguage) OnboardingStage.Activation else OnboardingStage.Language })
            OnboardingStage.Language -> LanguageScreen(onSelected = { country -> onLanguageSaved(country.code); stage = OnboardingStage.Activation })
            OnboardingStage.Activation -> ActivationScreen(
                onShowMe = { stage = OnboardingStage.OverlayPermission },
                onMaybeLater = onActivationDone
            )
            OnboardingStage.OverlayPermission -> OverlayPermissionScreen(
                onGranted = { stage = OnboardingStage.AccessibilityPermission },
                onSkipped = onActivationDone
            )
            OnboardingStage.AccessibilityPermission -> AccessibilityPermissionScreen(
                onGranted = onActivationDone,
                onSkipped = onActivationDone
            )
            OnboardingStage.Home -> Unit // never reached — the caller switches away from this router entirely
        }
    }
}

private fun afterIntro(hasKey: Boolean, hasLanguage: Boolean): OnboardingStage = when {
    !hasKey -> OnboardingStage.ApiKey
    !hasLanguage -> OnboardingStage.Language
    else -> OnboardingStage.Activation
}
