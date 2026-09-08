package com.sandarva.kotlinapps.brain.goal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.sandarva.kotlinapps.debug.BuddyLog

/** Opens a launcher app by its visible name — no drawer hunt. */
class AppLauncher(private val context: Context) {
    fun open(name: String): String {
        val query = name.trim()
        if (query.isBlank()) return "open_app failed — empty name"
        val pm = context.packageManager
        val apps = launcherApps(pm)
        val hit = pick(apps, pm, query)
        if (hit == null) {
            BuddyLog.d("Goal.openApp", "miss name=\"$query\" n=${apps.size}")
            return "app not found: $query"
        }
        val pkg = hit.activityInfo.packageName
        val launch = pm.getLaunchIntentForPackage(pkg)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(pkg, hit.activityInfo.name)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            context.startActivity(launch)
            val label = hit.loadLabel(pm).toString()
            BuddyLog.d("Goal.openApp", "ok name=\"$query\" pkg=$pkg")
            "opened $label"
        } catch (error: Exception) {
            BuddyLog.e("Goal.openApp", error.message ?: "start failed", error)
            "open_app failed: $query"
        }
    }

    private fun launcherApps(pm: PackageManager): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }

    private fun pick(apps: List<ResolveInfo>, pm: PackageManager, query: String): ResolveInfo? {
        val labeled = apps.map { it to it.loadLabel(pm).toString().trim() }
        return labeled.firstOrNull { it.second.equals(query, ignoreCase = true) }?.first
            ?: labeled.firstOrNull { it.second.startsWith(query, ignoreCase = true) }?.first
            ?: labeled.filter { it.second.contains(query, ignoreCase = true) }.minByOrNull { it.second.length }?.first
    }
}
