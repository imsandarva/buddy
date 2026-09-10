package com.sandarva.kotlinapps.data

import android.content.Context
import com.sandarva.kotlinapps.brain.GeminiClient
import com.sandarva.kotlinapps.brain.Reachability
import com.sandarva.kotlinapps.debug.BuddyLog
import org.json.JSONArray
import org.json.JSONObject

/** Turns a pasted key into a plain yes/no, in the same words a friend would use — never a code. */
object ApiKeyValidator {
    sealed class Result {
        object Valid : Result()
        data class Invalid(val reason: String) : Result()
    }

    suspend fun check(context: Context, key: String): Result {
        val trimmed = key.trim()
        if (trimmed.isBlank()) return Result.Invalid("Paste your key first.")
        if (!Reachability.online(context)) return Result.Invalid("No internet right now — connect and try again.")
        return try {
            val client = GeminiClient(trimmed)
            client.generate(MODEL, PING_BODY)
            BuddyLog.d("ApiKeyValidator.check", "ok")
            Result.Valid
        } catch (error: GeminiClient.ApiException) {
            BuddyLog.d("ApiKeyValidator.check", "rejected code=${error.code}")
            Result.Invalid(if (error.isAuthError) "That key didn’t work — double check it’s copied in full." else "Google couldn’t check that key just now — try again in a moment.")
        } catch (error: Exception) {
            BuddyLog.e("ApiKeyValidator.check", error.message ?: "failed", error)
            Result.Invalid("Couldn’t reach Google to check that — try again in a moment.")
        }
    }

    private const val MODEL = "gemini-3.5-flash-lite"

    /** Smallest possible real call — one word in, a couple tokens out. Just proves the key works. */
    private val PING_BODY = JSONObject()
        .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", "hi")))))
        .put("generationConfig", JSONObject().put("maxOutputTokens", 4).put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal")))
}
