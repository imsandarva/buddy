package com.sandarva.kotlinapps.brain.live

import android.util.Base64
import com.sandarva.kotlinapps.brain.GeminiTools
import com.sandarva.kotlinapps.brain.GuidancePrompt
import org.json.JSONArray
import org.json.JSONObject

data class LiveFunctionCall(val id: String, val name: String, val args: JSONObject)

/** JSON for BidiGenerateContent. The socket stays unaware of cursor meaning. */
object LiveMessages {
    fun setup(): String = JSONObject()
        .put("setup", JSONObject()
            .put("model", "models/${LiveConfig.MODEL}")
            .put("generationConfig", JSONObject()
                .put("responseModalities", JSONArray().put("AUDIO"))
                .put("speechConfig", JSONObject().put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", LiveConfig.VOICE))))
                .put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal")))
            .put("realtimeInputConfig", JSONObject().put("automaticActivityDetection", vad()))
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", GuidancePrompt.LIVE))))
            .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", GeminiTools.functionDeclarations(includeSay = false, includeRunGoal = true)))))
        .toString()

    fun audio(pcm: ByteArray): String = JSONObject()
        .put("realtimeInput", JSONObject().put("audio", blob(pcm, "${LiveConfig.PCM};rate=${LiveConfig.IN_HZ}")))
        .toString()

    /** realtimeInput text is the Live path for mid-session context — clientContent holds the turn open. */
    fun catalog(text: String): String = JSONObject()
        .put("realtimeInput", JSONObject().put("text", "SCREEN:\n$text"))
        .toString()

    fun toolResponse(id: String, name: String, result: String, screen: String): String = JSONObject()
        .put("toolResponse", JSONObject().put("functionResponses", JSONArray().put(
            JSONObject()
                .put("id", id)
                .put("name", name)
                .put("response", JSONObject().put("result", result).put("screen", screen))
        )))
        .toString()

    fun parseAudio(parts: JSONArray?): List<ByteArray> {
        if (parts == null) return emptyList()
        val out = ArrayList<ByteArray>(2)
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val blob = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data") ?: continue
            val mime = blob.optString("mimeType").ifBlank { blob.optString("mime_type") }
            if (!mime.contains("audio", ignoreCase = true)) continue
            val data = blob.optString("data")
            if (data.isNotBlank()) out += Base64.decode(data, Base64.DEFAULT)
        }
        return out
    }

    fun parseToolCalls(raw: JSONObject): List<LiveFunctionCall> {
        val calls = raw.optJSONObject("toolCall")?.optJSONArray("functionCalls")
            ?: raw.optJSONObject("tool_call")?.optJSONArray("function_calls")
            ?: raw.optJSONObject("toolCall")?.optJSONArray("function_calls")
            ?: return emptyList()
        val out = ArrayList<LiveFunctionCall>(calls.length())
        for (i in 0 until calls.length()) {
            val fc = calls.optJSONObject(i) ?: continue
            out += LiveFunctionCall(fc.optString("id"), fc.optString("name"), fc.optJSONObject("args") ?: JSONObject())
        }
        return out
    }

    private fun blob(pcm: ByteArray, mime: String) = JSONObject()
        .put("mimeType", mime)
        .put("data", Base64.encodeToString(pcm, Base64.NO_WRAP))

    private fun vad() = JSONObject()
        .put("disabled", false)
        .put("startOfSpeechSensitivity", "START_SENSITIVITY_HIGH")
        .put("endOfSpeechSensitivity", "END_SENSITIVITY_HIGH")
        .put("prefixPaddingMs", LiveConfig.VAD_PREFIX_MS)
        .put("silenceDurationMs", LiveConfig.VAD_SILENCE_MS)
}
