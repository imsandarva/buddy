package com.sandarva.kotlinapps.overlay

import com.sandarva.kotlinapps.session.BuddyCursorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-wide overlay session. Survives the activity; dies if the user force-stops the app. */
object OverlaySession {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()
    private val _awaitingPermission = MutableStateFlow(false)
    val awaitingPermission: StateFlow<Boolean> = _awaitingPermission.asStateFlow()
    var placement: BuddyCursorState = BuddyCursorState(visible = true, xFraction = 0.5f, yFraction = 0.5f)
        private set

    fun setActive(value: Boolean) { _active.value = value }
    fun setAwaitingPermission(value: Boolean) { _awaitingPermission.value = value }
    fun savePlacement(xFraction: Float, yFraction: Float) {
        placement = BuddyCursorState(true, xFraction.coerceIn(0f, 1f), yFraction.coerceIn(0f, 1f))
    }
}
