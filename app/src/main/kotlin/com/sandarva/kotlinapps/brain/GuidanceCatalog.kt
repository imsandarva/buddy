package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
import com.sandarva.kotlinapps.accessibility.ScreenTreeWalker

/** Compact on-screen list for the model. Bounds stay in the app. */
object GuidanceCatalog {
    fun format(snapshot: ScreenSnapshot, limit: Int = 160): String {
        if (snapshot.nodes.isEmpty()) return "App: ${appLabel(snapshot.packageName)}\nOn screen: (nothing readable)"
        val nodes = snapshot.nodes.take(limit)
        val lines = nodes.joinToString("\n") { node ->
            val kind = when {
                node.editable -> "type"
                node.clickable -> "tap"
                else -> "label"
            }
            "- ${node.id} | \"${node.label}\" | $kind"
        }
        return "App: ${appLabel(snapshot.packageName)}\nOn screen:\n$lines"
    }

    /** Drop the typed/spoken question if it leaked in as a text-field node. */
    fun forModel(snapshot: ScreenSnapshot, question: String): ScreenSnapshot {
        val echo = question.trim()
        if (echo.length < 12) return snapshot
        val slug = ScreenTreeWalker.slug(echo)
        val nodes = snapshot.nodes.filterNot { it.label.equals(echo, ignoreCase = true) || it.id == slug }
        return snapshot.copy(nodes = nodes)
    }

    private fun appLabel(pkg: String?): String = when {
        pkg.isNullOrBlank() -> "unknown"
        pkg == "com.android.systemui" || pkg.endsWith(".systemui") -> "Notifications and quick settings"
        else -> pkg
    }
}
