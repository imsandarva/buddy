package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.components.AmbientBackdrop
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/** Stable IME read — true while the keyboard is taking space. */
@Composable
internal fun keyboardOpen(): Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0

/**
 * Shared paper canvas. Content fills the column; an optional [bottom] action sits under it.
 * IME is excluded from the outer safe area and reapplied with [imePadding], so a keyboard
 * shortens this column instead of colliding with it — the standard Compose form pattern.
 */
@Composable
fun OnboardingScaffold(
    modifier: Modifier = Modifier,
    alive: Boolean = false,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val top by animateDpAsState(if (keyboardOpen()) 16.dp else 64.dp, BuddyMotion.crossfade(), label = "onboardTop")
    Box(modifier.fillMaxSize()) {
        AmbientBackdrop(alive = alive, modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime))
                .imePadding()
                .padding(horizontal = 32.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(top = top, bottom = if (bottom == null) 8.dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
            if (bottom != null) {
                Column(Modifier.fillMaxWidth().padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, content = bottom)
            }
        }
    }
}
