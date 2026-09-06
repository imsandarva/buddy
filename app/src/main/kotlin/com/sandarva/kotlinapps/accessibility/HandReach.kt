package com.sandarva.kotlinapps.accessibility

/**
 * Voice Access-style reach. A launcher page needs a long cross-screen pull,
 * not a short flick from wherever the cursor happens to sit.
 */
object HandReach {
    /** Drag distance as a fraction of the display. */
    const val DRAG = 0.55f

    data class Span(val x0: Float, val y0: Float, val x1: Float, val y1: Float)

    fun pageSwipe(screenW: Int, screenH: Int, dx: Float, dy: Float, tip: Pair<Float, Float>?): Span {
        val horizontal = kotlin.math.abs(dx) >= kotlin.math.abs(dy)
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

    private const val NEAR = 0.12f
    private const val FAR = 0.88f
}
