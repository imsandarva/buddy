package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Thin Gemini Developer API client. Tools only — no Live, no Computer Use. */
class GeminiClient(
    private val apiKey: String,
    private val http: OkHttpClient = defaultHttp()
) {
    suspend fun guide(question: String, catalog: String): GuidancePlan = withContext(Dispatchers.IO) {
        BuddyLog.d("Gemini.post", "model=$MODEL catalogChars=${catalog.length}")
        return@withContext GeminiTools.parse(post(MODEL, question, catalog))
    }

    private fun post(model: String, question: String, catalog: String): String {
        val body = GeminiTools.body(question, catalog).toRequestBody(JSON)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            BuddyLog.d("Gemini.response", "model=$model code=${response.code} body=${text.take(800)}")
            if (!response.isSuccessful) throw IllegalStateException("gemini ${response.code} ${text.take(180)}")
            return text
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val MODEL = "gemini-3.5-flash-lite"

        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
