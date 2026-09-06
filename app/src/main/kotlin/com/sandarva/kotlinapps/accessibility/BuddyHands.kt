package com.sandarva.kotlinapps.accessibility

import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import com.sandarva.kotlinapps.overlay.OverlaySession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Hands API. The brain asks for a tap, hold, swipe, or drag.
 * The overlay must go pass-through so the stroke hits the app, not the buddy window.
 */
object BuddyHands {
    @Volatile private var player: GesturePlayer? = null

    fun attach(next: GesturePlayer) { player = next }
    fun detach(current: GesturePlayer) { if (player === current) player = null }
    fun isReady(): Boolean = player != null

    suspend fun tapHere(): Boolean = atTip { x, y -> stroke(x, y, x, y, kind = Kind.Tap) }
    suspend fun holdHere(): Boolean = atTip { x, y -> stroke(x, y, x, y, kind = Kind.Hold) }
    suspend fun tapAt(x: Float, y: Float): Boolean = land(x, y) && stroke(x, y, x, y, kind = Kind.Tap)
    suspend fun holdAt(x: Float, y: Float): Boolean = land(x, y) && stroke(x, y, x, y, kind = Kind.Hold)

    suspend fun swipeHere(dxNorm: Float, dyNorm: Float): Boolean = slideHere(dxNorm, dyNorm, Kind.Swipe)
    suspend fun dragHere(dxNorm: Float, dyNorm: Float): Boolean = slideHere(dxNorm, dyNorm, Kind.Drag)

    suspend fun swipeTo(x: Float, y: Float): Boolean = atTip { x0, y0 -> stroke(x0, y0, x, y, Kind.Swipe) }
    suspend fun dragTo(x: Float, y: Float): Boolean = atTip { x0, y0 -> stroke(x0, y0, x, y, Kind.Drag) }

    suspend fun swipeFromTo(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        if (!land(x0, y0)) return false
        return stroke(x0, y0, x1, y1, Kind.Swipe)
    }

    suspend fun dragFromTo(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        if (!land(x0, y0)) return false
        return stroke(x0, y0, x1, y1, Kind.Drag)
    }

    private suspend fun slideHere(dxNorm: Float, dyNorm: Float, kind: Kind): Boolean {
        val (x0, y0) = BuddyCursorController.tipPixels() ?: return false
        val screen = BuddyCursorController.screenPixels() ?: return false
        val x1 = (x0 + screen.first * dxNorm).coerceIn(8f, screen.first - 8f)
        val y1 = (y0 + screen.second * dyNorm).coerceIn(8f, screen.second - 8f)
        return stroke(x0, y0, x1, y1, kind)
    }

    private suspend fun atTip(block: suspend (Float, Float) -> Boolean): Boolean {
        val tip = BuddyCursorController.tipPixels() ?: return false
        return block(tip.first, tip.second)
    }

    private suspend fun land(x: Float, y: Float): Boolean {
        val ok = withTimeoutOrNull(FLIGHT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val started = BuddyCursorController.animateToPixels(x, y) { if (cont.isActive) cont.resume(true) }
                if (!started && cont.isActive) cont.resume(false)
            }
        } ?: false
        if (ok) delay(LAND_SETTLE_MS)
        return ok
    }

    private suspend fun stroke(x0: Float, y0: Float, x1: Float, y1: Float, kind: Kind): Boolean {
        val hands = player
        if (hands == null) {
            BuddyLog.d("Hands.stroke", "no player kind=$kind")
            return false
        }
        BuddyLog.d("Hands.stroke", "kind=$kind from=$x0,$y0 to=$x1,$y1")
        OverlaySession.setPressing(true)
        BuddyCursorController.setPassthrough(true)
        return try {
            delay(PASSTHROUGH_MS)
            val ok = supervisorScope {
                if (kind == Kind.Swipe || kind == Kind.Drag) {
                    launch {
                        if (kind == Kind.Drag) delay(hands.holdMs())
                        follow(x1, y1, if (kind == Kind.Drag) DRAG_FOLLOW_MS else SWIPE_FOLLOW_MS)
                    }
                }
                when (kind) {
                    Kind.Tap -> hands.tap(x0, y0)
                    Kind.Hold -> hands.hold(x0, y0)
                    Kind.Swipe -> hands.swipe(x0, y0, x1, y1)
                    Kind.Drag -> hands.drag(x0, y0, x1, y1)
                }
            }
            BuddyLog.d("Hands.stroke", "kind=$kind ok=$ok")
            ok
        } finally {
            BuddyCursorController.setPassthrough(false)
            OverlaySession.setPressing(false)
        }
    }

    private suspend fun follow(x: Float, y: Float, durationMs: Long) {
        suspendCancellableCoroutine { cont ->
            val started = BuddyCursorController.slideToPixels(x, y, durationMs) { if (cont.isActive) cont.resume(Unit) }
            if (!started && cont.isActive) cont.resume(Unit)
        }
    }

    private enum class Kind { Tap, Hold, Swipe, Drag }

    private const val LAND_SETTLE_MS = 40L
    private const val FLIGHT_TIMEOUT_MS = 2400L
    private const val PASSTHROUGH_MS = 48L
    private const val SWIPE_FOLLOW_MS = 280L
    private const val DRAG_FOLLOW_MS = 420L
}
