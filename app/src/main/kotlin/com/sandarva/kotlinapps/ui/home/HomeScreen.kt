package com.sandarva.kotlinapps.ui.home

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
import com.sandarva.kotlinapps.ui.components.BuddyMark
import com.sandarva.kotlinapps.ui.components.StartBuddyButton
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors

@Composable
fun HomeScreen(onStartBuddy: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        AmbientBackdrop(Modifier.fillMaxSize())
        HomeForeground(onStartBuddy, Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing))
    }
}

@Composable
private fun HomeForeground(onStartBuddy: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HomeHero(Modifier.fillMaxWidth().padding(top = 72.dp))
        HomeCta(onStartBuddy, Modifier.fillMaxWidth().padding(bottom = 36.dp))
    }
}

@Composable
private fun HomeHero(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FadeSlideIn(0) { BuddyMark() }
        Spacer(Modifier.height(36.dp))
        FadeSlideIn(90) {
            Text(
                stringResource(R.string.home_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = BuddyColors.Sage,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))
        FadeSlideIn(160) {
            Text(
                stringResource(R.string.home_headline),
                style = MaterialTheme.typography.displayLarge,
                color = BuddyColors.Bone,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(18.dp))
        FadeSlideIn(240) {
            Text(
                stringResource(R.string.home_body),
                style = MaterialTheme.typography.bodyLarge,
                color = BuddyColors.BoneMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun HomeCta(onStartBuddy: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FadeSlideIn(320) { StartBuddyButton(onStartBuddy) }
        Spacer(Modifier.height(16.dp))
        FadeSlideIn(400) {
            Text(stringResource(R.string.home_footnote), style = MaterialTheme.typography.bodySmall, color = BuddyColors.Mist, textAlign = TextAlign.Center)
        }
    }
}
