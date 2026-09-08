package com.sandarva.kotlinapps.brain.agent

import com.sandarva.kotlinapps.accessibility.ScreenSnapshot

/**
 * Reflection without a second model call: what visibly changed after an action.
 * Fed back into the step log so the model knows whether its last move worked.
 */
object SceneDiff {
    enum class Kind { AppChanged, Changed, Same, Unreadable }

    data class Change(val kind: Kind, val summary: String) {
        val stuck: Boolean get() = kind == Kind.Same
    }

    fun describe(before: ScreenSnapshot, after: ScreenSnapshot): Change {
        if (after.nodes.isEmpty()) return Change(Kind.Unreadable, "screen not readable afterwards")
        val app = after.appLabel ?: after.packageName ?: "another app"
        if (before.packageName != after.packageName) return Change(Kind.AppChanged, "now in $app: ${headline(after)}")
        val notes = ArrayList<String>(4)
        if (before.keyboardShown != after.keyboardShown) notes += if (after.keyboardShown) "keyboard opened" else "keyboard closed"
        flips(before, after, notes)
        val was = before.nodes.mapTo(HashSet(before.nodes.size)) { it.label }
        val now = after.nodes.mapTo(HashSet(after.nodes.size)) { it.label }
        val fresh = after.nodes.asSequence().map { it.label }.filter { it !in was }.distinct().toList()
        val gone = before.nodes.count { it.label !in now }
        if (fresh.isEmpty() && gone == 0 && notes.isEmpty()) return Change(Kind.Same, "nothing changed on screen")
        if (fresh.isNotEmpty()) notes += "new on screen: " + fresh.take(5).joinToString(", ") { "\"${it.take(28)}\"" } + if (fresh.size > 5) " (+${fresh.size - 5})" else ""
        else if (gone > 0) notes += "$gone item${if (gone == 1) "" else "s"} left the screen"
        return Change(Kind.Changed, notes.joinToString("; ").take(220))
    }

    /** A switch or checkbox with the same id that flipped is the most useful thing to report. */
    private fun flips(before: ScreenSnapshot, after: ScreenSnapshot, into: MutableList<String>) {
        for (node in after.nodes) {
            val checked = node.checked ?: continue
            val old = before.node(node.id)?.checked ?: continue
            if (old != checked) into += "\"${node.label.take(28)}\" is now ${if (checked) "ON" else "OFF"}"
            if (into.size >= 3) return
        }
    }

    private fun headline(snapshot: ScreenSnapshot): String =
        snapshot.nodes.take(4).joinToString(", ") { "\"${it.label.take(24)}\"" }
}
