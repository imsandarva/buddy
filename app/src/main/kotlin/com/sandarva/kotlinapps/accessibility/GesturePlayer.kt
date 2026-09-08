package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.Handler
import android.os.Looper
import android.view.ViewConfiguration
import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Bound to the live accessibility service. Builds strokes; does not move the overlay. */
class GesturePlayer(private val service: AccessibilityService) {
    private val main = Handler(Looper.getMainLooper())

    suspend fun tap(x: Float, y: Float): Boolean {
        val (sx, sy) = clamp(x, y)
        return play(GestureStrokes.tap(sx, sy, TAP_MS))
    }

    suspend fun hold(x: Float, y: Float): Boolean {
        val (sx, sy) = clamp(x, y)
        return play(GestureStrokes.hold(sx, sy, holdMs()))
    }

    suspend fun swipe(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val a = clamp(x0, y0)
        val b = clamp(x1, y1)
        return play(GestureStrokes.swipe(a.first, a.second, b.first, b.second, SWIPE_MS))
    }

    suspend fun drag(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val a = clamp(x0, y0)
        val b = clamp(x1, y1)
        return play(GestureStrokes.drag(a.first, a.second, b.first, b.second, holdMs(), DRAG_MS))
    }

    /** Controlled scroll: slower than a swipe and resting before lift, so nothing flies past. */
    suspend fun pan(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val a = clamp(x0, y0)
        val b = clamp(x1, y1)
        return play(GestureStrokes.pan(a.first, a.second, b.first, b.second, PAN_MS, PAN_REST_MS))
    }

    fun holdMs(): Long = ViewConfiguration.getLongPressTimeout().toLong() + HOLD_PAD_MS

    private suspend fun play(gesture: GestureDescription): Boolean = suspendCancellableCoroutine { cont ->
        val run = Runnable {
            val sent = service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    BuddyLog.d("Hands.gesture", "completed")
                    if (cont.isActive) cont.resume(true)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    BuddyLog.d("Hands.gesture", "cancelled")
                    if (cont.isActive) cont.resume(false)
                }
            }, main)
            if (!sent) {
                BuddyLog.d("Hands.gesture", "dispatch rejected")
                if (cont.isActive) cont.resume(false)
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) run.run() else main.post(run)
        cont.invokeOnCancellation { main.removeCallbacks(run) }
    }

    private fun clamp(x: Float, y: Float): Pair<Float, Float> {
        val m = service.resources.displayMetrics
        val maxX = (m.widthPixels - EDGE).toFloat().coerceAtLeast(EDGE)
        val maxY = (m.heightPixels - EDGE).toFloat().coerceAtLeast(EDGE)
        return x.coerceIn(EDGE, maxX) to y.coerceIn(EDGE, maxY)
    }

    companion object {
        private const val TAP_MS = 60L
        private const val SWIPE_MS = 460L
        private const val DRAG_MS = 520L
        const val PAN_MS = 620L
        private const val PAN_REST_MS = 110L
        private const val HOLD_PAD_MS = 140L
        private const val EDGE = 3f
    }
}
