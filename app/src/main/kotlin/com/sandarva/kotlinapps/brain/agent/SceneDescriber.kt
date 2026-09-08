package com.sandarva.kotlinapps.brain.agent

import com.sandarva.kotlinapps.accessibility.NodeRole
import com.sandarva.kotlinapps.accessibility.ScreenNode
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
import com.sandarva.kotlinapps.accessibility.ScreenTreeWalker

/**
 * Turns a snapshot into the SCREEN block the model reads. One line per control, top to bottom:
 * `[id] kind "label" STATE @x,y` — the id is what comes back in an action; x,y are percent of the
 * screen so the model can reason about rows and what sits near the bottom. Pixels never leave the app.
 */
object SceneDescriber {
    /** Drop the typed/spoken request if it leaked in as a text-field node. */
    fun withoutEcho(snapshot: ScreenSnapshot, request: String): ScreenSnapshot {
        val echo = request.trim()
        if (echo.length < 12) return snapshot
        val slug = ScreenTreeWalker.slug(echo)
        return snapshot.copy(nodes = snapshot.nodes.filterNot { it.label.equals(echo, ignoreCase = true) || it.id == slug })
    }

    fun describe(snapshot: ScreenSnapshot, maxLines: Int = AGENT_LINES): String {
        val app = snapshot.appLabel ?: snapshot.packageName ?: "unknown"
        if (snapshot.nodes.isEmpty()) return "APP: $app\nSCREEN: (nothing readable yet — the app may still be loading)"
        val shown = select(snapshot.nodes, maxLines)
        val hidden = snapshot.nodes.size - shown.size + snapshot.truncated
        return buildString(shown.size * 56 + 128) {
            append("APP: ").append(app).append('\n')
            if (snapshot.keyboardShown) append("KEYBOARD: open (a field is ready for typing; back closes it)\n")
            append("SCREEN (top to bottom; [id] kind \"label\" state @x,y in % of the screen):\n")
            for (node in shown) appendLine(snapshot, node)
            if (hidden > 0) append("(").append(hidden).append(" more lines not shown)\n")
            if (snapshot.scrollables().isNotEmpty()) append("Lists marked scrollable may hold more below or above — scroll to see it.")
        }.trimEnd()
    }

    /** Keep every control; when over the cap, drop plain text first (bottom-up), never an actionable line. */
    private fun select(nodes: List<ScreenNode>, limit: Int): List<ScreenNode> {
        if (nodes.size <= limit) return nodes
        val keep = BooleanArray(nodes.size)
        var kept = 0
        nodes.forEachIndexed { i, node -> if (node.actionable && kept < limit) { keep[i] = true; kept++ } }
        for (i in nodes.indices) {
            if (kept >= limit) break
            if (!keep[i]) { keep[i] = true; kept++ }
        }
        return nodes.filterIndexed { i, _ -> keep[i] }
    }

    private fun StringBuilder.appendLine(snapshot: ScreenSnapshot, node: ScreenNode) {
        append('[').append(node.id).append("] ").append(kind(node)).append(" \"").append(node.label).append('"')
        state(node).forEach { append(' ').append(it) }
        if (snapshot.screenWidth > 0 && snapshot.screenHeight > 0) {
            append(" @").append((node.bounds.centerX * 100f / snapshot.screenWidth).toInt().coerceIn(0, 100))
            append(',').append((node.bounds.centerY * 100f / snapshot.screenHeight).toInt().coerceIn(0, 100))
        }
        append('\n')
    }

    private fun kind(node: ScreenNode): String = when {
        node.role == NodeRole.List -> "list (scrollable)"
        node.role == NodeRole.Item && node.longClickable && !node.clickable -> "item (hold only)"
        else -> node.role.word
    }

    private fun state(node: ScreenNode): List<String> {
        val out = ArrayList<String>(3)
        node.checked?.let { out += if (it) "ON" else "OFF" }
        if (node.selected) out += "selected"
        if (!node.enabled) out += "disabled"
        if (node.focused && node.editable) out += "focused"
        if (node.password) out += "password"
        node.range?.let { out += it }
        return out
    }

    const val AGENT_LINES = 96
    const val LIVE_LINES = 64
}
