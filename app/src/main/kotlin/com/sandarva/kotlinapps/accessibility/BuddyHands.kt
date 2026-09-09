package com.sandarva.kotlinapps.accessibility

import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import com.sandarva.kotlinapps.overlay.CursorMoodSignals
import com.sandarva.kotlinapps.overlay.OverlayChrome
import com.sandarva.kotlinapps.ui.cursor.CursorGestureKind
import com.sandarva.kotlinapps.ui.theme.CursorMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Hands API. The brain asks for a tap, hold, swipe, scroll, or drag.
 * Every Buddy overlay must go pass-through so the stroke hits the app, not us.
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

    /** Page pull in the direction the finger moves — launcher pages, carousels, dismiss. */
    suspend fun swipePage(direction: Direction): Boolean = swipeHere(direction.dx * HandReach.DRAG, direction.dy * HandReach.DRAG)

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

    /**
     * Reveal more content in [direction] inside [within] (a list) — or the middle of the screen when
     * no list is named. The finger moves the opposite way, about 40% of the container, then rests.
     */
    suspend fun scrollWithin(within: ScreenBounds?, direction: Direction): Boolean {
        val screen = BuddyCursorController.screenPixels() ?: return false
        val box = within ?: ScreenBounds((screen.first * 0.08f).toInt(), (screen.second * 0.16f).toInt(), (screen.first * 0.92f).toInt(), (screen.second * 0.86f).toInt())
        val span = HandReach.scrollPan(box, direction)
        if (!land(span.x0, span.y0)) return false
        return stroke(span.x0, span.y0, span.x1, span.y1, Kind.Scroll)
    }

    private suspend fun slideHere(dxNorm: Float, dyNorm: Float, kind: Kind): Boolean {
        val screen = BuddyCursorController.screenPixels() ?: return false
        val tip = BuddyCursorController.tipPixels()
        return if (kind == Kind.Swipe) {
            val span = HandReach.pageSwipe(screen.first, screen.second, dxNorm, dyNorm, tip)
            if (!land(span.x0, span.y0)) return false
            stroke(span.x0, span.y0, span.x1, span.y1, kind)
        } else {
            val (x0, y0) = tip ?: return false
            val x1 = (x0 + screen.first * dxNorm).coerceIn(8f, screen.first - 8f)
            val y1 = (y0 + screen.second * dyNorm).coerceIn(8f, screen.second - 8f)
            stroke(x0, y0, x1, y1, kind)
        }
    }

    private suspend fun atTip(block: suspend (Float, Float) -> Boolean): Boolean {
        val tip = BuddyCursorController.tipPixels() ?: return false
        return block(tip.first, tip.second)
    }

    /** Lands, then holds the anticipation beat (docs/cursor.md §6 — Targeting) before any stroke fires. */
    private suspend fun land(x: Float, y: Float): Boolean {
        val ok = withTimeoutOrNull(FLIGHT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val started = BuddyCursorController.animateToPixels(x, y) { if (cont.isActive) cont.resume(true) }
                if (!started && cont.isActive) cont.resume(false)
            }
        } ?: false
        if (ok) {
            CursorMoodSignals.setTargeting(true)
            delay(CursorMotion.TARGETING_SETTLE_MS)
        }
        return ok
    }

    private suspend fun stroke(x0: Float, y0: Float, x1: Float, y1: Float, kind: Kind): Boolean {
        val hands = player
        if (hands == null) {
            BuddyLog.d("Hands.stroke", "no player kind=$kind")
            return false
        }
        BuddyLog.d("Hands.stroke", "kind=$kind from=$x0,$y0 to=$x1,$y1")
        CursorMoodSignals.setTargeting(false)
        CursorMoodSignals.setGesture(kind.gesture)
        OverlayChrome.setPassthrough(true)
        return try {
            delay(PASSTHROUGH_MS)
            val ok = supervisorScope {
                if (kind.moves) {
                    launch {
                        if (kind == Kind.Drag) delay(hands.holdMs())
                        follow(x1, y1, kind.followMs)
                    }
                }
                when (kind) {
                    Kind.Tap -> hands.tap(x0, y0)
                    Kind.Hold -> hands.hold(x0, y0)
                    Kind.Swipe -> hands.swipe(x0, y0, x1, y1)
                    Kind.Scroll -> hands.pan(x0, y0, x1, y1)
                    Kind.Drag -> hands.drag(x0, y0, x1, y1)
                }
            }
            BuddyLog.d("Hands.stroke", "kind=$kind ok=$ok")
            ok
        } finally {
            OverlayChrome.setPassthrough(false)
            CursorMoodSignals.setGesture(null)
        }
    }

    private suspend fun follow(x: Float, y: Float, durationMs: Long) {
        suspendCancellableCoroutine { cont ->
            val started = BuddyCursorController.slideToPixels(x, y, durationMs) { if (cont.isActive) cont.resume(Unit) }
            if (!started && cont.isActive) cont.resume(Unit)
        }
    }

    /** How the cursor keeps up with the finger: swipes and drags are linear follows of the stroke. */
    private enum class Kind(val moves: Boolean, val followMs: Long, val gesture: CursorGestureKind) {
        Tap(false, 0L, CursorGestureKind.TAP),
        Hold(false, 0L, CursorGestureKind.HOLD),
        Swipe(true, SWIPE_FOLLOW_MS, CursorGestureKind.SWIPE),
        Scroll(true, GesturePlayer.PAN_MS, CursorGestureKind.SCROLL),
        Drag(true, DRAG_FOLLOW_MS, CursorGestureKind.DRAG)
    }

    private const val FLIGHT_TIMEOUT_MS = 2400L
    private const val PASSTHROUGH_MS = 48L
    private const val SWIPE_FOLLOW_MS = 460L
    private const val DRAG_FOLLOW_MS = 520L
}
