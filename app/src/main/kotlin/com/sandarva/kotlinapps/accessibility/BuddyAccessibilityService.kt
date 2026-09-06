package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/** System service — composition only. Walking lives in the reader; pointing lives in the eyes API. */
class BuddyAccessibilityService : AccessibilityService() {
    private val reader by lazy { AccessibilityTreeReader(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        BuddyScreenEyes.attach(reader)
        AccessibilitySession.setBound(true)
        AccessibilitySession.setEnabled(true)
        AccessibilitySession.setAwaitingGrant(false)
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
        BuddyScreenEyes.detach(reader)
        AccessibilitySession.setBound(false)
    }
}
