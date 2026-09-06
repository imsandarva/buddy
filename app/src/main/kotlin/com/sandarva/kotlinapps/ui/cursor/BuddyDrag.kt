package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import android.view.ViewConfiguration as AndroidViewConfiguration

/** Instant pickup after slop; a quick second tap asks Buddy. */
internal suspend fun PointerInputScope.dragBuddyCursor(
    onGrab: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onRelease: () -> Unit,
    onDoubleTap: () -> Unit
) {
    var lastTapAt = 0L
    var lastTap = Offset.Unspecified
    val tapWindow = AndroidViewConfiguration.getDoubleTapTimeout().toLong()
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown()
        var dragged = false
        var travel = 0f
        try {
            drag(down.id) { change ->
                val delta = change.positionChange()
                change.consume()
                travel += delta.getDistance()
                if (!dragged && travel >= slop) {
                    dragged = true
                    onGrab()
                }
                if (dragged && delta != Offset.Zero) onDrag(delta.x, delta.y)
            }
        } finally {
            if (dragged) {
                onRelease()
                lastTapAt = 0L
            } else {
                val now = down.uptimeMillis
                val near = lastTap != Offset.Unspecified && (down.position - lastTap).getDistance() <= slop * 2f
                if (now - lastTapAt <= tapWindow && near) {
                    lastTapAt = 0L
                    onDoubleTap()
                } else {
                    lastTapAt = now
                    lastTap = down.position
                }
            }
        }
    }
}
