package com.sandarva.kotlinapps.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where buddy's brain lives — the user's own Gemini key, on this device only. Never bundled at
 * build time, never sent anywhere but straight to Google's API. Backed by a private, app-scoped
 * SharedPreferences file (same sandboxing every Android app's own settings rely on); good enough
 * for a personal key on a personal device, with none of EncryptedSharedPreferences' known KeyStore
 * flakiness on real hardware.
 */
object ApiKeyStore {
    private const val PREFS = "buddy_key_store"
    private const val KEY_VALUE = "gemini_api_key"

    private lateinit var prefs: SharedPreferences
    private val _hasKey = MutableStateFlow(false)
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    /** Set once a live call actually fails with an auth error — surfaced as a calm recovery prompt. */
    private val _invalid = MutableStateFlow(false)
    val invalid: StateFlow<Boolean> = _invalid.asStateFlow()

    fun ensure(app: Application) {
        if (::prefs.isInitialized) return
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _hasKey.value = currentKey.isNotBlank()
    }

    /** Read fresh every time — callers must never cache this past a screen the user could edit it on. */
    val currentKey: String get() = if (::prefs.isInitialized) prefs.getString(KEY_VALUE, "").orEmpty() else ""

    fun save(key: String) {
        val trimmed = key.trim()
        if (trimmed.isBlank() || !::prefs.isInitialized) return
        prefs.edit().putString(KEY_VALUE, trimmed).apply()
        _invalid.value = false
        _hasKey.value = true
        BuddyLog.d("ApiKeyStore.save", "chars=${trimmed.length}")
    }

    fun clear() {
        if (!::prefs.isInitialized) return
        prefs.edit().remove(KEY_VALUE).apply()
        _invalid.value = false
        _hasKey.value = false
        BuddyLog.d("ApiKeyStore.clear", "done")
    }

    /** A real call just came back 401/403 — the key that used to work no longer does. */
    fun markInvalid() {
        if (currentKey.isBlank()) return
        BuddyLog.d("ApiKeyStore.markInvalid", "flagged")
        _invalid.value = true
    }

    fun clearInvalid() { _invalid.value = false }

    /** The last few characters only — enough to recognize the key without ever showing it whole. */
    fun masked(): String {
        val key = currentKey
        if (key.length <= 4) return "••••"
        return "•••• •••• " + key.takeLast(4)
    }
}
