package com.sandarva.kotlinapps.brain.live

/**
 * Socket close reasons → what they hear. Quota and billing stay a type-instead path, not a crash.
 */
object LiveFail {
    fun speak(reason: String): String {
        val r = reason.lowercase()
        return when {
            "quota" in r || "billing" in r || "resource exhausted" in r || "resource_exhausted" in r ->
                "Live talk isn’t available right now. Type what you need, and I’ll still help."
            "timeout" in r || "setup" in r -> "I couldn’t start a live talk. Try again in a moment."
            else -> "The live talk ended. Double-tap me to try again."
        }
    }
}
