package com.sandarva.kotlinapps.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/** Reads accessibility state and opens the system settings screen for this app. */
class AccessibilityController(private val context: Context) {

    private val serviceComponent = ComponentName(context, BuddyAccessibilityService::class.java)

    fun isEnabled(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return TextUtils.SimpleStringSplitter(':').let { splitter ->
            splitter.setString(enabled)
            splitter.any { ComponentName.unflattenFromString(it) == serviceComponent }
        }
    }

    fun openSettings() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
