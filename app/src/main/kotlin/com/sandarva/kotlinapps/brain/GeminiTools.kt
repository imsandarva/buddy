package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.brain.goal.GoalPrompt
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.CursorLanding
import org.json.JSONArray
import org.json.JSONObject

/** Gemini tool declarations and parse. REST and Live share the same functions. */
object GeminiTools {
    fun body(question: String, catalog: String): String {
        return JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", GuidancePrompt.SYSTEM))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", GuidancePrompt.userMessage(question, catalog))))))
            .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations(includeSay = true, includeRunGoal = true))))
            .put("toolConfig", JSONObject().put("functionCallingConfig", JSONObject().put("mode", "ANY")))
            .put("generationConfig", JSONObject().put("temperature", 0.2))
            .toString()
    }

    fun goalBody(goal: String, catalog: String, trail: String, step: Int): String {
        return JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", GoalPrompt.SYSTEM))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", GoalPrompt.stepMessage(goal, catalog, trail, step))))))
            .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations(includeSay = true, includeGoalExtras = true))))
            .put("toolConfig", JSONObject().put("functionCallingConfig", JSONObject().put("mode", "ANY")))
            .put("generationConfig", JSONObject().put("temperature", 0.15))
            .toString()
    }

    fun functionDeclarations(
        includeSay: Boolean,
        includeRunGoal: Boolean = false,
        includeGoalExtras: Boolean = false
    ): JSONArray {
        val list = JSONArray()
        if (includeSay) list.put(fn("say", "One short warm sentence. Never include coordinates, ids, tool names, or lists.", listOf(Arg("text", "Words to say out loud.")), listOf("text")))
        if (includeRunGoal) list.put(fn("run_goal", "Hand a multi-step job to the on-screen runner. Use when they asked for more than one phone step — logout, set up Wi‑Fi, go do this then that. Put their request in goal. Speak a short on-it first. Do not tap yourself.", listOf(Arg("goal", "What they asked you to finish, in their words.")), listOf("goal")))
        if (includeGoalExtras) {
            list.put(fn("open_app", "Open an installed app by its launcher name. Prefer this over hunting the app drawer.", listOf(Arg("name", "Launcher label, like Pinterest or Settings.")), listOf("name")))
            list.put(fn("done", "The goal is finished, impossible, or stuck. Stop the loop.", listOf(Arg("status", "How the goal ended.", listOf("succeeded", "failed", "stuck")), Arg("text", "One short sentence they can hear.")), listOf("status")))
        }
        list.put(fn("point_to", "Point at a listed control. Use when they asked where or show me — not when they asked you to tap. Copy element_id exactly from SCREEN.", listOf(Arg("element_id", "Exact element_id from the SCREEN line whose label they meant.")), listOf("element_id")))
        list.put(fn("fly_to", "Fly the buddy cursor to a named place. Use only when they asked the buddy itself to move.", listOf(Arg("place", "Where the buddy should go.", CursorLanding.PLACES)), listOf("place")))
        list.put(fn("tap", "Tap a listed control like a finger. Copy element_id exactly from SCREEN. Omit to tap where the buddy is now. Never invent an id.", listOf(Arg("element_id", "Exact element_id from the SCREEN line whose label they meant."))))
        list.put(fn("hold", "Press and hold a listed control. Copy element_id exactly from SCREEN. Omit to hold where the buddy is now.", listOf(Arg("element_id", "Exact element_id from the SCREEN line whose label they meant."))))
        list.put(fn("swipe", "A quick finger swipe across the screen.", listOf(Arg("direction", "Swipe direction.", DIRS), Arg("from_element_id", "Optional start control, copied from SCREEN."), Arg("to_element_id", "Optional end control, copied from SCREEN."), Arg("to_place", "Optional named end place.", CursorLanding.PLACES))))
        list.put(fn("drag", "Press, hold, and slide — to move an icon or a slider.", listOf(Arg("direction", "Drag direction.", DIRS), Arg("from_element_id", "Optional start control, copied from SCREEN."), Arg("to_element_id", "Optional end control, copied from SCREEN."), Arg("to_place", "Optional named end place.", CursorLanding.PLACES))))
        list.put(fn("type", "Type into a listed type field. Use that line’s element_id. Set submit true for search or send. Never invent an id.", listOf(Arg("text", "Exact words to put in the field."), Arg("element_id", "Exact element_id of a type field from SCREEN."), Arg("submit", "true to press Search, Send, or Enter after typing.", listOf("true", "false"))), listOf("text")))
        return list
    }

    fun planFromCall(name: String, args: JSONObject): GuidancePlan = when (name) {
        "say" -> GuidancePlan(args.optString("text").ifBlank { null }, null, null)
        "point_to" -> GuidancePlan(null, args.optString("element_id").ifBlank { null }, null)
        "fly_to" -> GuidancePlan(null, null, args.optString("place").ifBlank { null })
        "tap" -> GuidancePlan(null, null, null, HandPlan.Tap(args.optString("element_id").ifBlank { null }))
        "hold" -> GuidancePlan(null, null, null, HandPlan.Hold(args.optString("element_id").ifBlank { null }))
        "swipe" -> GuidancePlan(null, null, null, stroke(args, holdFirst = false))
        "drag" -> GuidancePlan(null, null, null, stroke(args, holdFirst = true))
        "type" -> GuidancePlan(null, null, null, type = typePlan(args))
        "run_goal" -> GuidancePlan(null, null, null, runGoal = args.optString("goal").ifBlank { null })
        "open_app" -> GuidancePlan(null, null, null, openApp = args.optString("name").ifBlank { null })
        "done" -> GuidancePlan(args.optString("text").ifBlank { null }, null, null, done = args.optString("status").ifBlank { "done" })
        else -> GuidancePlan(null, null, null)
    }

    fun parse(raw: String): GuidancePlan {
        val parts = JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            ?: return GuidancePlan(null, null, null)
        var say: String? = null
        var elementId: String? = null
        var place: String? = null
        var hand: HandPlan? = null
        var type: TypePlan? = null
        var runGoal: String? = null
        var openApp: String? = null
        var done: String? = null
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
                    "type" -> type = typePlan(args)
                    "run_goal" -> runGoal = args.optString("goal").ifBlank { null }
                    "open_app" -> openApp = args.optString("name").ifBlank { null }
                    "done" -> {
                        done = args.optString("status").ifBlank { "done" }
                        if (say == null) say = args.optString("text").ifBlank { null }
                    }
                }
            } else {
                val text = part.optString("text").trim()
                if (text.isNotBlank() && say == null) say = text
            }
        }
        BuddyLog.d("Gemini.parse", "calls=$names say=${say != null} place=$place elementId=$elementId hand=$hand type=$type runGoal=${runGoal != null} openApp=$openApp done=$done")
        return GuidancePlan(say, elementId, place, hand, type, runGoal, openApp, done)
    }

    private fun typePlan(args: JSONObject) = TypePlan(
        text = args.optString("text"),
        elementId = args.optString("element_id").ifBlank { null },
        submit = args.optBoolean("submit") || args.optString("submit").equals("true", ignoreCase = true)
    )

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
