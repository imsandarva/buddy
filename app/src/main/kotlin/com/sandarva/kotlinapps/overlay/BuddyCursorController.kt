package com.sandarva.kotlinapps.overlay

/** Window API. The brain flies or nudges through this; it never owns the overlay. */
interface BuddyCursorMover {
    fun animateToPixels(xPx: Float, yPx: Float, onLanded: (() -> Unit)? = null)
    fun animateToNormalized(x: Float, y: Float)
    fun animateToGrid999(x: Int, y: Int)
    fun nudgeNormalized(dx: Float, dy: Float)
    fun slideToPixels(xPx: Float, yPx: Float, durationMs: Long, onLanded: (() -> Unit)? = null)
    fun playPath(normalized: List<Pair<Float, Float>>)
    fun cancelFlight()
    fun tipPixels(): Pair<Float, Float>
    fun screenPixels(): Pair<Int, Int>
    fun setPassthrough(on: Boolean)
}

object BuddyCursorController {
    @Volatile
    private var mover: BuddyCursorMover? = null

    fun attach(next: BuddyCursorMover) { mover = next }
    fun detach(current: BuddyCursorMover) { if (mover === current) mover = null }
    fun isAttached(): Boolean = mover != null

    fun animateToPixels(xPx: Float, yPx: Float, onLanded: (() -> Unit)? = null): Boolean {
        val next = mover
        if (next == null) return false
        next.animateToPixels(xPx, yPx, onLanded)
        return true
    }

    fun slideToPixels(xPx: Float, yPx: Float, durationMs: Long, onLanded: (() -> Unit)? = null): Boolean {
        val next = mover
        if (next == null) return false
        next.slideToPixels(xPx, yPx, durationMs, onLanded)
        return true
    }

    fun tipPixels(): Pair<Float, Float>? = mover?.tipPixels()
    fun screenPixels(): Pair<Int, Int>? = mover?.screenPixels()
    fun setPassthrough(on: Boolean) { mover?.setPassthrough(on) }

    fun animateToNormalized(x: Float, y: Float): Boolean {
        val next = mover
        if (next == null) return false
        next.animateToNormalized(x, y)
        return true
    }

    fun animateToGrid999(x: Int, y: Int): Boolean {
        val next = mover
        if (next == null) return false
        next.animateToGrid999(x, y)
        return true
    }

    fun nudgeNormalized(dx: Float, dy: Float): Boolean {
        val next = mover
        if (next == null) return false
        next.nudgeNormalized(dx, dy)
        return true
    }

    fun playDemo() { mover?.playPath(DEMO_PATH) }
    fun cancelFlight() { mover?.cancelFlight() }

    /** Short hello path so we can prove the string before any model is wired. */
    val DEMO_PATH = listOf(0.72f to 0.22f, 0.22f to 0.46f, 0.78f to 0.64f, 0.50f to 0.40f)
}
