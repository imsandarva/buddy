package com.sandarva.kotlinapps.accessibility

/** Picks one teachable control so a debug tap can prove `point_to` without a model. */
object GuidePicker {
    fun choose(snapshot: ScreenSnapshot): ScreenNode? {
        val labeled = snapshot.nodes.filter { it.label.isNotBlank() && it.bounds.height >= 36 && it.bounds.width >= 48 }
        val pool = labeled.filter { it.clickable }.ifEmpty { labeled }
        return pool.maxByOrNull(::score)
    }

    private fun score(node: ScreenNode): Int {
        var score = 0
        if (node.clickable) score += 8
        if (node.label.length in 3..48) score += 6
        if (node.bounds.height in 48..280) score += 10
        if (node.bounds.width > 280) score += 4
        if (node.bounds.top > 140) score += 5
        if (node.viewId != null) score += 3
        if (node.bounds.height > 400) score -= 8
        return score
    }
}
