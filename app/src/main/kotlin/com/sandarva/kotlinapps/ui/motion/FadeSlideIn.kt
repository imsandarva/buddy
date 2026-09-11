package com.sandarva.kotlinapps.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.sandarva.kotlinapps.ui.theme.BuddyMotion
import kotlinx.coroutines.delay

/** One-shot fade + rise. A Box — [content] must be a single layout root, not loose siblings. */
@Composable
fun FadeSlideIn(delayMillis: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        shown = true
    }
    val progress by animateFloatAsState(if (shown) 1f else 0f, BuddyMotion.enter(), label = "enter")
    Box(modifier.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 18f
    }) { content() }
}
