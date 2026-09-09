package com.sandarva.kotlinapps.brain.agent

import com.sandarva.kotlinapps.brain.GeminiClient
import com.sandarva.kotlinapps.debug.BuddyLog
import org.json.JSONArray
import org.json.JSONObject

/**
 * One grounded Google Search via generateContent. Kept off the structured decision call because
 * `responseJsonSchema` silently drops `google_search` on this API — the runner searches, then thinks again.
 */
class WebSearch(private val client: GeminiClient) {

    suspend fun lookup(query: String): Outcome {
        val q = query.trim()
        if (q.isBlank()) return Outcome.fail("no search query")
        BuddyLog.d("Agent.search", "q=\"${q.take(80)}\"")
        return try {
            val text = GeminiClient.firstText(client.generate(AgentModels.FAST.id, request(q)))
                ?.trim().orEmpty()
            if (text.isBlank()) Outcome.fail("search found nothing useful")
            else Outcome.ok(text.take(RESULT_MAX)).also { BuddyLog.d("Agent.search", "ok chars=${it.detail.length}") }
        } catch (error: Exception) {
            BuddyLog.e("Agent.search", error.message ?: "unknown", error)
            Outcome.fail("search could not run")
        }
    }

    private fun request(query: String): JSONObject = JSONObject()
        .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", ASK + query)))))
        .put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
        .put("generationConfig", JSONObject()
            .put("temperature", 0.2)
            .put("maxOutputTokens", 1024)
            .put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal")))

    private companion object {
        const val RESULT_MAX = 900
        const val ASK = "Search the web. Reply with a short factual note a phone helper can act on — steps, menu names, numbers, or the answer. No filler.\n\n"
    }
}
