package com.sandarva.kotlinapps.brain.live

import android.util.Base64
import com.sandarva.kotlinapps.data.Country
import org.json.JSONArray
import org.json.JSONObject

data class LiveFunctionCall(val id: String, val name: String, val args: JSONObject)

/** JSON for BidiGenerateContent. The socket stays unaware of cursor meaning. */
object LiveMessages {
    /** [language] is the country the user picked to talk in — see [LiveLanguage]. */
    fun setup(language: Country): String =
        envelope(LivePrompt.SYSTEM + LiveLanguage.instruction(language), tools = true, withVad = true, speechCode = LiveLanguage.speechCode(language.language))

    /** First-meeting hello — same Live voice, no tools, no mic, one spoken line then hang up. */
    fun setupHello(language: Country): String =
        envelope(MEET_SYSTEM + LiveLanguage.instruction(language), tools = false, withVad = false, speechCode = LiveLanguage.speechCode(language.language))

    /** One greeting turn. Tools are forbidden until they actually speak. */
    fun hello(language: Country): String = turn(HELLO + LiveLanguage.greetingNudge(language))

    /** Onboarding meet — speak once, then the session ends. */
    fun meet(language: Country): String = turn(MEET + LiveLanguage.greetingNudge(language))

    private fun turn(text: String): String = JSONObject()
        .put("clientContent", JSONObject()
            .put("turns", JSONArray().put(JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", text)))))
            .put("turnComplete", true))
        .toString()

    fun audio(pcm: ByteArray): String = JSONObject()
        .put("realtimeInput", JSONObject().put("audio", blob(pcm, "${LiveConfig.PCM};rate=${LiveConfig.IN_HZ}")))
        .toString()

    /** realtimeInput text is the Live path for mid-session context — clientContent holds the turn open. */
    fun catalog(text: String): String = JSONObject()
        .put("realtimeInput", JSONObject().put("text", "SCREEN NOW — this is the only screen. Ignore every earlier SCREEN. Not a request. Do not tap.\nSCREEN:\n$text"))
        .toString()

    fun toolResponse(id: String, name: String, result: String, screen: String): String = JSONObject()
        .put("toolResponse", JSONObject().put("functionResponses", JSONArray().put(
            JSONObject()
                .put("id", id)
                .put("name", name)
                .put("response", JSONObject().put("result", result).put("screen", screen))
        )))
        .toString()

    /** A closed user turn so the model speaks — same session, not a new talk. */
    fun jobAsk(question: String): String = turn(JOB_ASK + question.trim())
    fun jobDone(message: String): String = turn(JOB_DONE + message.trim())

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

    private const val HELLO = "The live talk just started. Greet them in one short, warm, casual line — like a friend who just sat down. Then wait. Do not call any tools. Do not tap, point, fly, nudge, type, swipe, drag, or run_goal. SCREEN is not a request. Talking is not a job."
    private const val MEET_SYSTEM = "You are Buddy, a warm friend on their phone. Speak with your own voice. One short hello, then stop."
    private const val MEET = "They just woke you for the first time. Greet them in one short, warm, casual line — like a friend who just sat down. Then stop. Do not wait. Do not ask a question. Do not mention screens, keys, or tools."
    private const val JOB_ASK = "JOB ASK — not a screen request. The runner needs this from them. Ask in one short warm line. When they answer, call answer_job with their words. Do not tap or run_goal.\n"
    private const val JOB_DONE = "JOB DONE — same talk as before. Tell them this in your own voice, then wait. Do not call tools.\n"

    private fun envelope(instruction: String, tools: Boolean, withVad: Boolean, speechCode: String?) = JSONObject()
        .put("setup", JSONObject()
            .put("model", "models/${LiveConfig.MODEL}")
            .put("generationConfig", generation(speechCode))
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruction))))
            .apply {
                if (withVad) put("realtimeInputConfig", JSONObject().put("automaticActivityDetection", vad()))
                if (tools) put("tools", JSONArray().put(JSONObject().put("functionDeclarations", LiveTools.declarations())))
            })
        .toString()

    /** [speechCode] is null when Live has no native voice for the chosen language — the system
     *  instruction still asks for it in plain words, just without this explicit hint. */
    private fun generation(speechCode: String?) = JSONObject()
        .put("responseModalities", JSONArray().put("AUDIO"))
        .put(
            "speechConfig", JSONObject()
                .put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", LiveConfig.VOICE)))
                .apply { if (speechCode != null) put("languageCode", speechCode) }
        )
        .put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal"))

    private fun vad() = JSONObject()
        .put("disabled", false)
        .put("startOfSpeechSensitivity", "START_SENSITIVITY_LOW")
        .put("endOfSpeechSensitivity", "END_SENSITIVITY_HIGH")
        .put("prefixPaddingMs", LiveConfig.VAD_PREFIX_MS)
        .put("silenceDurationMs", LiveConfig.VAD_SILENCE_MS)
}
