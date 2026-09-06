package com.sandarva.kotlinapps.accessibility

/** Screen-pixel box. The cursor tip lands at the center. */
data class ScreenBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    fun contains(other: ScreenBounds): Boolean =
        left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom && this != other
}

/** One visible control. `id` is what a later `point_to` tool will send. */
data class ScreenNode(
    val id: String,
    val label: String,
    val bounds: ScreenBounds,
    val clickable: Boolean,
    val viewId: String? = null,
    val editable: Boolean = false
)

/** Immutable copy of what is on screen. Never holds live `AccessibilityNodeInfo`. */
data class ScreenSnapshot(val packageName: String?, val nodes: List<ScreenNode>) {
    fun node(id: String): ScreenNode? = nodes.find { it.id == id }

    companion object {
        val Empty = ScreenSnapshot(null, emptyList())
    }
}
