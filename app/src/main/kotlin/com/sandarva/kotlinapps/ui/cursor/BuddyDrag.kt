package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange

/** Instant pickup, then 1:1 finger tracking until lift. */
internal suspend fun PointerInputScope.dragBuddyCursor(
    onGrab: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onRelease: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        onGrab()
        try {
            drag(down.id) { change ->
                val delta = change.positionChange()
                change.consume()
                if (delta != Offset.Zero) onDrag(delta.x, delta.y)
            }
        } finally {
            onRelease()
        }
    }
}
