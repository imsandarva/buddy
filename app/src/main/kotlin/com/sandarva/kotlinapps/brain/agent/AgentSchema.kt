package com.sandarva.kotlinapps.brain.agent

import com.sandarva.kotlinapps.overlay.CursorLanding
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON Schema for one agent turn (Gemini structured output). Flat and enum-heavy on purpose:
 * the model fills a form, it does not free-write — that is what makes every step parseable.
 */
object AgentSchema {
    val RESPONSE: JSONObject by lazy {
        obj(
            props = mapOf(
                "thought" to str("What you see, what it means for the goal, and why this action. One or two short sentences."),
                "progress" to str("Your memory for the next step: what is done, what is left, anything learned. Rewrite fully each turn. Under 200 characters."),
                "say" to str("Optional. One short warm sentence to say out loud right now. Empty string when nothing needs saying."),
                "action" to obj(
                    props = mapOf(
                        "type" to enumStr("The one thing to do this turn.", AgentAction.TYPES),
                        "target" to str("Exact id from SCREEN for tap, long_press, type, scroll, point, and the start of drag. Copy it character for character."),
                        "to" to str("Exact id from SCREEN — where a drag ends."),
                        "text" to str("Words to type; the app name for open_app; the question for ask."),
                        "direction" to enumStr("For scroll: where more content is (down = see what is below). For swipe: where the finger moves.", listOf("up", "down", "left", "right")),
                        "submit" to bool("For type: true to press Search, Send, or Enter after typing."),
                        "place" to enumStr("For move_cursor only.", CursorLanding.PLACES)
                    ),
                    required = listOf("type")
                ),
                "done" to obj(
                    props = mapOf(
                        "status" to enumStr("done when the goal is reached; cannot when it is impossible or unsafe to continue.", listOf("done", "cannot")),
                        "message" to str("One or two warm sentences for the person. If they asked a question, this holds the answer you read on the screen.")
                    ),
                    required = listOf("status", "message"),
                    nullable = true
                )
            ),
            required = listOf("thought", "progress", "say", "action", "done")
        )
    }

    /** Same form in the older OpenAPI dialect (`STRING`, `nullable`) for endpoints that reject JSON Schema. */
    fun openApi(): JSONObject = convert(RESPONSE)

    private fun convert(schema: JSONObject): JSONObject {
        val out = JSONObject()
        val type = schema.opt("type")
        if (type is JSONArray) {
            out.put("type", type.optString(0).uppercase()).put("nullable", true)
        } else out.put("type", schema.optString("type").uppercase())
        schema.optString("description").takeIf { it.isNotBlank() }?.let { out.put("description", it) }
        schema.optJSONArray("enum")?.let { out.put("enum", it) }
        schema.optJSONArray("required")?.let { out.put("required", it) }
        schema.optJSONObject("properties")?.let { props ->
            val converted = JSONObject()
            props.keys().forEach { key -> converted.put(key, convert(props.getJSONObject(key))) }
            out.put("properties", converted)
        }
        return out
    }

    private fun str(description: String) = JSONObject().put("type", "string").put("description", description)
    private fun bool(description: String) = JSONObject().put("type", "boolean").put("description", description)
    private fun enumStr(description: String, values: List<String>) = str(description).put("enum", JSONArray(values))

    private fun obj(props: Map<String, JSONObject>, required: List<String>, nullable: Boolean = false): JSONObject {
        val properties = JSONObject()
        props.forEach { (name, spec) -> properties.put(name, spec) }
        val type = if (nullable) JSONArray(listOf("object", "null")) else "object"
        return JSONObject().put("type", type).put("properties", properties).put("required", JSONArray(required))
    }
}
