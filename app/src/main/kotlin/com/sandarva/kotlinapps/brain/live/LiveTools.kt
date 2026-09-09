package com.sandarva.kotlinapps.brain.live

import com.sandarva.kotlinapps.accessibility.BuddyGlobal
import com.sandarva.kotlinapps.accessibility.Direction
import com.sandarva.kotlinapps.brain.agent.AgentAction
import com.sandarva.kotlinapps.overlay.CursorLanding
import org.json.JSONArray
import org.json.JSONObject

/** What a Live tool call asks for: one immediate hand move, a multi-step job, a job answer, or something we do not know. */
sealed class LiveIntent {
    data class Act(val action: AgentAction) : LiveIntent()
    data class RunGoal(val goal: String) : LiveIntent()
    data class AnswerJob(val text: String) : LiveIntent()
    object CancelJob : LiveIntent()
    data class Unknown(val name: String) : LiveIntent()
}

/**
 * Function declarations for the Live session and the mapping back onto [AgentAction].
 * Live keeps one-shot tools for snappy “tap Wi‑Fi” moments. `run_goal` is only for a real
 * multi-step job — the router still has to agree before the runner starts.
 */
object LiveTools {
    fun declarations(): JSONArray = JSONArray()
        .put(fn("run_goal", RUN_GOAL_HELP, listOf(Arg("goal", "The phone job they asked you to finish, in their words.")), listOf("goal")))
        .put(fn("answer_job", "Pass their answer to the job that asked a question. Only after a JOB ASK. Put their words in answer.", listOf(Arg("answer", "What they said, in their words.")), listOf("answer")))
        .put(fn("cancel_job", "Stop the job that is on the screen. Only when they asked to stop, cancel, or never mind that job.", emptyList()))
        .put(fn("point_to", "Fly the cursor to a listed control to show it, without pressing. Use when they asked where or show me. Copy the id exactly from SCREEN.", listOf(Arg("element_id", ID_HELP)), listOf("element_id")))
        .put(fn("fly_to", "Move the buddy itself to a named place — a corner, side, or middle. Not for “up” or “down”; that is nudge.", listOf(Arg("place", "Where the buddy should go.", CursorLanding.PLACES)), listOf("place")))
        .put(fn("nudge", "Slide the buddy a bit up, down, left, or right. Only when they asked the buddy itself to move that way.", listOf(Arg("direction", "Which way to slide.", DIRS)), listOf("direction")))
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
            "answer_job" -> a.optString("answer").ifBlank { null }?.let { LiveIntent.AnswerJob(it) } ?: LiveIntent.Unknown(call.name)
            "cancel_job" -> LiveIntent.CancelJob
            "point_to" -> id?.let { LiveIntent.Act(AgentAction.Point(it)) } ?: LiveIntent.Unknown(call.name)
            "fly_to" -> fly(a.optString("place"))
            "nudge" -> Direction.parse(a.optString("direction"))?.let { LiveIntent.Act(AgentAction.NudgeCursor(it.dx * AgentAction.NUDGE, it.dy * AgentAction.NUDGE)) } ?: LiveIntent.Unknown(call.name)
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

    /** Named landing, or a direction treated as a nudge so “fly up” still works. */
    private fun fly(place: String): LiveIntent {
        val raw = place.trim().ifBlank { return LiveIntent.Unknown("fly_to") }
        if (CursorLanding.normalized(raw) != null) return LiveIntent.Act(AgentAction.MoveCursor(raw))
        val dir = Direction.parse(raw) ?: return LiveIntent.Unknown("fly_to")
        return LiveIntent.Act(AgentAction.NudgeCursor(dir.dx * AgentAction.NUDGE, dir.dy * AgentAction.NUDGE))
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
    private const val RUN_GOAL_HELP = "Only for a multi-step phone job they asked you to finish — find a setting, log out, set up Wi-Fi, free storage. Invocation: they want the phone changed or something found that is not on this SCREEN. This talk stays open. Do not call for hi, how are you, what do you see, what’s on the screen, moving the buddy, or any one-shot. Put their job in goal. Say a short on-it first, then keep talking."
    private val DIRS = listOf("up", "down", "left", "right")
}
