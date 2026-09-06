package com.sandarva.kotlinapps.accessibility

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/** Walks a live tree into value objects, then recycles every node. */
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
            val child = node.getChild(i) ?: continue
            walk(child, into, depth + 1)
            recycle(child)
        }
    }

    private fun consider(node: AccessibilityNodeInfo, into: ArrayList<ScreenNode>) {
        if (!node.isVisibleToUser) return
        node.getBoundsInScreen(rect)
        val bounds = ScreenBounds(rect.left, rect.top, rect.right, rect.bottom)
        if (bounds.width < 8 || bounds.height < 8) return
        if (isChrome(bounds)) return
        val viewId = shortViewId(node.viewIdResourceName)
        val label = labelOf(node, viewId) ?: return
        if (label.equals(skipLabel, ignoreCase = true)) return
        into += ScreenNode(uniqueId(viewId, label), label, bounds, node.isClickable, viewId)
    }

    private fun labelOf(node: AccessibilityNodeInfo, viewId: String?): String? {
        val raw = sequenceOf(node.text, node.contentDescription, hintOf(node))
            .map { it?.toString()?.trim() }
            .firstOrNull { !it.isNullOrBlank() }
        if (!raw.isNullOrBlank()) return raw.take(80)
        if (node.isClickable && !viewId.isNullOrBlank()) return viewId.replace('_', ' ')
        return null
    }

    private fun hintOf(node: AccessibilityNodeInfo): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText else null

    private fun uniqueId(viewId: String?, label: String): String {
        val base = viewId?.takeIf { it.isNotBlank() } ?: slug(label)
        if (usedIds.add(base)) return base
        var n = 2
        while (!usedIds.add("${base}_$n")) n += 1
        return "${base}_$n"
    }

    private fun isChrome(bounds: ScreenBounds): Boolean {
        if (screenW <= 0 || screenH <= 0) return false
        val coversWidth = bounds.width > screenW * 0.92f
        val coversHeight = bounds.height > screenH * 0.55f
        return coversWidth && coversHeight
    }

    private fun collapse(nodes: List<ScreenNode>): List<ScreenNode> = nodes.filter { node ->
        nodes.none { other ->
            other.id != node.id && other.label == node.label && other.clickable && !node.clickable && other.bounds.contains(node.bounds)
        }
    }

    private fun recycle(node: AccessibilityNodeInfo) {
        @Suppress("DEPRECATION")
        node.recycle()
    }

    companion object {
        private const val MAX_NODES = 72
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
