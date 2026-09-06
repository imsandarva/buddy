package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BrainPhase { Idle, Listening, Thinking }

/** Process-wide brain UI. Hands and eyes stay unaware of Gemini. */
object BrainSession {
    private val _phase = MutableStateFlow(BrainPhase.Idle)
    val phase: StateFlow<BrainPhase> = _phase.asStateFlow()
    private val _note = MutableStateFlow<String?>(null)
    val note: StateFlow<String?> = _note.asStateFlow()
    private val _askOpen = MutableStateFlow(false)
    val askOpen: StateFlow<Boolean> = _askOpen.asStateFlow()

    fun setPhase(value: BrainPhase) {
        if (_phase.value != value) BuddyLog.d("BrainSession.phase", "${_phase.value} -> $value")
        _phase.value = value
    }
    fun setNote(value: String?) { _note.value = value }
    fun setAskOpen(value: Boolean) {
        if (_askOpen.value != value) BuddyLog.d("BrainSession.askOpen", "${_askOpen.value} -> $value")
        _askOpen.value = value
    }
    fun reset() {
        BuddyLog.d("BrainSession.reset", "wasOpen=${_askOpen.value} phase=${_phase.value}")
        _phase.value = BrainPhase.Idle
        _note.value = null
        _askOpen.value = false
    }
}
