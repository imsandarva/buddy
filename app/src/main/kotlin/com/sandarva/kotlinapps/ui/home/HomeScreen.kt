package com.sandarva.kotlinapps.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.ui.components.AmbientBackdrop
import com.sandarva.kotlinapps.ui.components.BuddyActionButton
import com.sandarva.kotlinapps.ui.components.BuddyActionStyle
import com.sandarva.kotlinapps.ui.components.BuddyMark
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

@Composable
fun HomeScreen(
    isRunning: Boolean,
    awaitingPermission: Boolean,
    canSeeScreen: Boolean,
    awaitingAccess: Boolean,
    listening: Boolean,
    thinking: Boolean,
    live: Boolean,
    working: Boolean,
    onStartBuddy: () -> Unit,
    onStopBuddy: () -> Unit,
    onWatchMove: () -> Unit,
    onRequestAccess: () -> Unit,
    onPointAtControl: () -> Unit,
    onAskBuddy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        AmbientBackdrop(alive = isRunning, modifier = Modifier.fillMaxSize())
        HomeForeground(
            isRunning, awaitingPermission, canSeeScreen, awaitingAccess, listening, thinking, live, working,
            onStartBuddy, onStopBuddy, onWatchMove, onRequestAccess, onPointAtControl, onAskBuddy,
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
        )
    }
}

@Composable
private fun HomeForeground(
    isRunning: Boolean,
    awaitingPermission: Boolean,
    canSeeScreen: Boolean,
    awaitingAccess: Boolean,
    listening: Boolean,
    thinking: Boolean,
    live: Boolean,
    working: Boolean,
    onStartBuddy: () -> Unit,
    onStopBuddy: () -> Unit,
    onWatchMove: () -> Unit,
    onRequestAccess: () -> Unit,
    onPointAtControl: () -> Unit,
    onAskBuddy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HomeHero(isRunning, Modifier.fillMaxWidth().padding(top = 80.dp))
        HomeCta(
            isRunning, awaitingPermission, canSeeScreen, awaitingAccess, listening, thinking, live, working,
            onStartBuddy, onStopBuddy, onWatchMove, onRequestAccess, onPointAtControl, onAskBuddy,
            Modifier.fillMaxWidth().padding(bottom = 40.dp)
        )
    }
}

@Composable
private fun HomeHero(isRunning: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FadeSlideIn(0) { BuddyMark(alive = isRunning) }
        Spacer(Modifier.height(40.dp))
        FadeSlideIn(80) {
            Text(
                stringResource(R.string.home_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = BuddyColors.Violet,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(18.dp))
        FadeSlideIn(150) {
            AnimatedContent(
                targetState = isRunning,
                transitionSpec = { fadeIn(BuddyMotion.crossfade()) togetherWith fadeOut(BuddyMotion.crossfade()) },
                label = "heroCopy"
            ) { running ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(if (running) R.string.home_headline_running else R.string.home_headline),
                        style = MaterialTheme.typography.displayLarge,
                        color = BuddyColors.Ink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(if (running) R.string.home_body_running else R.string.home_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = BuddyColors.InkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCta(
    isRunning: Boolean,
    awaitingPermission: Boolean,
    canSeeScreen: Boolean,
    awaitingAccess: Boolean,
    listening: Boolean,
    thinking: Boolean,
    live: Boolean,
    working: Boolean,
    onStartBuddy: () -> Unit,
    onStopBuddy: () -> Unit,
    onWatchMove: () -> Unit,
    onRequestAccess: () -> Unit,
    onPointAtControl: () -> Unit,
    onAskBuddy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val footnote = when {
        working -> R.string.home_footnote_working
        live -> R.string.home_footnote_live
        thinking -> R.string.home_footnote_thinking
        listening -> R.string.home_footnote_listening
        awaitingPermission -> R.string.home_footnote_permission
        isRunning && awaitingAccess -> R.string.home_footnote_access
        isRunning -> R.string.home_footnote_running
        else -> R.string.home_footnote
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FadeSlideIn(280) {
            if (isRunning) BuddyActionButton(stringResource(R.string.stop_buddy), BuddyActionStyle.Stop, onStopBuddy)
            else BuddyActionButton(stringResource(R.string.start_your_buddy), BuddyActionStyle.Start, onStartBuddy)
        }
        AnimatedVisibility(visible = isRunning, enter = fadeIn(BuddyMotion.crossfade()), exit = fadeOut(BuddyMotion.crossfade())) {
            HomeRunningActions(canSeeScreen, onWatchMove, onRequestAccess, onPointAtControl, onAskBuddy)
        }
        Spacer(Modifier.height(20.dp))
        FadeSlideIn(360) {
            Text(stringResource(footnote), style = MaterialTheme.typography.bodySmall, color = BuddyColors.Mist, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HomeRunningActions(
    canSeeScreen: Boolean,
    onWatchMove: () -> Unit,
    onRequestAccess: () -> Unit,
    onPointAtControl: () -> Unit,
    onAskBuddy: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (canSeeScreen) QuietTextAction(stringResource(R.string.ask_buddy), onAskBuddy)
        else QuietTextAction(stringResource(R.string.let_me_see_screen), onRequestAccess)
        Spacer(Modifier.height(18.dp))
        QuietTextAction(stringResource(R.string.watch_buddy_move), onWatchMove)
        if (canSeeScreen) {
            Spacer(Modifier.height(18.dp))
            QuietTextAction(stringResource(R.string.point_at_something), onPointAtControl)
        }
    }
}
