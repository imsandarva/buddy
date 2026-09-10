package com.sandarva.kotlinapps.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ActivityEntry(val text: String, val atMillis: Long)

/**
 * A short, glanceable trail of the last few real requests — not a dashboard, just enough that a
 * task can be repeated with a tap instead of re-explaining it by voice every time (design spec §12).
 */
object ActivityLog {
    private const val PREFS = "buddy_activity"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 5

    private lateinit var prefs: SharedPreferences
    private val _entries = MutableStateFlow<List<ActivityEntry>>(emptyList())
    val entries: StateFlow<List<ActivityEntry>> = _entries.asStateFlow()

    fun ensure(app: Application) {
        if (::prefs.isInitialized) return
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _entries.value = readAll()
    }

    /** Only real, meaningful requests belong here — never system chatter or empty text. */
    fun record(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || !::prefs.isInitialized) return
        val next = (listOf(ActivityEntry(trimmed, System.currentTimeMillis())) + _entries.value)
            .distinctBy { it.text.lowercase() }
            .take(MAX_ENTRIES)
        _entries.value = next
        writeAll(next)
    }

    fun clear() {
        if (!::prefs.isInitialized) return
        _entries.value = emptyList()
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    private fun readAll(): List<ActivityEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val text = obj.optString("text").ifBlank { return@mapNotNull null }
                ActivityEntry(text, obj.optLong("at"))
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun writeAll(list: List<ActivityEntry>) {
        val array = JSONArray()
        list.forEach { entry -> array.put(JSONObject().put("text", entry.text).put("at", entry.atMillis)) }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }
}
