package com.sandarva.kotlinapps.overlay

import com.sandarva.kotlinapps.brain.BrainPhase
import com.sandarva.kotlinapps.brain.BrainSession
import com.sandarva.kotlinapps.ui.cursor.CursorGestureKind
import com.sandarva.kotlinapps.ui.cursor.CursorMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What buddy's hand is doing on the host app right now, and what the person is doing to buddy. */
private data class BodySignals(
    val gesture: CursorGestureKind?,
    val traveling: Boolean,
    val targeting: Boolean,
    val userDragging: Boolean,
    val armedToDismiss: Boolean
)

/** What buddy's mind is doing right now. */
private data class MindSignals(val considering: Boolean, val paused: Boolean, val phase: BrainPhase, val amplitude: Float)

/**
 * Turns the raw facts in [CursorMoodSignals] and [BrainSession] into the single [CursorMood] the
 * cursor draws. This is the only place priority between overlapping signals is decided — every
 * other file just reports a fact or renders a mood, never both. See docs/cursor.md §5.
 */
object CursorMoodResolver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val body = combine(
        CursorMoodSignals.gesture,
        CursorMoodSignals.traveling,
        CursorMoodSignals.targeting,
        CursorMoodSignals.userDragging,
        CursorMoodSignals.armedToDismiss,
        ::BodySignals
    )

    private val mind = combine(
        CursorMoodSignals.considering,
        CursorMoodSignals.paused,
        BrainSession.phase,
        CursorMoodSignals.voiceAmplitude,
        ::MindSignals
    )

    val mood: StateFlow<CursorMood> = combine(body, mind, ::resolve).stateIn(scope, SharingStarted.Eagerly, CursorMood.Idle)

    private fun resolve(body: BodySignals, mind: MindSignals): CursorMood = when {
        // Safety and the person's own touch always win — never hidden behind a "thinking" glow.
        body.userDragging || body.gesture == CursorGestureKind.DRAG || body.gesture == CursorGestureKind.SWIPE || body.gesture == CursorGestureKind.SCROLL ->
            CursorMood.Dragging(body.armedToDismiss)
        mind.paused -> CursorMood.Paused
        body.gesture == CursorGestureKind.HOLD -> CursorMood.Holding(0f) // ring progress is animated locally, see ActionForm
        body.gesture == CursorGestureKind.TAP -> CursorMood.Acting
        body.targeting -> CursorMood.Targeting
        body.traveling -> CursorMood.Traveling
        mind.phase == BrainPhase.Listening || mind.phase == BrainPhase.Live -> CursorMood.Listening(mind.amplitude)
        mind.phase == BrainPhase.Thinking -> CursorMood.Thinking
        mind.considering -> CursorMood.Considering
        else -> CursorMood.Idle
    }
}
