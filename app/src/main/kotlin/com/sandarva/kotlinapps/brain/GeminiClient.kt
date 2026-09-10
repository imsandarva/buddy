package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin Gemini Developer API client (`generateContent`). Knows HTTP and JSON, nothing about cursors.
 * Takes the key as a supplier, not a fixed value — the user's own key can be replaced at any time
 * from Settings, and every long-lived caller (the runner, the live search) must see the new one
 * on its very next request rather than the one that existed when it was built.
 */
class GeminiClient(
    private val apiKey: () -> String,
    private val http: OkHttpClient = defaultHttp()
) {
    constructor(fixedKey: String, http: OkHttpClient = defaultHttp()) : this({ fixedKey }, http)

    class ApiException(val code: Int, val body: String) : IllegalStateException("gemini $code ${body.take(180)}") {
        /** 401/403 means the key itself is the problem, not a transient server hiccup. */
        val isAuthError: Boolean get() = code == 401 || code == 403
    }

    suspend fun generate(model: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val payload = body.toString()
        BuddyLog.d("Gemini.post", "model=$model chars=${payload.length}")
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .addHeader("x-goog-api-key", apiKey())
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(JSON))
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            BuddyLog.d("Gemini.response", "model=$model code=${response.code} body=${text.take(600)}")
            if (!response.isSuccessful) throw ApiException(response.code, text)
            JSONObject(text)
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /** The model's answer text — thought parts skipped, several text parts joined. */
        fun firstText(response: JSONObject): String? {
            val parts = response.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: return null
            val out = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.optBoolean("thought")) continue
                out.append(part.optString("text"))
            }
            return out.toString().trim().ifBlank { null }
        }

        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
