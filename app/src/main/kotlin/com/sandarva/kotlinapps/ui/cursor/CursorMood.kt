package com.sandarva.kotlinapps.ui.cursor

/** One being, two shapes. See docs/cursor.md §1 — the shape itself is the signal. */
enum class CursorForm { Voice, Action }

/**
 * Every honest state buddycursor can be in. Exactly one is true at a time; [CursorMoodResolver]
 * (in `overlay/`) is the single place that decides which one wins when several signals overlap.
 */
sealed class CursorMood(val form: CursorForm) {
    /** Present but quiet — dimmed, parked, peripheral. */
    data object Idle : CursorMood(CursorForm.Action)

    /** Buddy is a mind: listening to the person, glow breathing and reacting to real sound. */
    data class Listening(val amplitude: Float) : CursorMood(CursorForm.Voice)

    /** Buddy is a mind composing a reply — a slow sheen turns inside the orb, no live amplitude. */
    data object Thinking : CursorMood(CursorForm.Voice)

    /** Buddy is deciding its next on-screen move, silently — the same sheen, at hand-scale. */
    data object Considering : CursorMood(CursorForm.Action)

    /** Buddy is a hand, gliding to its next stop with a trailing comet of light. */
    data object Traveling : CursorMood(CursorForm.Action)

    /** Landed, brightening, about to touch — the anticipation beat before contact. */
    data object Targeting : CursorMood(CursorForm.Action)

    /** Contact — a quick squash and a ripple outward from the exact point touched. */
    data object Acting : CursorMood(CursorForm.Action)

    /** Long-press in flight — a ring fills clockwise, so the wait has a visible end. */
    data class Holding(val progress: Float) : CursorMood(CursorForm.Action)

    /** A stroke is under way — buddy's own drag/swipe/scroll, or the person repositioning it. */
    data class Dragging(val armedToDismiss: Boolean = false) : CursorMood(CursorForm.Action)

    /** Just interrupted. Glow drops to a steady, unmoving low light — unmistakably not "thinking." */
    data object Paused : CursorMood(CursorForm.Action)
}

/** Buddy's own drag/swipe/scroll/tap/hold strokes on the host app — see `accessibility/BuddyHands.kt`. */
enum class CursorGestureKind { TAP, HOLD, SWIPE, SCROLL, DRAG }
