package com.sandarva.kotlinapps.brain.agent

import com.sandarva.kotlinapps.accessibility.BuddyGlobal
import com.sandarva.kotlinapps.accessibility.Direction
import org.json.JSONObject

/**
 * Everything Buddy's hands can do on a phone — what a finger does, plus the phone's own keys,
 * plus the two social moves (ask the person, tell the person). Targets are SCREEN ids, never pixels.
 */
sealed class AgentAction {
    /** Short line for the step log — what a person would write down. */
    abstract fun describe(): String

    /** `target` null means “right where the cursor is” — Live's “tap here”; the agent always names one. */
    data class Tap(val target: String?) : AgentAction() { override fun describe() = "tap ${target?.let { "[$it]" } ?: "here"}" }
    data class LongPress(val target: String?) : AgentAction() { override fun describe() = "long-press ${target?.let { "[$it]" } ?: "here"}" }
    data class Type(val target: String?, val text: String, val submit: Boolean) : AgentAction() {
        override fun describe() = "type \"${text.take(40)}\"${target?.let { " into [$it]" } ?: ""}${if (submit) " + enter" else ""}"
    }
    data class Scroll(val target: String?, val direction: Direction) : AgentAction() {
        override fun describe() = "scroll ${direction.word}${target?.let { " in [$it]" } ?: ""}"
    }
    data class Swipe(val direction: Direction) : AgentAction() { override fun describe() = "swipe ${direction.word}" }
    data class Drag(val from: String, val to: String) : AgentAction() { override fun describe() = "drag [$from] to [$to]" }
    data class Press(val key: BuddyGlobal.Key) : AgentAction() { override fun describe() = "press ${key.word}" }
    data class OpenApp(val name: String) : AgentAction() { override fun describe() = "open app \"$name\"" }
    data class Point(val target: String) : AgentAction() { override fun describe() = "point at [$target]" }
    data class MoveCursor(val place: String) : AgentAction() { override fun describe() = "move cursor to $place" }
    data class NudgeCursor(val dx: Float, val dy: Float) : AgentAction() { override fun describe() = "nudge cursor" }
    data class Ask(val question: String) : AgentAction() { override fun describe() = "ask \"${question.take(60)}\"" }
    object Wait : AgentAction() { override fun describe() = "wait" }
    object None : AgentAction() { override fun describe() = "no action" }

    /** Rough cost of the step for the guard — a finger action changes the phone, a social one does not. */
    val changesPhone: Boolean get() = this !is Point && this !is MoveCursor && this !is NudgeCursor && this !is Ask && this !is Wait && this !is None

    companion object {
        /** Verb names exactly as the model's schema spells them. */
        val TYPES = listOf(
            "tap", "long_press", "type", "scroll", "swipe", "drag", "back", "home", "recents", "notifications",
            "quick_settings", "open_app", "wait", "point", "move_cursor", "ask", "none"
        )

        /** Parses the model's `action` object; unknown shapes become [None] so the runner can hint the model. */
        fun fromJson(json: JSONObject?): AgentAction {
            if (json == null) return None
            val target = json.optString("target").trim().ifBlank { null }
            val text = json.optString("text").trim()
            val direction = Direction.parse(json.optString("direction"))
            return when (json.optString("type").trim().lowercase()) {
                "tap" -> target?.let(::Tap) ?: None
                "long_press" -> target?.let(::LongPress) ?: None
                "type" -> if (text.isBlank() && !json.optBoolean("submit")) None else Type(target, text, json.optBoolean("submit"))
                "scroll" -> Scroll(target, direction ?: Direction.Down)
                "swipe" -> Swipe(direction ?: Direction.Left)
                "drag" -> {
                    val to = json.optString("to").trim().ifBlank { null }
                    if (target != null && to != null) Drag(target, to) else None
                }
                "back" -> Press(BuddyGlobal.Key.Back)
                "home" -> Press(BuddyGlobal.Key.Home)
                "recents" -> Press(BuddyGlobal.Key.Recents)
                "notifications" -> Press(BuddyGlobal.Key.Notifications)
                "quick_settings" -> Press(BuddyGlobal.Key.QuickSettings)
                "open_app" -> text.ifBlank { target }?.let(::OpenApp) ?: None
                "wait" -> Wait
                "point" -> target?.let(::Point) ?: None
                "move_cursor" -> json.optString("place").trim().ifBlank { text }.ifBlank { null }?.let(::MoveCursor) ?: None
                "nudge_cursor", "nudge" -> direction?.let { NudgeCursor(it.dx * NUDGE, it.dy * NUDGE) } ?: None
                "ask" -> text.ifBlank { null }?.let(::Ask) ?: None
                else -> None
            }
        }

        const val NUDGE = 0.28f
    }
}
