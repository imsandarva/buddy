package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.sandarva.kotlinapps.debug.BuddyLog

/**
 * TalkBack-style: the one screen the person is looking at.
 * Skip Buddy chrome (overlays, ask sheet). Keep the Buddy activity when it is in front.
 * Pick by z-order, not focused/active — the overlay used to leave Buddy “active” and hide other apps.
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
            val root = AccessibilityNodes.root(window) ?: continue
            val pkg = root.packageName?.toString().orEmpty()
            if (isChrome(window, pkg, screenW, screenH)) { AccessibilityNodes.recycle(root); continue }
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

    /**
     * [AccessibilityService.getWindows] is topmost-first. After chrome is stripped, the first
     * covering window is the app they opened — not whoever our overlay left “active”.
     */
    private fun pickFront(apps: ArrayList<Candidate>): List<Candidate> {
        if (apps.isEmpty()) return emptyList()
        val top = apps.firstOrNull { it.covering } ?: apps.first()
        val keep = apps.filter { it.pkg == top.pkg }
        apps.filter { it.pkg != top.pkg }.forEach { AccessibilityNodes.recycle(it.root) }
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
        if (isSystemUi(pkg) && !isLarge(root, screenW, screenH)) { AccessibilityNodes.recycle(root); return }
        if (pkg == service.packageName && !isLarge(root, screenW, screenH)) { AccessibilityNodes.recycle(root); return }
        into += Candidate(pkg, root, covering = isLarge(root, screenW, screenH))
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

    private fun candidate(
        window: AccessibilityWindowInfo,
        pkg: String,
        root: AccessibilityNodeInfo,
        screenW: Int,
        screenH: Int
    ): Candidate {
        val box = Rect()
        window.getBoundsInScreen(box)
        val covering = box.height() > screenH * COVER_H && box.width() > screenW * COVER_W
        return Candidate(pkg, root, covering)
    }

    private data class Candidate(
        val pkg: String,
        val root: AccessibilityNodeInfo,
        val covering: Boolean
    )

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
