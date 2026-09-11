package com.sandarva.kotlinapps.brain.live

/**
 * Socket close reasons → what they hear. Quota and billing stay a type-instead path, not a crash.
 * A Live "leaked key" close is often Google rejecting the websocket, not the key itself — REST
 * must confirm before we tell them to replace it.
 */
object LiveFail {
    /** Live said the key is the problem — still confirm on REST before marking the store invalid. */
    fun isAuthError(reason: String): Boolean {
        val r = reason.lowercase()
        return "api key" in r || "api_key" in r || "leaked" in r || "unauthenticated" in r || "permission_denied" in r || "permission denied" in r
    }

    fun speak(reason: String): String {
        val r = reason.lowercase()
        return when {
            isAuthError(r) -> "This key isn’t accepted anymore. Add a new one in Settings and I’ll be right back."
            "quota" in r || "billing" in r || "resource exhausted" in r || "resource_exhausted" in r -> LIVE_UNAVAILABLE
            "timeout" in r || "setup" in r -> "I couldn’t start a live talk. Try again in a moment."
            else -> "The live talk ended. Double-tap me to try again."
        }
    }

    /** Live died; the same key still works for typed ask. */
    const val LIVE_UNAVAILABLE = "Live talk isn’t available right now. Type what you need, and I’ll still help."
}
