package com.sandarva.kotlinapps.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import com.sandarva.kotlinapps.cursor.flightDurationMs
import com.sandarva.kotlinapps.cursor.pointOnArc

/** One-at-a-time window flight. User grab should call cancel(). */
class CursorFlightAnimator(
    private val host: View,
    private val read: () -> Pair<Float, Float>,
    private val write: (Float, Float) -> Unit
) {
    private val easing = PathInterpolator(0.22f, 1f, 0.36f, 1f)
    private val linear = LinearInterpolator()
    private var animator: ValueAnimator? = null
    private var flightId = 0

    fun flyTo(x: Float, y: Float, onEnd: () -> Unit = {}) {
        val (x0, y0) = read()
        run(flightDurationMs(x0, y0, x, y), easing, onEnd) { from, t -> pointOnArc(t, from.first, from.second, x, y) }
    }

    /** Straight slide so the tip stays on a drag stroke. */
    fun slideTo(x: Float, y: Float, durationMs: Long, onEnd: () -> Unit = {}) {
        run(durationMs.coerceAtLeast(1L), linear, onEnd) { from, t ->
            from.first + (x - from.first) * t to from.second + (y - from.second) * t
        }
    }

    private fun run(durationMs: Long, interpolator: TimeInterpolator, onEnd: () -> Unit, at: (Pair<Float, Float>, Float) -> Pair<Float, Float>) {
        host.post {
            animator?.cancel()
            val id = ++flightId
            val from = read()
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = durationMs
                this.interpolator = interpolator
                addUpdateListener {
                    val (nx, ny) = at(from, it.animatedValue as Float)
                    write(nx, ny)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (animator === animation) animator = null
                        if (flightId == id) onEnd()
                    }
                })
            }
            animator = anim
            anim.start()
        }
    }

    fun cancel() {
        flightId += 1
        animator?.cancel()
        animator = null
    }
}
