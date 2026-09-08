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

/** What kind of control a person would call it. Drives both the model's wording and the executor. */
enum class NodeRole(val word: String) {
    Button("button"), Item("item"), Switch("switch"), Checkbox("checkbox"), Radio("radio"), Tab("tab"),
    Field("field"), Text("text"), Heading("heading"), Image("image"), List("list"), Slider("slider"), Control("control")
}

/** One visible control with its state. `id` is what the model sends back in an action. */
data class ScreenNode(
    val id: String,
    val label: String,
    val bounds: ScreenBounds,
    val role: NodeRole,
    val clickable: Boolean,
    val editable: Boolean = false,
    val scrollable: Boolean = false,
    val longClickable: Boolean = false,
    val checked: Boolean? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val focused: Boolean = false,
    val password: Boolean = false,
    val viewId: String? = null,
    /** Slider / progress position such as "40%" when the node reports a range. */
    val range: String? = null
) {
    val actionable: Boolean get() = clickable || editable || scrollable || longClickable || checked != null
}

/** Immutable copy of what is on screen. Never holds live `AccessibilityNodeInfo`. */
data class ScreenSnapshot(
    val packageName: String?,
    val nodes: List<ScreenNode>,
    val appLabel: String? = null,
    val keyboardShown: Boolean = false,
    /** Nodes the walker saw but dropped past its cap — the model is told there is more. */
    val truncated: Int = 0,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0
) {
    fun node(id: String): ScreenNode? = nodes.find { it.id == id }
    fun scrollables(): List<ScreenNode> = nodes.filter { it.scrollable }

    companion object {
        val Empty = ScreenSnapshot(null, emptyList())
    }
}
