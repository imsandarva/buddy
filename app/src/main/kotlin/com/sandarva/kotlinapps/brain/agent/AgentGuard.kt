package com.sandarva.kotlinapps.brain.agent

import android.os.SystemClock
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot

/**
 * The runner's judgement: when to think harder, when to nudge the model, when to ask before a
 * risky press, and when to stop. Cheap on-device rules — the model decides, the guard keeps it honest.
 */
class AgentGuard(goal: String) {
    sealed class Verdict {
        object Proceed : Verdict()
        data class Confirm(val question: String) : Verdict()
        data class Stop(val message: String) : Verdict()
    }

    private val goalWords = goal.lowercase()
    private val startedAt = SystemClock.elapsedRealtime()
    private var stuck = 0
    private var failed = 0
    private var unreadable = 0
    private var repeats = 0
    private var lastAction: String? = null
    private var invalidReplies = 0
    private var searches = 0
    var confirmed = false

    /** Escalate to the stronger model once the fast one is spinning its wheels. */
    fun deeperThought(): Boolean = stuck >= 2 || failed >= 2 || repeats >= 2 || invalidReplies >= 1

    /** Before acting: hard limits first, then a confirmation for presses that are hard to undo. */
    fun review(step: Int, decision: AgentDecision, snapshot: ScreenSnapshot): Verdict {
        if (step > MAX_STEPS) return Verdict.Stop("That’s as far as I could take it on my own.")
        if (SystemClock.elapsedRealtime() - startedAt > MAX_WALL_MS) return Verdict.Stop("That took longer than it should — let’s try again together.")
        if (stuck >= STUCK_STOP) return Verdict.Stop("I couldn’t find a way forward on this screen.")
        if (repeats >= REPEAT_STOP || failed >= FAIL_STOP) return Verdict.Stop("I kept running into the same wall, so I stopped.")
        if (unreadable >= UNREADABLE_STOP) return Verdict.Stop("I can’t read this screen well enough to keep going.")
        if (invalidReplies >= INVALID_STOP) return Verdict.Stop("I couldn’t think that through just now. Try once more in a moment.")
        riskyLabel(decision.action, snapshot)?.let { label ->
            if (!confirmed) return Verdict.Confirm("Just checking before I go on — this will tap “$label”. Should I?")
        }
        return Verdict.Proceed
    }

    /** After acting: remember how it went so the next prompt can carry a nudge. */
    fun observe(action: AgentAction, outcome: Outcome, change: SceneDiff.Change?) {
        if (action is AgentAction.SearchWeb) searches += 1
        val key = action.describe()
        repeats = if (key == lastAction) repeats + 1 else 0
        lastAction = key
        failed = if (outcome.ok) 0 else failed + 1
        if (action.changesPhone && change != null) {
            stuck = if (change.stuck) stuck + 1 else 0
            unreadable = if (change.kind == SceneDiff.Kind.Unreadable) unreadable + 1 else 0
        }
    }

    fun observeInvalidReply() { invalidReplies += 1 }
    fun observeValidReply() { invalidReplies = 0 }

    /** Extra guidance woven into the next step when the run is not going well. */
    fun hint(): String? = when {
        searches >= 3 -> "You have searched $searches times. Use what you already found, or finish with the answer. Do not search again unless the last result was empty."
        stuck >= 2 -> "Your last $stuck actions changed nothing on screen. Do something different: scroll to reveal more, use a search box, press back, search_web for the path, or try another control. If the goal truly cannot be reached here, finish with status cannot."
        stuck == 1 -> "Your last action changed nothing. Check the SCREEN before repeating it."
        repeats >= 2 -> "You have repeated the same action ${repeats + 1} times. Choose a different one."
        failed >= 1 -> "Your last action failed — the id may not exist on this SCREEN. Copy ids exactly from the list below."
        invalidReplies >= 1 -> "Your last reply could not be read. Return only the JSON form."
        else -> null
    }

    /** A press on a label that is hard to undo needs a yes first — unless they asked for exactly that. */
    private fun riskyLabel(action: AgentAction, snapshot: ScreenSnapshot): String? {
        val target = when (action) { is AgentAction.Tap -> action.target; is AgentAction.LongPress -> action.target; else -> null } ?: return null
        val label = snapshot.node(target)?.label ?: return null
        val hit = RISKY.find(label.lowercase())?.value ?: return null
        val family = FAMILIES.firstOrNull { hit in it } ?: listOf(hit)
        return if (family.any { it in goalWords }) null else label.take(40)
    }

    companion object {
        const val MAX_STEPS = 30
        private const val MAX_WALL_MS = 4 * 60_000L
        private const val STUCK_STOP = 5
        private const val REPEAT_STOP = 4
        private const val FAIL_STOP = 4
        private const val UNREADABLE_STOP = 4
        private const val INVALID_STOP = 3
        private val RISKY = Regex("""\b(delete|erase|remove|clear data|uninstall|factory reset|reset|format|pay|buy|purchase|checkout|place order|send|sign out|log out|logout|block|report|deactivate|unfollow)\b""")
        private val FAMILIES = listOf(
            listOf("delete", "erase", "remove", "clear", "trash"),
            listOf("sign out", "log out", "logout", "log me out", "sign me out"),
            listOf("pay", "buy", "purchase", "checkout", "order"),
            listOf("reset", "factory"),
            listOf("send", "message", "text", "email", "reply")
        )
    }
}
