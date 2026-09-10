package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sandarva.kotlinapps.ui.components.AmbientBackdrop
import com.sandarva.kotlinapps.ui.components.BuddyMark
import kotlinx.coroutines.delay

/**
 * The very first thing anyone sees — just buddy's own mark, the held/drag orb, breathing
 * alone on paper. The identity is the orb; this beat lasts exactly as long as it takes to feel
 * that, then moves on (design spec §4).
 */
@Composable
fun LaunchScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(900)
        onFinished()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AmbientBackdrop(alive = false, modifier = Modifier.fillMaxSize())
        BuddyMark(alive = false)
    }
}
