package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.CursorLanding
import org.json.JSONArray
import org.json.JSONObject

/** Gemini tool declarations and parse. The HTTP client stays unaware of cursor meaning. */
object GeminiTools {
    fun body(question: String, catalog: String): String {
        val say = fn("say", "Speak a short warm instruction. Never include coordinates or ids.", listOf(Arg("text", "Words to say out loud.")), listOf("text"))
        val point = fn("point_to", "Only point at a visible control. Use when they asked where, not when they asked you to tap.", listOf(Arg("element_id", "Exact element_id from the on-screen list.")), listOf("element_id"))
        val fly = fn("fly_to", "Fly the buddy cursor to a place on the screen. Use when they ask the buddy itself to move.", listOf(Arg("place", "Where the buddy should go.", CursorLanding.PLACES)), listOf("place"))
        val tap = fn("tap", "Tap a listed control like a finger. Omit element_id to tap where the buddy is now.", listOf(Arg("element_id", "Exact element_id from the on-screen list.")))
        val hold = fn("hold", "Press and hold a listed control. Omit element_id to hold where the buddy is now.", listOf(Arg("element_id", "Exact element_id from the on-screen list.")))
        val swipe = fn("swipe", "A quick finger swipe across the screen.", listOf(Arg("direction", "Swipe direction.", DIRS), Arg("from_element_id", "Optional start control."), Arg("to_element_id", "Optional end control."), Arg("to_place", "Optional named end place.", CursorLanding.PLACES)))
        val drag = fn("drag", "Press, hold, and slide — to move an icon or a slider.", listOf(Arg("direction", "Drag direction.", DIRS), Arg("from_element_id", "Optional start control."), Arg("to_element_id", "Optional end control."), Arg("to_place", "Optional named end place.", CursorLanding.PLACES)))
        return JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", GuidancePrompt.SYSTEM))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", GuidancePrompt.userMessage(question, catalog))))))
            .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", JSONArray().put(say).put(point).put(fly).put(tap).put(hold).put(swipe).put(drag))))
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
        var hand: HandPlan? = null
        val names = ArrayList<String>(6)
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
                    "tap" -> hand = HandPlan.Tap(args.optString("element_id").ifBlank { null })
                    "hold" -> hand = HandPlan.Hold(args.optString("element_id").ifBlank { null })
                    "swipe" -> hand = stroke(args, holdFirst = false)
                    "drag" -> hand = stroke(args, holdFirst = true)
                }
            } else {
                val text = part.optString("text").trim()
                if (text.isNotBlank() && say == null) say = text
            }
        }
        BuddyLog.d("Gemini.parse", "calls=$names say=${say != null} place=$place elementId=$elementId hand=$hand")
        return GuidancePlan(say, elementId, place, hand)
    }

    private fun stroke(args: JSONObject, holdFirst: Boolean) = HandPlan.Stroke(
        fromId = args.optString("from_element_id").ifBlank { null },
        toId = args.optString("to_element_id").ifBlank { null },
        toPlace = args.optString("to_place").ifBlank { null },
        direction = args.optString("direction").ifBlank { null },
        holdFirst = holdFirst
    )

    private fun fn(name: String, description: String, args: List<Arg>, required: List<String> = emptyList()): JSONObject {
        val props = JSONObject()
        args.forEach { arg ->
            val spec = JSONObject().put("type", "STRING").put("description", arg.help)
            if (arg.enumValues != null) spec.put("enum", JSONArray(arg.enumValues))
            props.put(arg.name, spec)
        }
        val params = JSONObject().put("type", "OBJECT").put("properties", props)
        if (required.isNotEmpty()) params.put("required", JSONArray(required))
        return JSONObject().put("name", name).put("description", description).put("parameters", params)
    }

    private data class Arg(val name: String, val help: String, val enumValues: List<String>? = null)

    private val DIRS = listOf("left", "right", "up", "down")
}
