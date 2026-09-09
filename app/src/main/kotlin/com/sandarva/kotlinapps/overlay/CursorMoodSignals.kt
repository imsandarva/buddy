package com.sandarva.kotlinapps.overlay

import com.sandarva.kotlinapps.ui.cursor.CursorGestureKind
import com.sandarva.kotlinapps.ui.theme.CursorMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Raw, low-level truths about what buddycursor's body is doing right now — one flow per fact,
 * written by whichever module owns that fact (hands, flight, the drag handle). Nobody downstream
 * decides *meaning* here; [CursorMoodResolver] turns these facts into the one [CursorMood] to draw.
 */
object CursorMoodSignals {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** A real touch/hold/swipe/drag/scroll buddy's hand is dispatching on the host app right now. */
    private val _gesture = MutableStateFlow<CursorGestureKind?>(null)
    val gesture: StateFlow<CursorGestureKind?> = _gesture.asStateFlow()

    /** The window is mid-flight between two points (`animateToPixels` / `slideToPixels`). */
    private val _traveling = MutableStateFlow(false)
    val traveling: StateFlow<Boolean> = _traveling.asStateFlow()

    /** Landed on a target and settling for the anticipation beat, just before a stroke fires. */
    private val _targeting = MutableStateFlow(false)
    val targeting: StateFlow<Boolean> = _targeting.asStateFlow()

    /** The person is holding and dragging the cursor itself to reposition or dismiss it. */
    private val _userDragging = MutableStateFlow(false)
    val userDragging: StateFlow<Boolean> = _userDragging.asStateFlow()

    /** True once a user-drag has crossed into the bottom release zone — releasing now stops Buddy. */
    private val _armedToDismiss = MutableStateFlow(false)
    val armedToDismiss: StateFlow<Boolean> = _armedToDismiss.asStateFlow()

    /** The agent is silently deciding its next on-screen move (no talk involved). */
    private val _considering = MutableStateFlow(false)
    val considering: StateFlow<Boolean> = _considering.asStateFlow()

    /** Just interrupted by a single tap — held briefly so "stopped" reads as a deliberate beat. */
    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    /** 0..1 loudness of whichever real audio is live — the person's mic, or buddy's own speech. */
    private val _voiceAmplitude = MutableStateFlow(0f)
    val voiceAmplitude: StateFlow<Float> = _voiceAmplitude.asStateFlow()

    /** Frame-to-frame window motion, in dp/frame, decayed by the UI layer — drives the comet trail. */
    private val _velocity = MutableStateFlow(0f to 0f)
    val velocity: StateFlow<Pair<Float, Float>> = _velocity.asStateFlow()

    /** One-shot: buddy couldn't find what it was looking for. A brief, honest wobble, then it moves on. */
    private val _uncertainPulses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val uncertainPulses: SharedFlow<Unit> = _uncertainPulses

    fun setGesture(kind: CursorGestureKind?) { _gesture.value = kind }
    fun setTraveling(value: Boolean) { _traveling.value = value }
    fun setTargeting(value: Boolean) { _targeting.value = value }
    fun setUserDragging(value: Boolean) { _userDragging.value = value; if (!value) _armedToDismiss.value = false }
    fun setArmedToDismiss(value: Boolean) { _armedToDismiss.value = value }
    fun setConsidering(value: Boolean) { _considering.value = value }
    fun setVoiceAmplitude(value: Float) { _voiceAmplitude.value = value.coerceIn(0f, 1f) }
    fun setVelocity(dx: Float, dy: Float) { _velocity.value = dx to dy }
    fun pulseUncertain() { _uncertainPulses.tryEmit(Unit) }

    /** Hold the stop acknowledgement on screen for a beat, then let the resolver fall back to idle. */
    fun pulsePaused() {
        _paused.value = true
        scope.launch { delay(CursorMotion.PAUSED_HOLD_MS); _paused.value = false }
    }

    /** Clears every transient signal — called when the overlay window is torn down. */
    fun reset() {
        _gesture.value = null
        _traveling.value = false
        _targeting.value = false
        _userDragging.value = false
        _armedToDismiss.value = false
        _considering.value = false
        _paused.value = false
        _voiceAmplitude.value = 0f
        _velocity.value = 0f to 0f
    }
}
