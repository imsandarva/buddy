package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.brain.agent.AppLauncher
import com.sandarva.kotlinapps.data.ActivityLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import com.sandarva.kotlinapps.ui.components.AmbientBackdrop
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion
import kotlinx.coroutines.delay

/**
 * Deliberately trivial, deliberately safe: buddycursor's full travel/target/act state machine
 * plays out in full view for the first time, then buddy opens the calculator — zero downside if
 * anything goes even slightly wrong. Boring but flawless beats impressive but risky (design §11).
 */
@Composable
fun FirstTaskScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var caption by remember { mutableStateOf("Let's try something small and safe.") }

    LaunchedEffect(Unit) {
        delay(700)
        BuddyCursorController.playDemo()
        delay(1900) // roughly the demo path's own travel time — see BuddyCursorController.DEMO_PATH
        caption = "I'll go ahead and open your calculator."
        delay(900)
        runCatching { AppLauncher(context.applicationContext).open("Calculator") }
        ActivityLog.record("Opened Calculator")
        delay(1400)
        onFinished()
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AmbientBackdrop(alive = true, modifier = Modifier.fillMaxSize())
        AnimatedContent(
            targetState = caption,
            transitionSpec = { fadeIn(BuddyMotion.crossfade()) togetherWith fadeOut(BuddyMotion.crossfade()) },
            label = "firstTaskCaption"
        ) { line ->
            Text(
                line,
                style = MaterialTheme.typography.titleLarge,
                color = BuddyColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}
