package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.sandarva.kotlinapps.debug.BuddyLog

/**
 * TalkBack-style: the screen the person is looking at.
 * The front covering app window is the scene; every readable window stacked above it
 * (its own dialogs, a permission prompt from another package, a popup menu) is part of it.
 * Buddy chrome is skipped; the Buddy activity is kept when it is in front.
 * Pick by z-order, not focused/active — the overlay used to leave Buddy “active” and hide other apps.
 */
class WindowRootPicker(private val service: AccessibilityService) {
    class Roots(val roots: List<AccessibilityNodeInfo>, val keyboardShown: Boolean)

    fun roots(): List<AccessibilityNodeInfo> = pick().roots

    fun pick(): Roots {
        val metrics = service.resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels
        val windows = allWindows()
        val keyboard = windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        val apps = ArrayList<Candidate>(8)
        val shade = ArrayList<Candidate>(2)
        for (window in windows) {
            if (!readable(window.type)) continue
            val root = AccessibilityNodes.root(window) ?: continue
            val pkg = root.packageName?.toString().orEmpty()
            if (isChrome(window, pkg, root, screenW, screenH)) { AccessibilityNodes.recycle(root); continue }
            when {
                isCoveringShade(window, pkg, screenW, screenH) -> shade += candidate(window, pkg, root, screenW, screenH)
                isSystemUi(pkg) -> AccessibilityNodes.recycle(root)
                else -> apps += candidate(window, pkg, root, screenW, screenH)
            }
        }
        val chosen = if (shade.isNotEmpty()) {
            apps.forEach { AccessibilityNodes.recycle(it.root) }
            shade
        } else pickFront(apps)
        val result = chosen.ifEmpty { fallback(screenW, screenH) }
        BuddyLog.d("Eyes.windows", "raw=${windows.size} kept=${result.size} pkgs=${result.map { it.pkg }} shade=${shade.size} keyboard=$keyboard")
        return Roots(result.map { it.root }, keyboard)
    }

    private fun allWindows(): List<AccessibilityWindowInfo> {
        val primary = service.windows.orEmpty()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return primary
        val seen = HashSet<Int>(primary.size)
        val merged = ArrayList<AccessibilityWindowInfo>(primary.size)
        for (window in primary) if (seen.add(window.id)) merged += window
        val displays = service.windowsOnAllDisplays
        for (i in 0 until displays.size()) {
            val list = displays.valueAt(i) ?: continue
            for (window in list) if (seen.add(window.id)) merged += window
        }
        return merged
    }

    /**
     * [AccessibilityService.getWindows] is topmost-first. The first covering window is the app
     * they opened; everything above it in z-order (dialogs, prompts, menus) is visible too.
     * Windows beneath the covering one are hidden and dropped.
     */
    private fun pickFront(apps: ArrayList<Candidate>): List<Candidate> {
        if (apps.isEmpty()) return emptyList()
        val topIndex = apps.indexOfFirst { it.covering }.takeIf { it >= 0 } ?: 0
        val keep = apps.subList(0, topIndex + 1).toList()
        apps.drop(topIndex + 1).forEach { AccessibilityNodes.recycle(it.root) }
        return keep
    }

    private fun fallback(screenW: Int, screenH: Int): List<Candidate> {
        val root = service.rootInActiveWindow ?: return emptyList()
        val pkg = root.packageName?.toString().orEmpty()
        val large = isLarge(root, screenW, screenH)
        if ((isSystemUi(pkg) || pkg == service.packageName) && !large) { AccessibilityNodes.recycle(root); return emptyList() }
        return listOf(Candidate(pkg, root, covering = large))
    }

    private fun readable(type: Int): Boolean =
        type == AccessibilityWindowInfo.TYPE_APPLICATION || type == AccessibilityWindowInfo.TYPE_SYSTEM

    /** Overlays, the keyboard, and slim or emptied Buddy chrome — not the Buddy home screen. */
    private fun isChrome(window: AccessibilityWindowInfo, pkg: String, root: AccessibilityNodeInfo, screenW: Int, screenH: Int): Boolean {
        if (window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) return true
        if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) return true
        if (pkg != service.packageName) return false
        if (hollow(root)) return true
        val box = Rect()
        window.getBoundsInScreen(box)
        val shortBar = box.height() < screenH * CHROME_H && box.width() > screenW * CHROME_W
        val tiny = box.width() < screenW * TINY_W && box.height() < screenH * TINY_H
        return shortBar || tiny
    }

    /** A Buddy window that hides its descendants from accessibility (cursor, ask sheet) reads as an empty shell. */
    private fun hollow(root: AccessibilityNodeInfo): Boolean {
        if (root.childCount == 0) return true
        if (root.childCount > 1) return false
        val only = AccessibilityNodes.child(root, 0) ?: return true
        val empty = only.childCount == 0 && only.text.isNullOrBlank() && only.contentDescription.isNullOrBlank()
        AccessibilityNodes.recycle(only)
        return empty
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

    private fun candidate(window: AccessibilityWindowInfo, pkg: String, root: AccessibilityNodeInfo, screenW: Int, screenH: Int): Candidate {
        val box = Rect()
        window.getBoundsInScreen(box)
        return Candidate(pkg, root, covering = box.height() > screenH * COVER_H && box.width() > screenW * COVER_W)
    }

    private class Candidate(val pkg: String, val root: AccessibilityNodeInfo, val covering: Boolean)

    companion object {
        private const val SHADE_MIN_H = 0.35f
        private const val SHADE_MIN_W = 0.55f
        private const val CHROME_H = 0.34f
        private const val CHROME_W = 0.40f
        private const val TINY_W = 0.28f
        private const val TINY_H = 0.22f
        private const val COVER_H = 0.45f
        private const val COVER_W = 0.55f
    }
}
