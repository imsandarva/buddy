package com.sandarva.kotlinapps.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils

/** Reads the system grant and opens the Buddy Assistant settings page. */
object AccessibilityController {
    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val target = ComponentName(context, BuddyAccessibilityService::class.java)
        return TextUtils.SimpleStringSplitter(':').let { splitter ->
            splitter.setString(enabled)
            splitter.any { ComponentName.unflattenFromString(it) == target }
        }
    }

    fun requestAccess(context: Context) {
        AccessibilitySession.setAwaitingGrant(true)
        openSettings(context)
    }

    fun onHostResumed(context: Context) {
        refresh(context)
        if (!AccessibilitySession.awaitingGrant.value) return
        if (isEnabled(context)) AccessibilitySession.setAwaitingGrant(false)
    }

    fun refresh(context: Context) {
        val on = isEnabled(context)
        AccessibilitySession.setEnabled(on)
        if (!on) AccessibilitySession.setBound(false)
    }

    fun openSettings(context: Context) {
        val component = ComponentName(context, BuddyAccessibilityService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val details = Intent(DETAILS_ACTION).putExtra(Intent.EXTRA_COMPONENT_NAME, component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(details)
                return
            } catch (_: Exception) { /* some OEMs only honor the list screen */ }
        }
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private const val DETAILS_ACTION = "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
}
