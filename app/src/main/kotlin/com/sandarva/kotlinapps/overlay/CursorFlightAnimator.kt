package com.sandarva.kotlinapps.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
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
    private var animator: ValueAnimator? = null
    private var flightId = 0

    fun flyTo(x: Float, y: Float, onEnd: () -> Unit = {}) {
        host.post {
            animator?.cancel()
            val id = ++flightId
            val (x0, y0) = read()
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = flightDurationMs(x0, y0, x, y)
                interpolator = easing
                addUpdateListener {
                    val (nx, ny) = pointOnArc(it.animatedValue as Float, x0, y0, x, y)
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
