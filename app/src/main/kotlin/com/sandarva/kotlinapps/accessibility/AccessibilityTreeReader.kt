package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.debug.BuddyLog

fun interface ScreenReader {
    fun snapshot(): ScreenSnapshot
}

/** Eyes implementation bound to the live accessibility service. */
class AccessibilityTreeReader(private val service: AccessibilityService) : ScreenReader {
    private val picker = WindowRootPicker(service)

    override fun snapshot(): ScreenSnapshot {
        val metrics = service.resources.displayMetrics
        val walker = ScreenTreeWalker(metrics.widthPixels, metrics.heightPixels, service.getString(R.string.buddy_cursor_label))
        val roots = picker.roots()
        if (roots.isEmpty()) {
            BuddyLog.d("Eyes.snapshot", "empty — no readable windows")
            return ScreenSnapshot.Empty
        }
        val nodes = ArrayList<ScreenNode>(80)
        var pkg: String? = null
        var pkgNodes = -1
        for (root in roots) {
            val (nextPkg, nextNodes) = walker.collect(root)
            if (nextPkg != null && nextNodes.size > pkgNodes && nextPkg != service.packageName) {
                pkg = nextPkg
                pkgNodes = nextNodes.size
            } else if (pkg == null) pkg = nextPkg
            nodes += nextNodes
            recycle(root)
        }
        val snap = ScreenSnapshot(pkg, nodes.distinctBy { it.id })
        BuddyLog.d("Eyes.snapshot", "pkg=${snap.packageName} nodes=${snap.nodes.size} ids=${snap.nodes.take(16).joinToString { it.id }}")
        return snap
    }

    private fun recycle(node: AccessibilityNodeInfo) {
        @Suppress("DEPRECATION")
        node.recycle()
    }
}
