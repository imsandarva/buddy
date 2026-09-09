package com.sandarva.kotlinapps.brain.agent

import com.sandarva.kotlinapps.brain.GeminiClient
import com.sandarva.kotlinapps.debug.BuddyLog
import org.json.JSONArray
import org.json.JSONObject

/** Which brain thinks this step. For now both tiers are 3.5 Flash Lite with high thinking. */
object AgentModels {
    class Tier(val id: String, val thinking: String)

    val FAST = Tier("gemini-3.5-flash-lite", "high")
    val STRONG = Tier("gemini-3.5-flash-lite", "high")
}

/**
 * One decision per call: system contract + step message → structured JSON → [AgentDecision].
 * Stateless by design (memory travels in the step message), so any tier can take any step.
 */
class AgentDecider(private val client: GeminiClient) {

    suspend fun decide(memory: AgentMemory, screen: String, hint: String?, deeper: Boolean): AgentDecision {
        val tier = if (deeper) AgentModels.STRONG else AgentModels.FAST
        val step = AgentPrompt.step(memory, screen, hint)
        BuddyLog.d("Agent.decide", "tier=${tier.id} step=${memory.stepCount + 1} chars=${step.length} hint=${hint != null}")
        val response = try {
            client.generate(tier.id, request(tier, step, jsonSchema = true))
        } catch (error: GeminiClient.ApiException) {
            if (error.code != 400) throw error
            BuddyLog.d("Agent.decide", "schema rejected — retrying with OpenAPI schema")
            client.generate(tier.id, request(tier, step, jsonSchema = false))
        }
        val text = GeminiClient.firstText(response) ?: return AgentDecision.invalid("empty reply")
        return AgentDecision.parse(text).also {
            BuddyLog.d("Agent.decision", "action=${it.action.describe()} done=${it.finish?.succeeded} say=${it.say != null} thought=\"${it.thought.take(120)}\"")
        }
    }

    private fun request(tier: AgentModels.Tier, step: String, jsonSchema: Boolean): JSONObject {
        val generation = JSONObject()
            .put("responseMimeType", "application/json")
            .put("thinkingConfig", JSONObject().put("thinkingLevel", tier.thinking))
            .put("temperature", TEMPERATURE)
            .put("maxOutputTokens", MAX_OUTPUT_TOKENS)
        if (jsonSchema) generation.put("responseJsonSchema", AgentSchema.RESPONSE) else generation.put("responseSchema", AgentSchema.openApi())
        return JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", AgentPrompt.SYSTEM))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", step)))))
            .put("generationConfig", generation)
    }

    private companion object {
        const val TEMPERATURE = 0.2
        const val MAX_OUTPUT_TOKENS = 8192
    }
}
