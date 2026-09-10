package com.sandarva.kotlinapps.brain.live

/**
 * Socket close reasons → what they hear. Quota and billing stay a type-instead path, not a crash.
 */
object LiveFail {
    /** The key itself is rejected, not just a busy server — this is the one worth fixing in Settings. */
    fun isAuthError(reason: String): Boolean {
        val r = reason.lowercase()
        return "api key" in r || "api_key" in r || "unauthenticated" in r || "permission_denied" in r || "permission denied" in r
    }

    fun speak(reason: String): String {
        val r = reason.lowercase()
        return when {
            isAuthError(r) -> "Your key stopped working. Add a new one in Settings and I’ll be right back."
            "quota" in r || "billing" in r || "resource exhausted" in r || "resource_exhausted" in r ->
                "Live talk isn’t available right now. Type what you need, and I’ll still help."
            "timeout" in r || "setup" in r -> "I couldn’t start a live talk. Try again in a moment."
            else -> "The live talk ended. Double-tap me to try again."
        }
    }
}
