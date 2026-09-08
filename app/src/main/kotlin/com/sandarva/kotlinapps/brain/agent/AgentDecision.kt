package com.sandarva.kotlinapps.brain.agent

import org.json.JSONObject

/** How a run ended, in the model's words. */
data class AgentFinish(val succeeded: Boolean, val message: String)

/**
 * One turn of the model's mind: a short thought, an updated progress note (its memory),
 * an optional line to say out loud, the one action to take, and — when the job is over — how it ended.
 */
data class AgentDecision(
    val thought: String,
    val progress: String,
    val say: String?,
    val action: AgentAction,
    val finish: AgentFinish?
) {
    companion object {
        /** Parses the structured JSON the model returned; a broken reply becomes a no-op the runner can hint on. */
        fun parse(raw: String): AgentDecision {
            val json = try { JSONObject(raw) } catch (_: Exception) { return invalid("reply was not valid JSON") }
            val done = json.optJSONObject("done")
            val finish = done?.takeIf { it.has("status") }?.let {
                AgentFinish(succeeded = it.optString("status") == "done", message = it.optString("message").trim())
            }
            return AgentDecision(
                thought = json.optString("thought").trim().take(400),
                progress = json.optString("progress").trim().take(AgentMemory.PROGRESS_MAX),
                say = json.optString("say").trim().ifBlank { null },
                action = AgentAction.fromJson(json.optJSONObject("action")),
                finish = finish
            )
        }

        fun invalid(reason: String) = AgentDecision(reason, "", null, AgentAction.None, null)
    }
}
