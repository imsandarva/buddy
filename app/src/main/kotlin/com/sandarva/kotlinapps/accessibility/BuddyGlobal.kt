package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import com.sandarva.kotlinapps.debug.BuddyLog

/**
 * The phone's own buttons — Back, Home, Recents, the notification shade, quick settings.
 * Same path TalkBack and Voice Access use (`performGlobalAction`); no gesture, no overlay pass-through.
 */
object BuddyGlobal {
    enum class Key(val action: Int, val word: String) {
        Back(AccessibilityService.GLOBAL_ACTION_BACK, "back"),
        Home(AccessibilityService.GLOBAL_ACTION_HOME, "home"),
        Recents(AccessibilityService.GLOBAL_ACTION_RECENTS, "recent apps"),
        Notifications(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "notifications"),
        QuickSettings(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, "quick settings")
    }

    @Volatile private var service: AccessibilityService? = null

    fun attach(next: AccessibilityService) { service = next }
    fun detach(current: AccessibilityService) { if (service === current) service = null }
    fun isReady(): Boolean = service != null

    fun press(key: Key): Boolean {
        val host = service ?: return false
        val ok = host.performGlobalAction(key.action)
        BuddyLog.d("Global.press", "key=$key ok=$ok")
        return ok
    }
}
