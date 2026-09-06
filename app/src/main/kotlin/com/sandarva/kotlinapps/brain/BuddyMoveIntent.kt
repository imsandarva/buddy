package com.sandarva.kotlinapps.brain

/**
 * On-device cursor verbs. Sliding the buddy around does not need Gemini —
 * same idea as AssistiveTouch / Switch Access directional moves.
 */
object BuddyMoveIntent {
    sealed class Move {
        abstract val say: String
        data class ToPlace(val place: String, override val say: String) : Move()
        data class Nudge(val dx: Float, val dy: Float, override val say: String) : Move()
    }

    fun parse(raw: String): Move? {
        val q = raw.lowercase().trim()
        if (q.isBlank() || !looksLikeMove(q)) return null
        return place(q) ?: nudge(q)
    }

    private fun looksLikeMove(q: String): Boolean {
        if (HAND.containsMatchIn(q)) return false
        if (TASK.containsMatchIn(q) && !CURSOR.containsMatchIn(q)) return false
        return CURSOR.containsMatchIn(q) || (MOVE.containsMatchIn(q) && (PLACE.containsMatchIn(q) || DIR.containsMatchIn(q)))
    }

    private fun place(q: String): Move.ToPlace? {
        val hit = PLACES.firstOrNull { it.match.containsMatchIn(q) } ?: return null
        return Move.ToPlace(hit.place, hit.say)
    }

    private fun nudge(q: String): Move.Nudge? {
        val hit = NUDGES.firstOrNull { it.match.containsMatchIn(q) } ?: return null
        return Move.Nudge(hit.dx, hit.dy, hit.say)
    }

    private data class PlacePat(val match: Regex, val place: String, val say: String)
    private data class NudgePat(val match: Regex, val dx: Float, val dy: Float, val say: String)

    private val CURSOR = Regex("""\b(cursor|buddy|pointer)\b""")
    private val MOVE = Regex("""\b(move|fly|nudge|slide|put|place|go)\b""")
    private val HAND = Regex("""\b(tap|click|press|hold|drag|swipe|flick|pull|slide)\b""")
    private val TASK = Regex("""\b(settings|wifi|date|time|tap|click|open|press|type|search|bluetooth)\b""")
    private val PLACE = Regex("""\b(top|bottom|center|middle|corner|left|right)\b""")
    private val DIR = Regex("""\b(up|down|left|right|upward|downward|upwards|downwards|higher|lower)\b""")

    private val PLACES = listOf(
        PlacePat(Regex("""top[-_\s]?left|upper[-_\s]?left"""), "top_left", "Okay — I’m going to the top left."),
        PlacePat(Regex("""top[-_\s]?right|upper[-_\s]?right"""), "top_right", "Okay — I’m going to the top right."),
        PlacePat(Regex("""bottom[-_\s]?left"""), "bottom_left", "Okay — I’m going to the bottom left."),
        PlacePat(Regex("""bottom[-_\s]?right"""), "bottom_right", "Okay — I’m going to the bottom right."),
        PlacePat(Regex("""\b(center|middle)\b"""), "center", "Okay — I’m going to the middle."),
        PlacePat(Regex("""\bto the top\b|\bat the top\b|\btop of (the )?screen\b"""), "top", "Okay — I’m going to the top."),
        PlacePat(Regex("""\bto the bottom\b|\bat the bottom\b|\bbottom of (the )?screen\b"""), "bottom", "Okay — I’m going to the bottom.")
    )

    private val NUDGES = listOf(
        NudgePat(Regex("""\b(up|upward|upwards|higher)\b"""), 0f, -STEP, "Okay — I’m moving up."),
        NudgePat(Regex("""\b(down|downward|downwards|lower)\b"""), 0f, STEP, "Okay — I’m moving down."),
        NudgePat(Regex("""\b(left)\b"""), -STEP, 0f, "Okay — I’m moving left."),
        NudgePat(Regex("""\b(right)\b"""), STEP, 0f, "Okay — I’m moving right.")
    )

    private const val STEP = 0.28f
}
