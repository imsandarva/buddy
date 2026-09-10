package com.sandarva.kotlinapps.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The handful of one-way doors in first-run: has the wordless intro been seen, and has the
 * activation funnel (first talk → capability reveal → permissions) been walked at
 * least once — either finished or honestly declined. Once true, that funnel never runs again;
 * a returning user always lands on the home screen, per the "respect a no" rule in the design spec.
 */
object OnboardingPrefs {
    private const val PREFS = "buddy_onboarding"
    private const val KEY_INTRO_SEEN = "intro_seen"
    private const val KEY_ACTIVATION_DONE = "activation_done"
    private const val KEY_SESSION_OPENS = "session_opens"

    private lateinit var prefs: SharedPreferences
    private val _introSeen = MutableStateFlow(false)
    val introSeen: StateFlow<Boolean> = _introSeen.asStateFlow()
    private val _activationDone = MutableStateFlow(false)
    val activationDone: StateFlow<Boolean> = _activationDone.asStateFlow()
    private val _sessionOpens = MutableStateFlow(0)
    val sessionOpens: StateFlow<Int> = _sessionOpens.asStateFlow()

    fun ensure(app: Application) {
        if (::prefs.isInitialized) return
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _introSeen.value = prefs.getBoolean(KEY_INTRO_SEEN, false)
        _activationDone.value = prefs.getBoolean(KEY_ACTIVATION_DONE, false)
        _sessionOpens.value = prefs.getInt(KEY_SESSION_OPENS, 0)
    }

    fun markIntroSeen() { set(KEY_INTRO_SEEN, true, _introSeen) }

    /** Finished the funnel, or honestly declined a step — either way, never ask again unhandled. */
    fun markActivationDone() { set(KEY_ACTIVATION_DONE, true, _activationDone) }

    /** Counts home screens actually reached — used only to fade out the first-run prompt chips. */
    fun noteHomeOpened() {
        if (!::prefs.isInitialized || _sessionOpens.value >= CHIP_SESSION_LIMIT) return
        val next = _sessionOpens.value + 1
        prefs.edit().putInt(KEY_SESSION_OPENS, next).apply()
        _sessionOpens.value = next
    }

    /** Full reset from Settings — the user asked buddy to forget everything and start over. */
    fun clear() {
        if (!::prefs.isInitialized) return
        prefs.edit().clear().apply()
        _introSeen.value = false
        _activationDone.value = false
        _sessionOpens.value = 0
    }

    const val CHIP_SESSION_LIMIT = 6

    private fun set(key: String, value: Boolean, flow: MutableStateFlow<Boolean>) {
        if (!::prefs.isInitialized) return
        prefs.edit().putBoolean(key, value).apply()
        flow.value = value
    }
}
