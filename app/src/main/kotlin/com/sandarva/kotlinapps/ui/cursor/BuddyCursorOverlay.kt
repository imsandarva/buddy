package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.session.BuddyCursorState
import com.sandarva.kotlinapps.ui.theme.BuddyMotion
import kotlin.math.roundToInt

/** Places BuddyCursor from normalized session state. Appear grows from the tip. */
@Composable
fun BuddyCursorOverlay(state: BuddyCursorState, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.buddy_cursor_label)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val xPx = constraints.maxWidth * state.xFraction
        val yPx = constraints.maxHeight * state.yFraction
        AnimatedVisibility(
            visible = state.visible,
            modifier = Modifier.offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) },
            enter = fadeIn(BuddyMotion.enter()) + scaleIn(spring(dampingRatio = 0.62f, stiffness = 380f), 0.82f, TransformOrigin(0f, 0f)),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), 0.92f, TransformOrigin(0f, 0f))
        ) { BuddyCursor(contentDescription = label) }
    }
}
