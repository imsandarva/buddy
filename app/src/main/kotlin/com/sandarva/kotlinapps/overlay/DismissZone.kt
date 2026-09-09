package com.sandarva.kotlinapps.overlay

import kotlin.math.hypot

/**
 * Hit geometry for the drag-to-dismiss X. Visual size lives here too so the overlay window
 * and the Compose target cannot drift apart. Same idea as Messenger chat-heads / Android Bubbles:
 * a fixed circle near the bottom, armed by proximity, not by a full-width band.
 */
object DismissZone {
    const val REST_DP = 56f // standard FAB / chat-head dismiss scale
    const val ARMED_DP = 68f // modest bloom when the cursor is over it
    const val BOTTOM_GAP_DP = 18f
    const val HIT_RADIUS_DP = 38f
    const val MAGNET_RADIUS_DP = 52f
    const val GLOW_PAD_DP = 20f

    fun hits(tipX: Float, tipY: Float, centerX: Float, centerY: Float, density: Float): Boolean {
        val r = HIT_RADIUS_DP * density
        return hypot(tipX - centerX, tipY - centerY) <= r
    }

    /** Gentle suction toward the X once the cursor is close — eases the drop without stealing the finger. */
    fun magnetDelta(tipX: Float, tipY: Float, centerX: Float, centerY: Float, density: Float): Pair<Float, Float> {
        val dx = centerX - tipX
        val dy = centerY - tipY
        val dist = hypot(dx, dy)
        val magnet = MAGNET_RADIUS_DP * density
        if (dist < 1f || dist > magnet) return 0f to 0f
        val t = 1f - dist / magnet
        val pull = 0.2f * t * t
        return dx * pull to dy * pull
    }
}
