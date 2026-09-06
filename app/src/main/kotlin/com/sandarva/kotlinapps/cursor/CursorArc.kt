package com.sandarva.kotlinapps.cursor

import kotlin.math.hypot

/** Quadratic arc from start → end. t is 0..1 after easing. */
fun pointOnArc(t: Float, x0: Float, y0: Float, x1: Float, y1: Float): Pair<Float, Float> {
    val dx = x1 - x0
    val dy = y1 - y0
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val lift = (len * 0.22f).coerceIn(48f, 180f)
    val cx = (x0 + x1) * 0.5f - dy / len * lift
    val cy = (y0 + y1) * 0.5f + dx / len * lift
    val u = 1f - t
    return (u * u * x0 + 2f * u * t * cx + t * t * x1) to (u * u * y0 + 2f * u * t * cy + t * t * y1)
}

fun flightDurationMs(x0: Float, y0: Float, x1: Float, y1: Float): Long =
    (280f + hypot(x1 - x0, y1 - y0) * 0.42f).toLong().coerceIn(280L, 720L)
