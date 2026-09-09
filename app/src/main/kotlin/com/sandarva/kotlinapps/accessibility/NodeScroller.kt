package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.hypot

/**
 * TalkBack-style list motion: ask the node to scroll itself before injecting a finger.
 * RecyclerView, Gallery grids, ViewPager, and Compose lists honor this; canvas viewers do not.
 */
class NodeScroller(private val service: AccessibilityService) {
    private val picker = WindowRootPicker(service)
    private val main = Handler(Looper.getMainLooper())

    suspend fun scroll(direction: Direction, bounds: ScreenBounds?, viewId: String?): Boolean {
        val ok = onMain { scrollNow(direction, bounds, viewId) } == true
        BuddyLog.d("Hands.nodeScroll", "ok=$ok dir=${direction.word} viewId=$viewId")
        return ok
    }

    private fun scrollNow(direction: Direction, bounds: ScreenBounds?, viewId: String?): Boolean {
        val node = find(bounds, viewId) ?: return false
        try {
            return scrollChain(node, direction)
        } finally {
            AccessibilityNodes.recycle(node)
        }
    }

    private fun scrollChain(start: AccessibilityNodeInfo, direction: Direction): Boolean {
        var current: AccessibilityNodeInfo? = AccessibilityNodes.copy(start)
        var depth = 0
        try {
            while (current != null && depth < MAX_ANCESTORS) {
                if (canScroll(current) && tryScroll(current, direction)) return true
                val parent = current.parent
                AccessibilityNodes.recycle(current)
                current = parent
                depth += 1
            }
            return false
        } finally {
            if (current != null) AccessibilityNodes.recycle(current)
        }
    }

    private fun tryScroll(node: AccessibilityNodeInfo, direction: Direction): Boolean {
        val directional = actionFor(direction)
        if (hasAction(node, directional) && node.performAction(directional)) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val args = Bundle().apply { putInt(SCROLL_DIRECTION_ARG, focusFor(direction)) }
            if (node.performAction(android.R.id.accessibilityActionScrollInDirection, args)) return true
        }
        val axis = if (direction == Direction.Down || direction == Direction.Right) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return node.performAction(axis)
    }

    private fun find(bounds: ScreenBounds?, viewId: String?): AccessibilityNodeInfo? {
        val roots = picker.roots()
        if (roots.isEmpty()) return null
        val found = ArrayList<Hit>(8)
        for (root in roots) collect(root, found)
        val hit = pick(found, bounds, viewId)
        val keep = hit?.let { AccessibilityNodes.copy(it.node) }
        for (item in found) AccessibilityNodes.recycle(item.node)
        for (root in roots) AccessibilityNodes.recycle(root)
        return keep
    }

    private fun collect(node: AccessibilityNodeInfo, into: ArrayList<Hit>) {
        if (canScroll(node)) {
            val box = Rect()
            node.getBoundsInScreen(box)
            if (box.width() >= 24 && box.height() >= 24) {
                into += Hit(
                    node = AccessibilityNodes.copy(node),
                    viewId = ScreenTreeWalker.shortViewId(node.viewIdResourceName),
                    bounds = ScreenBounds(box.left, box.top, box.right, box.bottom)
                )
            }
        }
        val count = node.childCount
        for (i in 0 until count) {
            val child = AccessibilityNodes.child(node, i) ?: continue
            collect(child, into)
            AccessibilityNodes.recycle(child)
        }
    }

    private fun pick(hits: List<Hit>, bounds: ScreenBounds?, viewId: String?): Hit? {
        if (hits.isEmpty()) return null
        viewId?.let { id -> hits.find { it.viewId == id } }?.let { return it }
        if (bounds != null) {
            hits.filter { overlap(it.bounds, bounds) }
                .minByOrNull { hypot(it.bounds.centerX - bounds.centerX, it.bounds.centerY - bounds.centerY) }
                ?.let { return it }
        }
        return hits.maxByOrNull { it.bounds.width.toLong() * it.bounds.height }
    }

    private fun canScroll(node: AccessibilityNodeInfo): Boolean {
        if (node.isEditable) return false
        if (node.isScrollable) return true
        return node.actionList.any { it.id in SCROLL_ACTIONS }
    }

    private fun hasAction(node: AccessibilityNodeInfo, id: Int): Boolean =
        node.actionList.any { it.id == id }

    private fun actionFor(direction: Direction): Int = when (direction) {
        Direction.Up -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id
        Direction.Down -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id
        Direction.Left -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id
        Direction.Right -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id
    }

    private fun focusFor(direction: Direction): Int = when (direction) {
        Direction.Up -> View.FOCUS_UP
        Direction.Down -> View.FOCUS_DOWN
        Direction.Left -> View.FOCUS_LEFT
        Direction.Right -> View.FOCUS_RIGHT
    }

    private fun overlap(a: ScreenBounds, b: ScreenBounds): Boolean {
        val x = abs(a.centerX - b.centerX) < (a.width + b.width) / 2f
        val y = abs(a.centerY - b.centerY) < (a.height + b.height) / 2f
        return x && y
    }

    private suspend fun <T> onMain(block: () -> T): T? = suspendCancellableCoroutine { cont ->
        val run = Runnable { if (cont.isActive) cont.resume(block()) }
        if (Looper.myLooper() == Looper.getMainLooper()) run.run() else main.post(run)
        cont.invokeOnCancellation { main.removeCallbacks(run) }
    }

    private data class Hit(val node: AccessibilityNodeInfo, val viewId: String?, val bounds: ScreenBounds)

    private companion object {
        const val MAX_ANCESTORS = 8
        const val SCROLL_DIRECTION_ARG = "android.view.accessibility.action.ARGUMENT_DIRECTION_INT"
        val SCROLL_ACTIONS = intArrayOf(
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id,
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id
        )
    }
}
