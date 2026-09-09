package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewConfiguration
import android.view.WindowManager
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

    /** One-finger pan — same motion a person uses to scroll a list or a gallery grid. */
    suspend fun pan(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val a = clamp(x0, y0)
        val b = clamp(x1, y1)
        return play(GestureStrokes.pan(a.first, a.second, b.first, b.second, PAN_MS))
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
        val (w, h) = screenSize()
        val maxX = (w - EDGE).coerceAtLeast(EDGE)
        val maxY = (h - EDGE).coerceAtLeast(EDGE)
        return x.coerceIn(EDGE, maxX) to y.coerceIn(EDGE, maxY)
    }

    /** Same coordinate space the overlay uses, so a landed tip is a legal stroke start. */
    private fun screenSize(): Pair<Float, Float> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = service.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
            bounds.width().toFloat() to bounds.height().toFloat()
        } else {
            val m = service.resources.displayMetrics
            m.widthPixels.toFloat() to m.heightPixels.toFloat()
        }
    }

    companion object {
        private const val TAP_MS = 60L
        private const val SWIPE_MS = 320L
        private const val DRAG_MS = 450L
        const val PAN_MS = 380L
        private const val HOLD_PAD_MS = 140L
        private const val EDGE = 8f
    }
}
