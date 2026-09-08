package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.debug.BuddyLog

fun interface ScreenReader {
    fun snapshot(): ScreenSnapshot
}

/** Eyes implementation bound to the live accessibility service. */
class AccessibilityTreeReader(private val service: AccessibilityService) : ScreenReader {
    private val picker = WindowRootPicker(service)
    private val labels = HashMap<String, String>()

    override fun snapshot(): ScreenSnapshot {
        val metrics = service.resources.displayMetrics
        val walker = ScreenTreeWalker(metrics.widthPixels, metrics.heightPixels, service.getString(R.string.buddy_cursor_label))
        val picked = picker.pick()
        if (picked.roots.isEmpty()) {
            BuddyLog.d("Eyes.snapshot", "empty — no readable windows")
            return ScreenSnapshot.Empty
        }
        val nodes = ArrayList<ScreenNode>(80)
        var pkg: String? = null
        var pkgNodes = -1
        for (root in picked.roots) {
            val (nextPkg, nextNodes) = walker.collect(root)
            if (nextPkg != null && nextNodes.size > pkgNodes && nextPkg != service.packageName) {
                pkg = nextPkg
                pkgNodes = nextNodes.size
            } else if (pkg == null) pkg = nextPkg
            nodes += nextNodes
            AccessibilityNodes.recycle(root)
        }
        val snap = ScreenSnapshot(pkg, nodes.distinctBy { it.id }, appLabel(pkg), picked.keyboardShown, walker.truncated, metrics.widthPixels, metrics.heightPixels)
        BuddyLog.d("Eyes.snapshot", "app=${snap.appLabel} pkg=${snap.packageName} nodes=${snap.nodes.size} keyboard=${snap.keyboardShown} ids=${snap.nodes.take(16).joinToString { it.id }}")
        return snap
    }

    /** Human app name (“Settings”, not `com.android.settings`). Cached per package. */
    private fun appLabel(pkg: String?): String? {
        if (pkg.isNullOrBlank()) return null
        if (pkg == "com.android.systemui" || pkg.endsWith(".systemui")) return "Notifications and quick settings"
        return labels.getOrPut(pkg) {
            try {
                service.packageManager.getApplicationLabel(service.packageManager.getApplicationInfo(pkg, 0)).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        }
    }
}
