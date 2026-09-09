package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.GestureDescription
import android.graphics.Path

/**
 * One-finger [GestureDescription]s for [android.accessibilityservice.AccessibilityService.dispatchGesture].
 *
 * Each stroke in a description is a separate pointer. A continued segment must not overlap the
 * previous one in time — startTime 0 on both looks like two fingers (Gallery pinch, lists ignore).
 * A 1px nudge makes a “stay” path legal on OEMs that reject zero-length strokes.
 */
object GestureStrokes {
    fun tap(x: Float, y: Float, durationMs: Long): GestureDescription =
        one(dwell(x, y), durationMs)

    fun hold(x: Float, y: Float, durationMs: Long): GestureDescription =
        one(dwell(x, y), durationMs)

    fun swipe(x0: Float, y0: Float, x1: Float, y1: Float, durationMs: Long): GestureDescription =
        one(line(x0, y0, x1, y1), durationMs)

    /** Long-press, then slide without lifting — home-screen rearrange, sliders, zoomed photos. */
    fun drag(x0: Float, y0: Float, x1: Float, y1: Float, holdMs: Long, moveMs: Long): GestureDescription {
        val press = GestureDescription.StrokeDescription(dwell(x0, y0), 0, holdMs, true)
        val fromX = x0 + STAY
        val slide = press.continueStroke(line(fromX, y0, x1, y1), holdMs, moveMs, false)
        return GestureDescription.Builder().addStroke(press).addStroke(slide).build()
    }

    /** One finger across the list. No second pointer, no overlapping rest stroke. */
    fun pan(x0: Float, y0: Float, x1: Float, y1: Float, moveMs: Long): GestureDescription =
        one(line(x0, y0, x1, y1), moveMs)

    private fun one(path: Path, durationMs: Long): GestureDescription =
        GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(1L)))
            .build()

    private fun dwell(x: Float, y: Float) = Path().apply {
        moveTo(x, y)
        lineTo(x + STAY, y)
    }

    private fun line(x0: Float, y0: Float, x1: Float, y1: Float) = Path().apply {
        moveTo(x0, y0)
        lineTo(x1, y1)
    }

    private const val STAY = 1f
}
