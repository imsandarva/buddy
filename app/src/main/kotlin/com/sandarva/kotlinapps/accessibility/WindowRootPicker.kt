package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.sandarva.kotlinapps.debug.BuddyLog

/**
 * TalkBack-style: the windows the person can actually see.
 * Skip Buddy chrome. Skip the thin status/nav strips. Keep a pulled-down shade.
 */
class WindowRootPicker(private val service: AccessibilityService) {
    fun roots(): List<AccessibilityNodeInfo> {
        val self = service.packageName
        val metrics = service.resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels
        val windows = allWindows()
        val apps = ArrayList<Candidate>(8)
        val shade = ArrayList<Candidate>(2)
        for (window in windows) {
            if (!readable(window.type)) continue
            val root = window.root ?: continue
            val pkg = root.packageName?.toString().orEmpty()
            if (pkg == self) { recycle(root); continue }
            when {
                isCoveringShade(window, pkg, screenW, screenH) -> shade += Candidate(pkg, root)
                isSystemUi(pkg) -> recycle(root)
                else -> apps += Candidate(pkg, root)
            }
        }
        val chosen = if (shade.isNotEmpty()) {
            apps.forEach { recycle(it.root) }
            shade
        } else apps
        if (chosen.isEmpty()) addFallback(chosen, self, screenW, screenH)
        BuddyLog.d("Eyes.windows", "raw=${windows.size} kept=${chosen.size} pkgs=${chosen.map { it.pkg }} shade=${shade.size} self=$self")
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

    private fun addFallback(into: ArrayList<Candidate>, self: String, screenW: Int, screenH: Int) {
        val root = service.rootInActiveWindow ?: return
        val pkg = root.packageName?.toString().orEmpty()
        if (pkg == self) { recycle(root); return }
        if (isSystemUi(pkg) && !isLarge(root, screenW, screenH)) { recycle(root); return }
        into += Candidate(pkg, root)
    }

    private fun readable(type: Int): Boolean =
        type == AccessibilityWindowInfo.TYPE_APPLICATION || type == AccessibilityWindowInfo.TYPE_SYSTEM

    private fun isCoveringShade(window: AccessibilityWindowInfo, pkg: String, screenW: Int, screenH: Int): Boolean {
        if (!isSystemUi(pkg)) return false
        val box = Rect()
        window.getBoundsInScreen(box)
        return box.height() > screenH * SHADE_MIN_H && box.width() > screenW * SHADE_MIN_W
    }

    private fun isLarge(root: AccessibilityNodeInfo, screenW: Int, screenH: Int): Boolean {
        val box = Rect()
        root.getBoundsInScreen(box)
        return box.height() > screenH * SHADE_MIN_H && box.width() > screenW * SHADE_MIN_W
    }

    private fun isSystemUi(pkg: String) = pkg == "com.android.systemui" || pkg.endsWith(".systemui")

    private fun recycle(node: AccessibilityNodeInfo) {
        @Suppress("DEPRECATION")
        node.recycle()
    }

    private data class Candidate(val pkg: String, val root: AccessibilityNodeInfo)

    companion object {
        private const val SHADE_MIN_H = 0.35f
        private const val SHADE_MIN_W = 0.55f
    }
}
