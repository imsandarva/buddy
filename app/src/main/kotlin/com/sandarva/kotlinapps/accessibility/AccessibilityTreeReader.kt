package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.sandarva.kotlinapps.R

fun interface ScreenReader {
    fun snapshot(): ScreenSnapshot
}

/** Eyes implementation bound to the live accessibility service. */
class AccessibilityTreeReader(private val service: AccessibilityService) : ScreenReader {
    override fun snapshot(): ScreenSnapshot {
        val metrics = service.resources.displayMetrics
        val walker = ScreenTreeWalker(metrics.widthPixels, metrics.heightPixels, service.getString(R.string.buddy_cursor_label))
        val roots = preferredRoots()
        if (roots.isEmpty()) return ScreenSnapshot.Empty
        val nodes = ArrayList<ScreenNode>(48)
        var pkg: String? = null
        for (root in roots) {
            val (nextPkg, nextNodes) = walker.collect(root)
            if (pkg == null) pkg = nextPkg
            nodes += nextNodes
            recycle(root)
        }
        return ScreenSnapshot(pkg, nodes.distinctBy { it.id })
    }

    private fun preferredRoots(): List<AccessibilityNodeInfo> {
        val windows = service.windows
        if (windows.isNullOrEmpty()) return listOfNotNull(service.rootInActiveWindow)
        val apps = ArrayList<Pair<Boolean, AccessibilityNodeInfo>>(windows.size)
        for (window in windows) {
            if (window.type != AccessibilityWindowInfo.TYPE_APPLICATION) continue
            val root = window.root ?: continue
            val pkg = root.packageName?.toString().orEmpty()
            if (isSystemUi(pkg)) {
                recycle(root)
                continue
            }
            apps += window.isActive to root
        }
        val active = apps.filter { it.first }
        val chosen = active.ifEmpty { apps }
        if (active.isNotEmpty()) apps.filter { !it.first }.forEach { recycle(it.second) }
        return chosen.map { it.second }
    }

    private fun isSystemUi(pkg: String) = pkg == "com.android.systemui" || pkg.endsWith(".systemui")

    private fun recycle(node: AccessibilityNodeInfo) {
        @Suppress("DEPRECATION")
        node.recycle()
    }
}
