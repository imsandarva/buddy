package com.sandarva.kotlinapps.ui.cursor

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/** Grab target for BuddyCursor. Reports finger deltas; the host (window or layout) applies movement. */
@Composable
fun BuddyCursorHandle(
    onDrag: (Float, Float) -> Unit,
    onRelease: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onGrab: () -> Unit = {},
    onDoubleTap: () -> Unit = {}
) {
    var held by remember { mutableStateOf(false) }
    val grab = rememberGrabMotion(held)
    val view = LocalView.current
    Box(
        modifier
            .graphicsLayer {
                scaleX = grab.scale
                scaleY = grab.scale
                rotationZ = grab.rotation
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .size(56.dp, 68.dp)
            .pointerInput(Unit) {
                dragBuddyCursor(
                    onGrab = {
                        held = true
                        onGrab()
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    onDrag = onDrag,
                    onRelease = {
                        held = false
                        onRelease()
                    },
                    onDoubleTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onDoubleTap()
                    }
                )
            }
    ) { BuddyCursor(held = held, contentDescription = label) }
}
