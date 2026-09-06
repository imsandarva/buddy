package com.sandarva.kotlinapps.session

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Session owner for Buddy's on-screen presence. UI observes; this module decides when it appears. */
class BuddySessionViewModel : ViewModel() {
    private val _cursor = MutableStateFlow(BuddyCursorState())
    val cursor: StateFlow<BuddyCursorState> = _cursor.asStateFlow()

    fun startBuddy() {
        _cursor.update { if (it.visible) it else it.copy(visible = true, xFraction = 0.5f, yFraction = 0.5f) }
    }

    fun moveCursor(xFraction: Float, yFraction: Float) {
        _cursor.update { it.copy(xFraction = xFraction.coerceIn(0f, 1f), yFraction = yFraction.coerceIn(0f, 1f)) }
    }
}
