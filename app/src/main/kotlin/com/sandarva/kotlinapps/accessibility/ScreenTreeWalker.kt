package com.sandarva.kotlinapps.accessibility

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Walks a live tree into value objects the way a person reads a screen:
 * a tappable row is one line ("Wi‑Fi · Connected"), the switch inside it is its own line
 * named after the row, headings and values stay as text, lists are marked scrollable.
 * Recycle is only for pre-API 33 pools.
 */
class ScreenTreeWalker(
    private val screenW: Int,
    private val screenH: Int,
    private val skipLabel: String
) {
    private val rect = Rect()
    private val usedIds = HashSet<String>()
    var truncated = 0
        private set

    fun collect(root: AccessibilityNodeInfo): Pair<String?, List<ScreenNode>> {
        val entries = ArrayList<Entry>(64)
        walk(root, entries, control = null, depth = 0)
        return root.packageName?.toString() to entries.map(::toNode)
    }

    private fun walk(node: AccessibilityNodeInfo, into: ArrayList<Entry>, control: Entry?, depth: Int) {
        if (depth > MAX_DEPTH || !shown(node)) return
        node.getBoundsInScreen(rect)
        val bounds = ScreenBounds(rect.left, rect.top, rect.right, rect.bottom)
        val tiny = bounds.width < 8 || bounds.height < 8
        val text = if (tiny) null else ownText(node)
        val editable = !tiny && (node.isEditable || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT })
        val checkable = !tiny && node.isCheckable
        val scroll = !tiny && node.isScrollable && !editable
        val pressable = !tiny && (node.isClickable || node.isLongClickable || checkable || editable)
        var nextControl = control
        when {
            scroll -> emit(into, entry(node, bounds, text, NodeRole.List, control)).also { nextControl = null }
            pressable && !isDimmer(bounds, text) -> {
                val made = entry(node, bounds, text, roleOf(node, text, editable, checkable), control)
                emit(into, made)
                nextControl = made
            }
            text != null -> {
                if (control != null) control.addDetail(text)
                else if (!repeatsLast(into, text, bounds)) emit(into, entry(node, bounds, text, if (isHeading(node)) NodeRole.Heading else NodeRole.Text, null))
            }
        }
        val count = node.childCount
        for (i in 0 until count) {
            val child = AccessibilityNodes.child(node, i) ?: continue
            walk(child, into, nextControl, depth + 1)
            AccessibilityNodes.recycle(child)
        }
    }

    private fun emit(into: ArrayList<Entry>, entry: Entry?) {
        if (entry == null) return
        if (into.size >= MAX_NODES) { truncated += 1; return }
        into += entry
    }

    private fun entry(node: AccessibilityNodeInfo, bounds: ScreenBounds, text: String?, role: NodeRole, parent: Entry?): Entry? {
        if (text != null && text.equals(skipLabel, ignoreCase = true)) return null
        return Entry(
            text = text, role = role, bounds = bounds, parent = parent,
            viewId = shortViewId(node.viewIdResourceName),
            clickable = node.isClickable || node.isCheckable,
            editable = role == NodeRole.Field,
            scrollable = role == NodeRole.List,
            longClickable = node.isLongClickable,
            checked = if (node.isCheckable) node.isChecked else null,
            selected = node.isSelected,
            enabled = node.isEnabled,
            focused = node.isFocused,
            password = node.isPassword,
            range = rangeOf(node)
        )
    }

    /** Compose / OEM overlays often lie with isVisibleToUser — Voice Access uses on-screen bounds too. */
    private fun shown(node: AccessibilityNodeInfo): Boolean {
        if (node.isVisibleToUser) return true
        node.getBoundsInScreen(rect)
        return rect.width() >= 8 && rect.height() >= 8 &&
            rect.left < screenW && rect.top < screenH && rect.right > 0 && rect.bottom > 0
    }

    private fun ownText(node: AccessibilityNodeInfo): String? =
        sequenceOf(node.text, node.contentDescription, hintOf(node), tooltipOf(node), stateOf(node))
            .map { it?.toString()?.trim() }
            .firstOrNull { !it.isNullOrBlank() }
            ?.take(MAX_LABEL)

    private fun roleOf(node: AccessibilityNodeInfo, text: String?, editable: Boolean, checkable: Boolean): NodeRole {
        val cls = node.className?.toString()?.substringAfterLast('.').orEmpty()
        return when {
            editable -> NodeRole.Field
            checkable && (cls.contains("Switch") || cls.contains("Toggle")) -> NodeRole.Switch
            checkable && cls.contains("Radio") -> NodeRole.Radio
            checkable && cls.contains("Tab") -> NodeRole.Tab
            checkable -> NodeRole.Checkbox
            cls.contains("Tab") -> NodeRole.Tab
            cls.contains("SeekBar") || cls.contains("Slider") || node.rangeInfo != null -> NodeRole.Slider
            cls.contains("Button") -> NodeRole.Button
            cls.contains("Image") && text == null -> NodeRole.Image
            cls.contains("Image") -> NodeRole.Button
            text != null -> NodeRole.Item
            else -> NodeRole.Control
        }
    }

    /** A clickable, unlabeled sheet covering most of the screen is a scrim, not a control. */
    private fun isDimmer(bounds: ScreenBounds, text: String?): Boolean =
        text == null && screenW > 0 && screenH > 0 && bounds.width > screenW * 0.92f && bounds.height > screenH * 0.55f

    /** A container that repeats the exact text of the child it wraps would otherwise show twice. */
    private fun repeatsLast(into: List<Entry>, text: String, bounds: ScreenBounds): Boolean {
        val last = into.lastOrNull() ?: return false
        return last.role == NodeRole.Text && last.text.equals(text, ignoreCase = true) && (last.bounds.contains(bounds) || bounds.contains(last.bounds))
    }

    private fun toNode(entry: Entry): ScreenNode {
        val title = entry.title()
        val label = entry.label(title)
        // A bare clickable layout whose name came from its children is a row, not an unnamed control.
        val role = if (entry.role == NodeRole.Control && title != null) NodeRole.Item else entry.role
        return ScreenNode(
            id = uniqueId(entry.viewId, label), label = label, bounds = entry.bounds, role = role,
            clickable = entry.clickable, editable = entry.editable, scrollable = entry.scrollable, longClickable = entry.longClickable,
            checked = entry.checked, selected = entry.selected, enabled = entry.enabled, focused = entry.focused,
            password = entry.password, viewId = entry.viewId, range = entry.range
        )
    }

    private fun uniqueId(viewId: String?, label: String): String {
        val base = viewId?.takeIf { it.isNotBlank() } ?: slug(label)
        if (usedIds.add(base)) return base
        var n = 2
        while (!usedIds.add("${base}_$n")) n += 1
        return "${base}_$n"
    }

    private fun hintOf(node: AccessibilityNodeInfo): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText else null

    private fun tooltipOf(node: AccessibilityNodeInfo): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) node.tooltipText else null

    private fun stateOf(node: AccessibilityNodeInfo): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) node.stateDescription else null

    private fun isHeading(node: AccessibilityNodeInfo): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && node.isHeading

    private fun rangeOf(node: AccessibilityNodeInfo): String? {
        val info = node.rangeInfo ?: return null
        val span = info.max - info.min
        if (span <= 0f) return null
        return "${(((info.current - info.min) / span) * 100f).toInt().coerceIn(0, 100)}%"
    }

    /** Mutable while walking; frozen into [ScreenNode] afterwards. */
    private class Entry(
        val text: String?,
        val role: NodeRole,
        val bounds: ScreenBounds,
        val parent: Entry?,
        val viewId: String?,
        val clickable: Boolean,
        val editable: Boolean,
        val scrollable: Boolean,
        val longClickable: Boolean,
        val checked: Boolean?,
        val selected: Boolean,
        val enabled: Boolean,
        val focused: Boolean,
        val password: Boolean,
        val range: String?
    ) {
        val details = ArrayList<String>(3)

        fun addDetail(value: String) {
            if (details.size >= MAX_DETAILS) return
            if (value.equals(text, ignoreCase = true) || details.any { it.equals(value, ignoreCase = true) }) return
            details += value
        }

        /** Own text, else the first child text, else the row it sits in (a bare switch inherits "Wi‑Fi"). */
        fun title(): String? = text ?: details.firstOrNull() ?: parent?.text ?: parent?.details?.firstOrNull()

        fun label(title: String?): String {
            val rest = if (text == null) details.drop(1) else details
            val head = title ?: viewId?.replace('_', ' ') ?: when (role) {
                NodeRole.Field -> "text field"
                NodeRole.List -> "list"
                NodeRole.Image -> "picture"
                else -> role.word
            }
            if (rest.isEmpty() || role == NodeRole.List) return head
            return (head + " · " + rest.joinToString(" · ") { it.take(MAX_DETAIL) }).take(MAX_LINE)
        }
    }

    companion object {
        private const val MAX_NODES = 160
        private const val MAX_DEPTH = 32
        private const val MAX_DETAILS = 3
        private const val MAX_LABEL = 80
        private const val MAX_DETAIL = 44
        private const val MAX_LINE = 120

        fun shortViewId(raw: String?): String? = raw?.substringAfterLast('/')?.takeIf { it.isNotBlank() }

        fun slug(label: String): String {
            val compact = buildString(label.length) {
                var gap = false
                for (ch in label.lowercase()) {
                    if (ch in 'a'..'z' || ch in '0'..'9') {
                        if (gap && isNotEmpty()) append('_')
                        append(ch)
                        gap = false
                    } else gap = true
                }
            }
            return compact.take(40).ifBlank { "node" }
        }
    }
}
