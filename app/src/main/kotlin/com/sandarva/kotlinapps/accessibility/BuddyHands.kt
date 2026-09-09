package com.sandarva.kotlinapps.accessibility

import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import com.sandarva.kotlinapps.overlay.CursorMoodSignals
import com.sandarva.kotlinapps.overlay.OverlayChrome
import com.sandarva.kotlinapps.ui.cursor.CursorGestureKind
import com.sandarva.kotlinapps.ui.theme.CursorMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Hands API. The brain asks for a tap, hold, swipe, scroll, or drag.
 * Every Buddy overlay must go pass-through so the stroke hits the app, not us.
 */
object BuddyHands {
    @Volatile private var player: GesturePlayer? = null
    @Volatile private var lists: NodeScroller? = null

    fun attach(next: GesturePlayer, scroller: NodeScroller) {
        player = next
        lists = scroller
    }

    fun detach(current: GesturePlayer, scroller: NodeScroller) {
        if (player === current) player = null
        if (lists === scroller) lists = null
    }

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
     * no list is named. Prefers the node’s own scroll action (TalkBack); falls back to one finger.
     */
    suspend fun scrollWithin(within: ScreenBounds?, direction: Direction, viewId: String? = null): Boolean {
        val screen = BuddyCursorController.screenPixels() ?: return false
        val box = within ?: ScreenBounds(
            (screen.first * 0.08f).toInt(), (screen.second * 0.16f).toInt(),
            (screen.first * 0.92f).toInt(), (screen.second * 0.86f).toInt()
        )
        val span = HandReach.scrollPan(box, direction)
        if (!land(span.x0, span.y0)) return false
        if (scrollByNode(direction, within, viewId, span)) return true
        return stroke(span.x0, span.y0, span.x1, span.y1, Kind.Scroll)
    }

    private suspend fun scrollByNode(direction: Direction, within: ScreenBounds?, viewId: String?, span: HandReach.Span): Boolean {
        val scroller = lists ?: return false
        CursorMoodSignals.setTargeting(false)
        CursorMoodSignals.setGesture(CursorGestureKind.SCROLL)
        return try {
            if (!scroller.scroll(direction, within, viewId)) return false
            follow(span.x1, span.y1, GesturePlayer.PAN_MS)
            true
        } finally {
            CursorMoodSignals.setGesture(null)
        }
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
            val ok = when (kind) {
                Kind.Tap -> hands.tap(x0, y0)
                Kind.Hold -> hands.hold(x0, y0)
                Kind.Swipe -> hands.swipe(x0, y0, x1, y1)
                Kind.Scroll -> hands.pan(x0, y0, x1, y1)
                Kind.Drag -> hands.drag(x0, y0, x1, y1)
            }
            // Moving the overlay while dispatchGesture is in flight cancels the finger on many OEMs.
            if (ok && kind.moves) follow(x1, y1, SETTLE_FOLLOW_MS)
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

    private enum class Kind(val moves: Boolean, val gesture: CursorGestureKind) {
        Tap(false, CursorGestureKind.TAP),
        Hold(false, CursorGestureKind.HOLD),
        Swipe(true, CursorGestureKind.SWIPE),
        Scroll(true, CursorGestureKind.SCROLL),
        Drag(true, CursorGestureKind.DRAG)
    }

    private const val FLIGHT_TIMEOUT_MS = 2400L
    private const val PASSTHROUGH_MS = 160L
    private const val SETTLE_FOLLOW_MS = 220L
}
