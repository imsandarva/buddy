package com.sandarva.kotlinapps.brain

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

    fun setPhase(value: BrainPhase) { _phase.value = value }
    fun setNote(value: String?) { _note.value = value }
    fun setAskOpen(value: Boolean) { _askOpen.value = value }
    fun reset() {
        _phase.value = BrainPhase.Idle
        _note.value = null
        _askOpen.value = false
    }
}
