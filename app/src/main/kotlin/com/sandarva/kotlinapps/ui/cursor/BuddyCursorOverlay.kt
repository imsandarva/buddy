package com.sandarva.kotlinapps.ui.cursor

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.session.BuddyCursorState
import com.sandarva.kotlinapps.session.BuddySessionViewModel
import com.sandarva.kotlinapps.ui.theme.BuddyMotion
import kotlin.math.roundToInt

/** Collects session cursor state here so home does not recompose during drags. */
@Composable
fun BuddyCursorLayer(session: BuddySessionViewModel) {
    val cursor by session.cursor.collectAsStateWithLifecycle()
    BuddyCursorOverlay(cursor, session::moveCursor)
}

@Composable
fun BuddyCursorOverlay(state: BuddyCursorState, onMoved: (Float, Float) -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.buddy_cursor_label)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val maxX = constraints.maxWidth.toFloat()
        val maxY = constraints.maxHeight.toFloat()
        AnimatedVisibility(visible = state.visible, enter = fadeIn(BuddyMotion.enter()), exit = fadeOut()) {
            BuddyCursorHandle(
                startX = maxX * state.xFraction,
                startY = maxY * state.yFraction,
                maxX = maxX,
                maxY = maxY,
                onMoved = onMoved,
                label = label
            )
        }
    }
}

@Composable
private fun BuddyCursorHandle(
    startX: Float,
    startY: Float,
    maxX: Float,
    maxY: Float,
    onMoved: (Float, Float) -> Unit,
    label: String
) {
    var x by remember { mutableFloatStateOf(startX) }
    var y by remember { mutableFloatStateOf(startY) }
    var held by remember { mutableStateOf(false) }
    val grab = rememberGrabMotion(held)
    val view = LocalView.current
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .graphicsLayer {
                scaleX = grab.scale
                scaleY = grab.scale
                rotationZ = grab.rotation
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .size(56.dp, 68.dp)
            .pointerInput(maxX, maxY) {
                dragBuddyCursor(
                    onGrab = {
                        held = true
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    onDrag = { dx, dy ->
                        x = (x + dx).coerceIn(0f, maxX)
                        y = (y + dy).coerceIn(0f, maxY)
                    },
                    onRelease = {
                        held = false
                        if (maxX > 0f && maxY > 0f) onMoved(x / maxX, y / maxY)
                    }
                )
            }
    ) { BuddyCursor(held = held, contentDescription = label) }
}
