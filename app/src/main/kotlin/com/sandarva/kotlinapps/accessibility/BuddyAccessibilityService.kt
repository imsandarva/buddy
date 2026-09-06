package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.sandarva.kotlinapps.debug.BuddyLog

/** System service — composition only. Walking lives in the reader; strokes live in the hands API. */
class BuddyAccessibilityService : AccessibilityService() {
    private val reader by lazy { AccessibilityTreeReader(this) }
    private val player by lazy { GesturePlayer(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo
        info.flags = info.flags or
            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        serviceInfo = info
        BuddyScreenEyes.attach(reader)
        BuddyHands.attach(player)
        AccessibilitySession.setBound(true)
        AccessibilitySession.setEnabled(true)
        AccessibilitySession.setAwaitingGrant(false)
        BuddyLog.d("Eyes.service", "connected flags=${serviceInfo.flags} windows=${windows?.size}")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        releaseEyes()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        releaseEyes()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Snapshot on demand. Do not walk the tree on every window event.
    }

    override fun onInterrupt() = Unit

    private fun releaseEyes() {
        BuddyHands.detach(player)
        BuddyScreenEyes.detach(reader)
        AccessibilitySession.setBound(false)
    }
}
