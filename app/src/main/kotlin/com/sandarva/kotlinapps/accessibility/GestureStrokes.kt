package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.GestureDescription
import android.graphics.Path

/**
 * Finger-like strokes for [android.accessibilityservice.AccessibilityService.dispatchGesture].
 * A 1px nudge makes a “stay” path legal on OEMs that reject zero-length strokes.
 */
object GestureStrokes {
    fun tap(x: Float, y: Float, durationMs: Long): GestureDescription =
        one(dwell(x, y), durationMs)

    fun hold(x: Float, y: Float, durationMs: Long): GestureDescription =
        one(dwell(x, y), durationMs)

    fun swipe(x0: Float, y0: Float, x1: Float, y1: Float, durationMs: Long): GestureDescription =
        one(line(x0, y0, x1, y1), durationMs)

    /** Long-press, then slide without lifting — home-screen rearrange, sliders, etc. */
    fun drag(x0: Float, y0: Float, x1: Float, y1: Float, holdMs: Long, moveMs: Long): GestureDescription {
        val press = GestureDescription.StrokeDescription(dwell(x0, y0), 0, holdMs, true)
        val slide = press.continueStroke(line(x0, y0, x1, y1), 0, moveMs, false)
        return GestureDescription.Builder().addStroke(press).addStroke(slide).build()
    }

    private fun one(path: Path, durationMs: Long): GestureDescription =
        GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(1L)))
            .build()

    private fun dwell(x: Float, y: Float) = Path().apply {
        moveTo(x, y)
        lineTo(x + 1f, y)
    }

    private fun line(x0: Float, y0: Float, x1: Float, y1: Float) = Path().apply {
        moveTo(x0, y0)
        lineTo(x1, y1)
    }
}
