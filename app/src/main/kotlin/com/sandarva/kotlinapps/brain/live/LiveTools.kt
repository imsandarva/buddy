package com.sandarva.kotlinapps.brain.live

import com.sandarva.kotlinapps.accessibility.BuddyGlobal
import com.sandarva.kotlinapps.accessibility.Direction
import com.sandarva.kotlinapps.brain.agent.AgentAction
import com.sandarva.kotlinapps.overlay.CursorLanding
import org.json.JSONArray
import org.json.JSONObject

/** What a Live tool call asks for: one immediate hand move, a multi-step job, or something we do not know. */
sealed class LiveIntent {
    data class Act(val action: AgentAction) : LiveIntent()
    data class RunGoal(val goal: String) : LiveIntent()
    data class Unknown(val name: String) : LiveIntent()
}

/**
 * Function declarations for the Live session and the mapping back onto [AgentAction].
 * Live keeps one-shot tools for snappy “tap Wi‑Fi” moments; anything longer is `run_goal`.
 */
object LiveTools {
    fun declarations(): JSONArray = JSONArray()
        .put(fn("run_goal", "Hand a job that takes more than one phone step to the on-screen runner — find a setting, log out, set up Wi‑Fi, go do this then that, answer a question that needs looking around the phone. Put their request in goal. Say a short on-it first. Do not tap yourself.", listOf(Arg("goal", "What they asked you to finish, in their words.")), listOf("goal")))
        .put(fn("point_to", "Fly the cursor to a listed control to show it, without pressing. Use when they asked where or show me. Copy the id exactly from SCREEN.", listOf(Arg("element_id", ID_HELP)), listOf("element_id")))
        .put(fn("fly_to", "Move the buddy cursor to a named place. Only when they asked the buddy itself to move.", listOf(Arg("place", "Where the buddy should go.", CursorLanding.PLACES)), listOf("place")))
        .put(fn("tap", "Tap a listed control like a finger. Copy the id exactly from SCREEN. Omit to tap where the buddy is now.", listOf(Arg("element_id", ID_HELP))))
        .put(fn("hold", "Press and hold a listed control. Copy the id exactly from SCREEN. Omit to hold where the buddy is now.", listOf(Arg("element_id", ID_HELP))))
        .put(fn("scroll", "Reveal more content in a list. direction is where the content is: down shows what is below.", listOf(Arg("direction", "Where the content is.", DIRS), Arg("element_id", "Optional list id from SCREEN when several lists exist."))))
        .put(fn("swipe", "A finger swipe across the whole screen — turn a launcher page, dismiss a card. direction is where the finger moves.", listOf(Arg("direction", "Where the finger moves.", DIRS)), listOf("direction")))
        .put(fn("drag", "Press, hold, and slide one listed control onto another — rearrange, move to a folder.", listOf(Arg("from_element_id", ID_HELP), Arg("to_element_id", ID_HELP)), listOf("from_element_id", "to_element_id")))
        .put(fn("type", "Type into a listed field. Use that field's id when one is on SCREEN. Set submit true for search or send.", listOf(Arg("text", "Exact words to put in the field."), Arg("element_id", "Exact id of a field from SCREEN."), Arg("submit", "true to press Search, Send, or Enter after typing.", listOf("true", "false"))), listOf("text")))
        .put(fn("back", "Press the phone's Back button — close a menu, dialog, or keyboard, or go to the previous screen.", emptyList()))
        .put(fn("home", "Go to the home screen.", emptyList()))
        .put(fn("open_app", "Open an installed app by its name. Prefer this over hunting the app drawer.", listOf(Arg("name", "App name, like Settings or Chrome.")), listOf("name")))

    fun intent(call: LiveFunctionCall): LiveIntent {
        val a = call.args
        val id = a.optString("element_id").ifBlank { null }
        return when (call.name) {
            "run_goal" -> a.optString("goal").ifBlank { null }?.let { LiveIntent.RunGoal(it) } ?: LiveIntent.Unknown(call.name)
            "point_to" -> id?.let { LiveIntent.Act(AgentAction.Point(it)) } ?: LiveIntent.Unknown(call.name)
            "fly_to" -> a.optString("place").ifBlank { null }?.let { LiveIntent.Act(AgentAction.MoveCursor(it)) } ?: LiveIntent.Unknown(call.name)
            "tap" -> LiveIntent.Act(AgentAction.Tap(id))
            "hold" -> LiveIntent.Act(AgentAction.LongPress(id))
            "scroll" -> LiveIntent.Act(AgentAction.Scroll(id, Direction.parse(a.optString("direction")) ?: Direction.Down))
            "swipe" -> LiveIntent.Act(AgentAction.Swipe(Direction.parse(a.optString("direction")) ?: Direction.Left))
            "drag" -> {
                val from = a.optString("from_element_id").ifBlank { null }
                val to = a.optString("to_element_id").ifBlank { null }
                if (from != null && to != null) LiveIntent.Act(AgentAction.Drag(from, to)) else LiveIntent.Unknown(call.name)
            }
            "type" -> LiveIntent.Act(AgentAction.Type(id, a.optString("text"), a.optBoolean("submit") || a.optString("submit").equals("true", ignoreCase = true)))
            "back" -> LiveIntent.Act(AgentAction.Press(BuddyGlobal.Key.Back))
            "home" -> LiveIntent.Act(AgentAction.Press(BuddyGlobal.Key.Home))
            "open_app" -> a.optString("name").ifBlank { null }?.let { LiveIntent.Act(AgentAction.OpenApp(it)) } ?: LiveIntent.Unknown(call.name)
            else -> LiveIntent.Unknown(call.name)
        }
    }

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

    private const val ID_HELP = "Exact id from the SCREEN line whose label they meant, copied character for character."
    private val DIRS = listOf("up", "down", "left", "right")
}
