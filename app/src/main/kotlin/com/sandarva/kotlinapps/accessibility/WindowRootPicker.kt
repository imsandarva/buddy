package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.sandarva.kotlinapps.debug.BuddyLog

/**
 * TalkBack-style: the one screen the person is looking at.
 * Skip Buddy chrome (overlays, the live pill). Keep the Buddy activity when it is in front.
 * Do not union the launcher under an open app — that is how we used to “see” the drawer after opening Buddy.
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
            if (isChrome(window, pkg, screenW, screenH)) { recycle(root); continue }
            when {
                isCoveringShade(window, pkg, screenW, screenH) -> shade += candidate(window, pkg, root)
                isSystemUi(pkg) -> recycle(root)
                else -> apps += candidate(window, pkg, root)
            }
        }
        val chosen = if (shade.isNotEmpty()) {
            apps.forEach { recycle(it.root) }
            shade
        } else pickFront(apps)
        val result = chosen.ifEmpty { fallback(screenW, screenH) }
        BuddyLog.d("Eyes.windows", "raw=${windows.size} kept=${result.size} pkgs=${result.map { it.pkg }} shade=${shade.size} self=$self")
        return result.map { it.root }
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

    private fun pickFront(apps: ArrayList<Candidate>): List<Candidate> {
        if (apps.isEmpty()) return emptyList()
        val focused = apps.filter { it.focused }
        val pool = focused.ifEmpty { apps.filter { it.active } }.ifEmpty { apps }
        val top = pool.maxByOrNull { it.layer } ?: return emptyList()
        val keep = apps.filter { it.pkg == top.pkg }
        apps.filter { it.pkg != top.pkg }.forEach { recycle(it.root) }
        return keep
    }

    private fun fallback(screenW: Int, screenH: Int): List<Candidate> {
        val into = ArrayList<Candidate>(1)
        addFallback(into, screenW, screenH)
        return into
    }

    private fun addFallback(into: ArrayList<Candidate>, screenW: Int, screenH: Int) {
        val root = service.rootInActiveWindow ?: return
        val pkg = root.packageName?.toString().orEmpty()
        if (isSystemUi(pkg) && !isLarge(root, screenW, screenH)) { recycle(root); return }
        into += Candidate(pkg, root, 0, focused = true, active = true)
    }

    private fun readable(type: Int): Boolean =
        type == AccessibilityWindowInfo.TYPE_APPLICATION || type == AccessibilityWindowInfo.TYPE_SYSTEM

    /** Overlays and slim Buddy chrome — not the Buddy home screen. */
    private fun isChrome(window: AccessibilityWindowInfo, pkg: String, screenW: Int, screenH: Int): Boolean {
        if (window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) return true
        if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) return true
        if (pkg != service.packageName) return false
        val box = Rect()
        window.getBoundsInScreen(box)
        val shortBar = box.height() < screenH * CHROME_H && box.width() > screenW * CHROME_W
        val tiny = box.width() < screenW * TINY_W && box.height() < screenH * TINY_H
        return shortBar || tiny
    }

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

    private fun candidate(window: AccessibilityWindowInfo, pkg: String, root: AccessibilityNodeInfo) =
        Candidate(pkg, root, window.layer, window.isFocused, window.isActive)

    private fun recycle(node: AccessibilityNodeInfo) {
        @Suppress("DEPRECATION")
        node.recycle()
    }

    private data class Candidate(
        val pkg: String,
        val root: AccessibilityNodeInfo,
        val layer: Int,
        val focused: Boolean,
        val active: Boolean
    )

    companion object {
        private const val SHADE_MIN_H = 0.35f
        private const val SHADE_MIN_W = 0.55f
        private const val CHROME_H = 0.34f
        private const val CHROME_W = 0.40f
        private const val TINY_W = 0.28f
        private const val TINY_H = 0.22f
    }
}
