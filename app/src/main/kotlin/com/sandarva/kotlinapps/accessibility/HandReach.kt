package com.sandarva.kotlinapps.accessibility

import kotlin.math.abs

/**
 * Voice Access-style reach. A launcher page needs a long cross-screen pull,
 * not a short flick from wherever the cursor happens to sit. A list scroll is a
 * measured pan inside that list so nothing is skipped.
 */
object HandReach {
    /** Drag distance as a fraction of the display. */
    const val DRAG = 0.55f

    data class Span(val x0: Float, val y0: Float, val x1: Float, val y1: Float)

    fun pageSwipe(screenW: Int, screenH: Int, dx: Float, dy: Float, tip: Pair<Float, Float>?): Span {
        val horizontal = abs(dx) >= abs(dy)
        return if (horizontal) {
            val y = (tip?.second ?: screenH * 0.48f).coerceIn(screenH * 0.18f, screenH * 0.68f)
            val start = if (dx < 0) FAR else NEAR
            val end = if (dx < 0) NEAR else FAR
            Span(screenW * start, y, screenW * end, y)
        } else {
            val x = (tip?.first ?: screenW * 0.50f).coerceIn(screenW * 0.14f, screenW * 0.86f)
            val start = if (dy < 0) FAR else NEAR
            val end = if (dy < 0) NEAR else FAR
            Span(x, screenH * start, x, screenH * end)
        }
    }

    /**
     * Reveal content in [content] direction inside [box]: the finger moves the other way across
     * about half the container, so each scroll shows the next page of the list.
     */
    fun scrollPan(box: ScreenBounds, content: Direction): Span {
        val finger = content.opposite
        return if (content.horizontal) {
            val y = box.centerY
            val x0 = box.left + box.width * (if (finger.dx < 0) PAN_FAR else PAN_NEAR)
            val x1 = box.left + box.width * (if (finger.dx < 0) PAN_NEAR else PAN_FAR)
            Span(x0, y, x1, y)
        } else {
            val x = box.centerX
            val y0 = box.top + box.height * (if (finger.dy < 0) PAN_FAR else PAN_NEAR)
            val y1 = box.top + box.height * (if (finger.dy < 0) PAN_NEAR else PAN_FAR)
            Span(x, y0, x, y1)
        }
    }

    private const val NEAR = 0.12f
    private const val FAR = 0.88f
    private const val PAN_NEAR = 0.22f
    private const val PAN_FAR = 0.78f
}
