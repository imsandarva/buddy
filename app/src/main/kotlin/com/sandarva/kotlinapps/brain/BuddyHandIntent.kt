package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.accessibility.Direction
import com.sandarva.kotlinapps.accessibility.HandReach

/**
 * On-device finger verbs at the current tip.
 * “Tap here” / “hold this” / “drag left” do not need Gemini or a snapshot.
 * Named controls (“tap Wi‑Fi”) still go to the model.
 */
object BuddyHandIntent {
    sealed class Action {
        abstract val say: String
        data class Tap(override val say: String = "Okay — I’ll tap.") : Action()
        data class Hold(override val say: String = "Okay — I’ll hold.") : Action()
        data class Swipe(val dx: Float, val dy: Float, override val say: String) : Action()
        data class Drag(val dx: Float, val dy: Float, override val say: String) : Action()
        data class Scroll(val direction: Direction, override val say: String) : Action()
    }

    fun parse(raw: String): Action? {
        val q = raw.lowercase().trim()
        if (q.isBlank() || namesAControl(q)) return null
        return hold(q) ?: drag(q) ?: swipe(q) ?: scroll(q) ?: tap(q)
    }

    private fun tap(q: String): Action.Tap? {
        if (!TAP.containsMatchIn(q)) return null
        if (HOLD.containsMatchIn(q) || DRAG.containsMatchIn(q) || SWIPE.containsMatchIn(q) || SCROLL.containsMatchIn(q)) return null
        return Action.Tap()
    }

    private fun hold(q: String): Action.Hold? = if (HOLD.containsMatchIn(q) && !DRAG.containsMatchIn(q)) Action.Hold() else null

    private fun swipe(q: String): Action.Swipe? {
        if (!SWIPE.containsMatchIn(q)) return null
        val dir = direction(q) ?: return null
        return Action.Swipe(dir.dx, dir.dy, dir.swipeSay)
    }

    private fun scroll(q: String): Action.Scroll? {
        if (!SCROLL.containsMatchIn(q)) return null
        val dir = direction(q)?.direction ?: Direction.Down
        return Action.Scroll(dir, "Okay — I’ll scroll ${dir.word}.")
    }

    private fun drag(q: String): Action.Drag? {
        if (!DRAG.containsMatchIn(q)) return null
        val dir = direction(q) ?: return null
        return Action.Drag(dir.dx, dir.dy, dir.dragSay)
    }

    private fun direction(q: String): Dir? = DIRS.firstOrNull { it.match.containsMatchIn(q) }

    /** Leftover nouns mean they named a control — let Gemini resolve the id. */
    private fun namesAControl(q: String): Boolean {
        val leftover = q.replace(NOISE, " ").replace(Regex("\\s+"), " ").trim()
        return leftover.isNotBlank()
    }

    private data class Dir(val match: Regex, val dx: Float, val dy: Float, val swipeSay: String, val dragSay: String, val direction: Direction)

    private val TAP = Regex("""\b(tap|click|press)\b""")
    private val HOLD = Regex("""\b(hold|long[- ]?press|press and hold|press[- ]and[- ]hold)\b""")
    private val SWIPE = Regex("""\b(swipe|flick)\b""")
    private val DRAG = Regex("""\b(drag|pull|slide)\b""")
    private val SCROLL = Regex("""\b(scroll|page)\b""")
    private val NOISE = Regex(
        """\b(please|can you|could you|would you|just|now|for me|the|a|an|and|then|it|this|that|here|there|buddy|cursor|pointer|screen|tap|click|press|hold|long|drag|pull|slide|swipe|flick|scroll|page|down|up|left|right|upward|downward|upwards|downwards)\b"""
    )
    private val DIRS = listOf(
        Dir(Regex("""\b(left)\b"""), -STEP, 0f, "Okay — I’ll swipe left.", "Okay — I’ll drag left.", Direction.Left),
        Dir(Regex("""\b(right)\b"""), STEP, 0f, "Okay — I’ll swipe right.", "Okay — I’ll drag right.", Direction.Right),
        Dir(Regex("""\b(up|upward|upwards)\b"""), 0f, -STEP, "Okay — I’ll swipe up.", "Okay — I’ll drag up.", Direction.Up),
        Dir(Regex("""\b(down|downward|downwards)\b"""), 0f, STEP, "Okay — I’ll swipe down.", "Okay — I’ll drag down.", Direction.Down)
    )
    private const val STEP = HandReach.DRAG
}
