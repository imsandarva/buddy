package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-wide eyes session. Bound means the service can snapshot; grant lives in system Settings. */
object AccessibilitySession {
    @Volatile var service: AccessibilityService? = null
        private set
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    private val _bound = MutableStateFlow(false)
    val bound: StateFlow<Boolean> = _bound.asStateFlow()
    private val _awaitingGrant = MutableStateFlow(false)
    val awaitingGrant: StateFlow<Boolean> = _awaitingGrant.asStateFlow()

    fun bind(host: AccessibilityService) {
        service = host
        setBound(true)
    }

    fun unbind(host: AccessibilityService) {
        if (service === host) service = null
        setBound(false)
    }

    fun setEnabled(value: Boolean) { _enabled.value = value }
    fun setBound(value: Boolean) {
        if (!value) service = null
        _bound.value = value
    }
    fun setAwaitingGrant(value: Boolean) { _awaitingGrant.value = value }
}
