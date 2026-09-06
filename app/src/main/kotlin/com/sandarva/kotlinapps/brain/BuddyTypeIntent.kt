package com.sandarva.kotlinapps.brain

/**
 * On-device typing verbs.
 * “Type hello” / “search for pizza” / “press enter” do not need Gemini.
 * “Type that in the search box” still goes to the model so it can pick the field.
 */
object BuddyTypeIntent {
    sealed class Action {
        abstract val say: String
        data class Type(val text: String, val submit: Boolean, override val say: String) : Action()
        data class Submit(override val say: String = "Okay — I’ll press enter.") : Action()
    }

    fun parse(raw: String): Action? {
        val q = raw.trim()
        if (q.isBlank() || ASK.containsMatchIn(q.lowercase())) return null
        return quoted(q) ?: search(q) ?: type(q) ?: enter(q)
    }

    private fun quoted(q: String): Action.Type? {
        val m = QUOTED.find(q) ?: return null
        val lower = q.lowercase()
        if (!TYPE.containsMatchIn(lower) && !SEARCH.containsMatchIn(lower)) return null
        val text = m.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        if (text.isBlank()) return null
        val submit = SEARCH.containsMatchIn(lower)
        return Action.Type(text, submit, if (submit) SEARCH_SAY else TYPE_SAY)
    }

    private fun type(q: String): Action.Type? {
        val m = TYPE_REST.find(q) ?: return null
        val text = m.groupValues[1].trim().trim('"', '\'', '“', '”', '`')
        if (text.isBlank() || FIELD.containsMatchIn(text.lowercase())) return null
        return Action.Type(text, false, TYPE_SAY)
    }

    private fun search(q: String): Action.Type? {
        val m = SEARCH_REST.find(q) ?: return null
        val text = m.groupValues[1].trim().trim('"', '\'', '“', '”', '`')
        if (text.isBlank() || FIELD.containsMatchIn(text.lowercase())) return null
        return Action.Type(text, true, SEARCH_SAY)
    }

    private fun enter(q: String): Action.Submit? {
        val lower = q.lowercase().trim()
        if (!ENTER.containsMatchIn(lower)) return null
        val leftover = lower.replace(ENTER_NOISE, " ").replace(Regex("\\s+"), " ").trim()
        return if (leftover.isBlank()) Action.Submit() else null
    }

    private val ASK = Regex("""\b(how|where|what|why|which)\b""")
    private val TYPE = Regex("""\btype\b""")
    private val SEARCH = Regex("""\bsearch\b""")
    private val FIELD = Regex("""\b(in the|into the|on the|search box|text field|message box)\b""")
    private val QUOTED = Regex("""["“](.+?)["”]|'(.+?)'""")
    private val TYPE_REST = Regex("""(?i)^\s*(?:please\s+)?(?:can you\s+|could you\s+|would you\s+)?type\s+(.+)$""")
    private val SEARCH_REST = Regex("""(?i)^\s*(?:please\s+)?(?:can you\s+|could you\s+|would you\s+)?search(?:\s+for)?\s+(.+)$""")
    private val ENTER = Regex("""\b((press|hit|tap)\s+)?(enter|return)\b|\b(submit|press send|hit send|send it)\b""")
    private val ENTER_NOISE = Regex(
        """\b(please|can you|could you|would you|just|now|for me|the|a|an|and|then|it|this|that|here|press|hit|tap|enter|return|submit|send|buddy)\b"""
    )
    private const val TYPE_SAY = "Okay — I’ll type that."
    private const val SEARCH_SAY = "Okay — I’ll search for that."
}
