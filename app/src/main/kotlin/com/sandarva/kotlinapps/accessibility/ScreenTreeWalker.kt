package com.sandarva.kotlinapps.accessibility

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/** Walks a live tree into value objects. Recycle is only for pre-API 33 pools. */
class ScreenTreeWalker(
    private val screenW: Int,
    private val screenH: Int,
    private val skipLabel: String
) {
    private val rect = Rect()
    private val usedIds = HashSet<String>()

    fun collect(root: AccessibilityNodeInfo): Pair<String?, List<ScreenNode>> {
        val nodes = ArrayList<ScreenNode>(48)
        walk(root, nodes, 0)
        return root.packageName?.toString() to collapse(nodes)
    }

    private fun walk(node: AccessibilityNodeInfo, into: ArrayList<ScreenNode>, depth: Int) {
        if (into.size >= MAX_NODES || depth > MAX_DEPTH) return
        consider(node, into)
        val count = node.childCount
        for (i in 0 until count) {
            val child = AccessibilityNodes.child(node, i) ?: continue
            walk(child, into, depth + 1)
            AccessibilityNodes.recycle(child)
        }
    }

    private fun consider(node: AccessibilityNodeInfo, into: ArrayList<ScreenNode>) {
        if (!shown(node)) return
        node.getBoundsInScreen(rect)
        val bounds = ScreenBounds(rect.left, rect.top, rect.right, rect.bottom)
        if (bounds.width < 8 || bounds.height < 8) return
        val viewId = shortViewId(node.viewIdResourceName)
        val editable = node.isEditable || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
        val clickable = node.isClickable || node.isCheckable
        val label = labelOf(node, viewId) ?: when {
            editable -> "text field"
            clickable -> viewId?.replace('_', ' ') ?: roleLabel(node)
            else -> return
        }
        if (isBlankChrome(bounds, clickable, editable)) return
        if (label.equals(skipLabel, ignoreCase = true)) return
        into += ScreenNode(uniqueId(viewId, label), label, bounds, clickable, viewId, editable)
    }

    /** Compose / OEM overlays often lie with isVisibleToUser — Voice Access uses on-screen bounds too. */
    private fun shown(node: AccessibilityNodeInfo): Boolean {
        if (node.isVisibleToUser) return true
        node.getBoundsInScreen(rect)
        return rect.width() >= 8 && rect.height() >= 8 &&
            rect.left < screenW && rect.top < screenH && rect.right > 0 && rect.bottom > 0
    }

    private fun labelOf(node: AccessibilityNodeInfo, viewId: String?): String? {
        val raw = sequenceOf(node.text, node.contentDescription, hintOf(node), tooltipOf(node), stateOf(node))
            .map { it?.toString()?.trim() }
            .firstOrNull { !it.isNullOrBlank() }
        if (!raw.isNullOrBlank()) return raw.take(80)
        if (node.isClickable && !viewId.isNullOrBlank()) return viewId.replace('_', ' ')
        return null
    }

    private fun hintOf(node: AccessibilityNodeInfo): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText else null

    private fun tooltipOf(node: AccessibilityNodeInfo): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) node.tooltipText else null

    private fun stateOf(node: AccessibilityNodeInfo): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) node.stateDescription else null

    private fun uniqueId(viewId: String?, label: String): String {
        val base = viewId?.takeIf { it.isNotBlank() } ?: slug(label)
        if (usedIds.add(base)) return base
        var n = 2
        while (!usedIds.add("${base}_$n")) n += 1
        return "${base}_$n"
    }

    private fun isBlankChrome(bounds: ScreenBounds, clickable: Boolean, editable: Boolean): Boolean {
        if (clickable || editable) return false
        if (screenW <= 0 || screenH <= 0) return false
        return bounds.width > screenW * 0.92f && bounds.height > screenH * 0.55f
    }

    private fun roleLabel(node: AccessibilityNodeInfo): String {
        val simple = node.className?.toString()?.substringAfterLast('.') ?: return "control"
        val spaced = buildString(simple.length + 4) {
            simple.forEachIndexed { i, ch ->
                if (i > 0 && ch.isUpperCase()) append(' ')
                append(ch.lowercaseChar())
            }
        }
        return spaced.ifBlank { "control" }
    }

    private fun collapse(nodes: List<ScreenNode>): List<ScreenNode> = nodes.filter { node ->
        if (node.editable) return@filter true
        nodes.none { other ->
            other.id != node.id && other.label == node.label && other.clickable && !node.clickable && other.bounds.contains(node.bounds)
        }
    }

    companion object {
        private const val MAX_NODES = 160
        private const val MAX_DEPTH = 28

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
