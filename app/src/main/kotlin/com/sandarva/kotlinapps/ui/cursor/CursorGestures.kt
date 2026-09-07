package com.sandarva.kotlinapps.ui.cursor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import android.view.ViewConfiguration as AndroidViewConfiguration

/** Tap blooms, double-tap summons, hold lifts, drag drifts. */
internal suspend fun PointerInputScope.cursorGestures(
    onTap: () -> Unit,
    onSummon: () -> Unit,
    onLift: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onRelease: () -> Unit
) {
    var lastTapAt = 0L
    var lastTap = Offset.Unspecified
    val tapWindow = AndroidViewConfiguration.getDoubleTapTimeout().toLong()
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown()
        var dragged = false
        var lifted = false
        var travel = 0f
        val liftAt = down.uptimeMillis + 110L
        try {
            drag(down.id) { change ->
                val delta = change.positionChange()
                change.consume()
                travel += delta.getDistance()
                if (!lifted && change.uptimeMillis >= liftAt && travel < slop) {
                    lifted = true
                    onLift()
                }
                if (!dragged && travel >= slop) {
                    dragged = true
                    if (!lifted) { lifted = true; onLift() }
                    onDragStart()
                }
                if (dragged && delta != Offset.Zero) onDrag(delta.x, delta.y)
            }
        } finally {
            if (dragged) {
                onRelease()
                lastTapAt = 0L
            } else {
                val now = down.uptimeMillis
                val near = lastTap != Offset.Unspecified && (down.position - lastTap).getDistance() <= slop * 2.5f
                if (now - lastTapAt <= tapWindow && near) {
                    lastTapAt = 0L
                    onSummon()
                } else {
                    lastTapAt = now
                    lastTap = down.position
                    onTap()
                }
            }
        }
    }
}
