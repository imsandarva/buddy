package com.sandarva.kotlinapps.ui.onboarding

/**
 * The whole first-run sequence, front door to home screen (design spec §3). Everything before
 * [Home] happens at most once per install — see `docs/onboarding.md`.
 */
enum class OnboardingStage { Launch, Intro, ApiKey, Activation, OverlayPermission, AccessibilityPermission, Home }
