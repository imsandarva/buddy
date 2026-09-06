package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Thin Gemini Developer API client. Tools only — no Live, no Computer Use. */
class GeminiClient(
    private val apiKey: String,
    private val http: OkHttpClient = defaultHttp()
) {
    suspend fun guide(question: String, catalog: String): GuidancePlan = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (model in MODELS) {
            try {
                BuddyLog.d("Gemini.post", "model=$model catalogChars=${catalog.length}")
                return@withContext parse(post(model, question, catalog))
            } catch (error: Exception) {
                BuddyLog.e("Gemini.postFail", "model=$model ${error.message}", error)
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("empty")
    }

    private fun post(model: String, question: String, catalog: String): String {
        val body = requestJson(question, catalog).toRequestBody(JSON)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            BuddyLog.d("Gemini.response", "model=$model code=${response.code} body=${text.take(400)}")
            if (!response.isSuccessful) throw IllegalStateException("gemini ${response.code} ${text.take(180)}")
            return text
        }
    }

    private fun requestJson(question: String, catalog: String): String {
        val say = fn("say", "Speak a short warm instruction. Never include coordinates or ids.", "text", "Words to say out loud.")
        val point = fn("point_to", "Point the buddy at a visible control from the list.", "element_id", "Exact element_id from the on-screen list.")
        return JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", GuidancePrompt.SYSTEM))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", GuidancePrompt.userMessage(question, catalog))))))
            .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", JSONArray().put(say).put(point))))
            .toString()
    }

    private fun fn(name: String, description: String, arg: String, argHelp: String): JSONObject {
        val props = JSONObject().put(arg, JSONObject().put("type", "STRING").put("description", argHelp))
        val params = JSONObject().put("type", "OBJECT").put("properties", props).put("required", JSONArray().put(arg))
        return JSONObject().put("name", name).put("description", description).put("parameters", params)
    }

    private fun parse(raw: String): GuidancePlan {
        val parts = JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            ?: return GuidancePlan(null, null)
        var say: String? = null
        var elementId: String? = null
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val call = part.optJSONObject("functionCall")
            if (call != null) {
                val args = call.optJSONObject("args") ?: JSONObject()
                when (call.optString("name")) {
                    "say" -> say = args.optString("text").ifBlank { null }
                    "point_to" -> elementId = args.optString("element_id").ifBlank { null }
                }
            } else {
                val text = part.optString("text").trim()
                if (text.isNotBlank() && say == null) say = text
            }
        }
        return GuidancePlan(say, elementId)
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val MODELS = listOf("gemini-3.6-flash", "gemini-3.5-flash-lite")

        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
