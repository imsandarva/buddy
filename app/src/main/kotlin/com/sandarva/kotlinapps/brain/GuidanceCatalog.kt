package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.accessibility.ScreenSnapshot

/** Compact on-screen list for the model. Bounds stay in the app. */
object GuidanceCatalog {
    fun format(snapshot: ScreenSnapshot): String {
        if (snapshot.nodes.isEmpty()) return "App: ${snapshot.packageName ?: "unknown"}\nOn screen: (nothing readable)"
        val lines = snapshot.nodes.joinToString("\n") { node ->
            val kind = if (node.clickable) "tap" else "label"
            "- ${node.id} | \"${node.label}\" | $kind"
        }
        return "App: ${snapshot.packageName ?: "unknown"}\nOn screen:\n$lines"
    }
}
