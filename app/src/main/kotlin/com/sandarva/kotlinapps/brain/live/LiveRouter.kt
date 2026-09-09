package com.sandarva.kotlinapps.brain.live

import com.sandarva.kotlinapps.accessibility.BuddyGlobal
import com.sandarva.kotlinapps.brain.BuddyMoveIntent
import com.sandarva.kotlinapps.brain.agent.AgentAction

/**
 * Veto gate in front of `run_goal`. The Live model already picked a job; we only stop greetings,
 * “what do you see”, and one-shots Live can do itself. Missing a keyword is not a veto — that is
 * how “open Pinterest and log me out” used to die as Talk(“no job”).
 */
object LiveRouter {

    sealed class Route {
        data class Talk(val detail: String) : Route()
        data class Act(val action: AgentAction) : Route()
        data class Goal(val goal: String) : Route()
    }

    fun decide(goal: String): Route {
        val q = goal.trim()
        if (q.isBlank()) return Route.Talk("empty")
        move(q)?.let { return it }
        if (isTalk(q)) return Route.Talk("conversation")
        oneShot(q)?.let { return it }
        return Route.Goal(q)
    }

    fun talkReply(): String =
        "not a job — answer with your voice. Call search_web if you need a fact SCREEN does not have. Do not call run_goal again."

    private fun move(q: String): Route.Act? = when (val move = BuddyMoveIntent.parse(q)) {
        is BuddyMoveIntent.Move.ToPlace -> Route.Act(AgentAction.MoveCursor(move.place))
        is BuddyMoveIntent.Move.Nudge -> Route.Act(AgentAction.NudgeCursor(move.dx, move.dy))
        null -> null
    }

    /** Greetings and “what do you see” — unless they also asked for phone work. */
    private fun isTalk(q: String): Boolean {
        val text = q.lowercase()
        if (SEE.containsMatchIn(text)) return true
        if (isWork(text)) return false
        return GREET.containsMatchIn(text) || META.containsMatchIn(text) || SOCIAL.containsMatchIn(text)
    }

    private fun oneShot(q: String): Route.Act? {
        val text = q.lowercase().trim().trimEnd('.', '!', '?')
        if (BACK.matches(text)) return Route.Act(AgentAction.Press(BuddyGlobal.Key.Back))
        if (HOME.matches(text)) return Route.Act(AgentAction.Press(BuddyGlobal.Key.Home))
        val name = OPEN.matchEntire(text)?.groupValues?.get(1)?.trim().orEmpty()
        if (name.isBlank() || AND.containsMatchIn(name) || name.length > 40) return null
        return Route.Act(AgentAction.OpenApp(name))
    }

    /** Enough phone-work that “hey, log me out” is not small talk. */
    private fun isWork(q: String): Boolean = WORK.containsMatchIn(q.lowercase())

    private val SEE = Regex(
        """\b(what('s| is| are)? (on )?(the |my )?screen|what (are you|you'?re) (seeing|looking at)|""" +
            """what do you see|describe (the )?(screen|this)|what app is (this|that|open))\b"""
    )
    private val GREET = Regex(
        """\b(hi|hey|hello|howdy|yo|what'?s up|how are you|how'?s it going|how do you do|""" +
            """good (morning|afternoon|evening|night)|you (ok|okay|good|there))\b"""
    )
    private val META = Regex("""\b(who are you|what (are you|can you do)|your name|what do you do)\b""")
    private val SOCIAL = Regex("""\b(thanks|thank you|thx|bye|goodbye|see you|never ?mind|cool|nice|got it)\b""")
    private val BACK = Regex("""^(please )?(go )?back$""")
    private val HOME = Regex("""^(please )?(go )?(to )?(the )?home( screen)?$""")
    private val OPEN = Regex("""^(?:please )?(?:could you )?(?:open|launch|start) (.+)$""")
    private val AND = Regex("""\b(and|then)\b""")
    private val WORK = Regex(
        """\b(open|launch|start|turn(ing)? (it |them )?(on|off)|switch(ing)? (it )?(on|off)|enable|disable|toggle|""" +
            """log(ging)?( me| us)? out|sign(ing)?( me| us)? out|uninstall|install|delete|remove|set up|connect to|""" +
            """disconnect|forget|how much (storage|space|battery|memory)|free up|available storage|""" +
            """change (the |my )?(date|time|font|brightness|language|password)|""" +
            """go to settings|in settings|find (my |the )|where (is|can i)|show me how|""" +
            """help me (turn|find|open|change|set|connect|log)|airplane mode)\b"""
    )
}
