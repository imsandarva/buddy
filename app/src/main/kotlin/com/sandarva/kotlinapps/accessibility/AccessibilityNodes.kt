package com.sandarva.kotlinapps.accessibility

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

/**
 * TalkBack / Voice Access snapshot helpers.
 * API 33+ needs an uninterruptible prefetch or a new window’s root looks empty.
 * [AccessibilityNodeInfo.recycle] is a no-op there — and harmful on some OEMs — so we skip it.
 */
object AccessibilityNodes {
    fun root(window: AccessibilityWindowInfo): AccessibilityNodeInfo? {
        val node = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            window.getRoot(SNAPSHOT_PREFETCH)
        } else window.root
        if (node != null && node.childCount == 0) node.refresh()
        return node
    }

    fun child(parent: AccessibilityNodeInfo, index: Int): AccessibilityNodeInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parent.getChild(index, SNAPSHOT_PREFETCH)
        } else parent.getChild(index)

    fun copy(node: AccessibilityNodeInfo): AccessibilityNodeInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) AccessibilityNodeInfo(node)
        else {
            @Suppress("DEPRECATION")
            AccessibilityNodeInfo.obtain(node)
        }

    fun recycle(node: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        @Suppress("DEPRECATION")
        node.recycle()
    }

    private val SNAPSHOT_PREFETCH =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID or
                AccessibilityNodeInfo.FLAG_PREFETCH_UNINTERRUPTIBLE
        } else 0
}
