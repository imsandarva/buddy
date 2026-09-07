package com.sandarva.kotlinapps.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

@Composable
fun Modifier.pressScale(pressed: Boolean, down: Float = 0.985f): Modifier {
    val scale by animateFloatAsState(if (pressed) down else 1f, BuddyMotion.Press, label = "press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}
