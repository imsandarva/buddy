package com.sandarva.kotlinapps.ui.cursor

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView

/** Touch target around the being; tap, double-tap, hold, and drag all stay on this layer. */
@Composable
fun BuddyCursorHandle(
    mood: CursorMood,
    onDrag: (Float, Float) -> Unit,
    onRelease: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onGrab: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onInterrupt: () -> Unit = {}
) {
    val view = LocalView.current
    Box(
        modifier
            .size(Cursor.touchWidth, Cursor.touchHeight)
            .pointerInput(Unit) {
                cursorGestures(
                    onTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onInterrupt() // safety-critical: a single tap while buddy is busy stops it (§7)
                    },
                    onSummon = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onDoubleTap()
                    },
                    onLift = { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) },
                    onDragStart = onGrab,
                    onDrag = onDrag,
                    onRelease = onRelease
                )
            }
    ) {
        BuddyCursor(mood = mood, contentDescription = label)
    }
}
