package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.CursorLanding
import org.json.JSONArray
import org.json.JSONObject

/** Gemini tool declarations and parse. The HTTP client stays unaware of cursor meaning. */
object GeminiTools {
    fun body(question: String, catalog: String): String {
        val say = fn("say", "Speak a short warm instruction. Never include coordinates or ids.", "text", "Words to say out loud.")
        val point = fn("point_to", "Point the buddy at a visible control from the list. Only use an exact element_id from the list. Never invent an id.", "element_id", "Exact element_id from the on-screen list.")
        val fly = fn("fly_to", "Fly the buddy cursor to a place on the screen. Use when they ask the buddy itself to move.", "place", "Where the buddy should go.", CursorLanding.PLACES)
        return JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", GuidancePrompt.SYSTEM))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", GuidancePrompt.userMessage(question, catalog))))))
            .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", JSONArray().put(say).put(point).put(fly))))
            .put("toolConfig", JSONObject().put("functionCallingConfig", JSONObject().put("mode", "ANY")))
            .put("generationConfig", JSONObject().put("temperature", 0.2))
            .toString()
    }

    fun parse(raw: String): GuidancePlan {
        val parts = JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            ?: return GuidancePlan(null, null, null)
        var say: String? = null
        var elementId: String? = null
        var place: String? = null
        val names = ArrayList<String>(4)
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val call = part.optJSONObject("functionCall")
            if (call != null) {
                val name = call.optString("name")
                names += name
                val args = call.optJSONObject("args") ?: JSONObject()
                when (name) {
                    "say" -> say = args.optString("text").ifBlank { null }
                    "point_to" -> elementId = args.optString("element_id").ifBlank { null }
                    "fly_to" -> place = args.optString("place").ifBlank { null }
                }
            } else {
                val text = part.optString("text").trim()
                if (text.isNotBlank() && say == null) say = text
            }
        }
        BuddyLog.d("Gemini.parse", "calls=$names say=${say != null} place=$place elementId=$elementId")
        return GuidancePlan(say, elementId, place)
    }

    private fun fn(name: String, description: String, arg: String, argHelp: String, enumValues: List<String>? = null): JSONObject {
        val spec = JSONObject().put("type", "STRING").put("description", argHelp)
        if (enumValues != null) spec.put("enum", JSONArray(enumValues))
        val props = JSONObject().put(arg, spec)
        val params = JSONObject().put("type", "OBJECT").put("properties", props).put("required", JSONArray().put(arg))
        return JSONObject().put("name", name).put("description", description).put("parameters", params)
    }
}
