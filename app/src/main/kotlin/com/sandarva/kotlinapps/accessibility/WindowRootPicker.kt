package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.sandarva.kotlinapps.debug.BuddyLog

/**
 * The overlay is often the “active” window after a double-tap, so we never
 * snapshot only that — TalkBack-style: every real app window, skip our chrome.
 */
class WindowRootPicker(private val service: AccessibilityService) {
    fun roots(): List<AccessibilityNodeInfo> {
        val self = service.packageName
        val metrics = service.resources.displayMetrics
        val windows = allWindows()
        val candidates = ArrayList<Candidate>(8)
        for (window in windows) {
            if (!readable(window.type)) continue
            val root = window.root ?: continue
            val pkg = root.packageName?.toString().orEmpty()
            if (isSystemUi(pkg) || isOwnOverlay(window, pkg, self, metrics.widthPixels, metrics.heightPixels)) {
                recycle(root)
                continue
            }
            candidates += Candidate(pkg, root)
        }
        if (candidates.isEmpty()) addFallback(candidates, self)
        val foreign = candidates.any { it.pkg.isNotBlank() && it.pkg != self }
        val chosen = if (foreign) {
            candidates.filter { it.pkg == self }.forEach { recycle(it.root) }
            candidates.filter { it.pkg != self }
        } else candidates
        BuddyLog.d("Eyes.windows", "raw=${windows.size} kept=${chosen.size} pkgs=${chosen.map { it.pkg }} self=$self")
        return chosen.map { it.root }
    }

    private fun allWindows(): List<AccessibilityWindowInfo> {
        val primary = service.windows.orEmpty()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return primary
        val seen = HashSet<Int>(primary.size)
        val merged = ArrayList<AccessibilityWindowInfo>(primary.size)
        for (window in primary) {
            if (seen.add(window.id)) merged += window
        }
        val displays = service.windowsOnAllDisplays
        for (i in 0 until displays.size()) {
            val list = displays.valueAt(i) ?: continue
            for (window in list) if (seen.add(window.id)) merged += window
        }
        return merged
    }

    private fun addFallback(into: ArrayList<Candidate>, self: String) {
        val root = service.rootInActiveWindow ?: return
        val pkg = root.packageName?.toString().orEmpty()
        if (isSystemUi(pkg) || pkg == self) {
            recycle(root)
            return
        }
        into += Candidate(pkg, root)
    }

    private fun readable(type: Int): Boolean =
        type == AccessibilityWindowInfo.TYPE_APPLICATION || type == AccessibilityWindowInfo.TYPE_SYSTEM

    private fun isOwnOverlay(window: AccessibilityWindowInfo, pkg: String, self: String, screenW: Int, screenH: Int): Boolean {
        if (pkg != self) return false
        val box = Rect()
        window.getBoundsInScreen(box)
        return box.width() < screenW * 0.4f && box.height() < screenH * 0.4f
    }

    private fun isSystemUi(pkg: String) = pkg == "com.android.systemui" || pkg.endsWith(".systemui")

    private fun recycle(node: AccessibilityNodeInfo) {
        @Suppress("DEPRECATION")
        node.recycle()
    }

    private data class Candidate(val pkg: String, val root: AccessibilityNodeInfo)
}
