package com.sandarva.kotlinapps.overlay

/** Hands API. The brain (AI later) only calls this; it never owns the window. */
interface BuddyCursorMover {
    fun animateToPixels(xPx: Float, yPx: Float)
    fun animateToNormalized(x: Float, y: Float)
    fun animateToGrid999(x: Int, y: Int)
    fun playPath(normalized: List<Pair<Float, Float>>)
    fun cancelFlight()
}

object BuddyCursorController {
    @Volatile
    private var mover: BuddyCursorMover? = null

    fun attach(next: BuddyCursorMover) { mover = next }
    fun detach(current: BuddyCursorMover) { if (mover === current) mover = null }

    fun animateToPixels(xPx: Float, yPx: Float) { mover?.animateToPixels(xPx, yPx) }
    fun animateToNormalized(x: Float, y: Float) { mover?.animateToNormalized(x, y) }
    fun animateToGrid999(x: Int, y: Int) { mover?.animateToGrid999(x, y) }
    fun playDemo() { mover?.playPath(DEMO_PATH) }
    fun cancelFlight() { mover?.cancelFlight() }

    /** Short hello path so we can prove the string before any model is wired. */
    val DEMO_PATH = listOf(0.72f to 0.22f, 0.22f to 0.46f, 0.78f to 0.64f, 0.50f to 0.40f)
}
