package com.sandarva.kotlinapps.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one language Buddy talks live in — picked once as a country (see [CountryData]), which maps
 * straight to that country's language. Same on-device SharedPreferences pattern as [ApiKeyStore]
 * and [OnboardingPrefs]: nothing here ever leaves the phone except as plain words inside the Live
 * system instruction the user's own key already talks to.
 */
object LanguagePrefs {
    private const val PREFS = "buddy_language"
    private const val KEY_COUNTRY_CODE = "country_code"

    private lateinit var prefs: SharedPreferences
    private val _selectedCode = MutableStateFlow("")
    val selectedCode: StateFlow<String> = _selectedCode.asStateFlow()
    private val _hasSelected = MutableStateFlow(false)
    val hasSelected: StateFlow<Boolean> = _hasSelected.asStateFlow()

    fun ensure(app: Application) {
        if (::prefs.isInitialized) return
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_COUNTRY_CODE, "").orEmpty()
        _selectedCode.value = saved
        _hasSelected.value = saved.isNotBlank()
    }

    fun save(code: String) {
        val trimmed = code.trim().lowercase()
        if (trimmed.isBlank() || !::prefs.isInitialized) return
        prefs.edit().putString(KEY_COUNTRY_CODE, trimmed).apply()
        _selectedCode.value = trimmed
        _hasSelected.value = true
    }

    fun clear() {
        if (!::prefs.isInitialized) return
        prefs.edit().remove(KEY_COUNTRY_CODE).apply()
        _selectedCode.value = ""
        _hasSelected.value = false
    }

    /** The chosen country, or Buddy's English default before anyone has picked one. */
    fun current(): Country = CountryData.find(_selectedCode.value) ?: CountryData.DEFAULT
}
