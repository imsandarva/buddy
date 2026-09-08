package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.sandarva.kotlinapps.debug.BuddyLog

/** System service — composition only. Walking lives in the reader; strokes, typing, and global keys attach here. */
class BuddyAccessibilityService : AccessibilityService() {
    private val reader by lazy { AccessibilityTreeReader(this) }
    private val tracker by lazy { ScreenSceneTracker { reader.snapshot() } }
    private val player by lazy { GesturePlayer(this) }
    private val writer by lazy { FieldWriter(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo
        info.eventTypes = info.eventTypes or
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
            AccessibilityEvent.TYPE_WINDOWS_CHANGED or
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
            AccessibilityEvent.TYPE_VIEW_SCROLLED
        info.flags = info.flags or
            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR
        }
        serviceInfo = info
        BuddyScreenEyes.attach(reader, tracker)
        BuddyHands.attach(player)
        BuddyType.attach(writer)
        BuddyGlobal.attach(this)
        AccessibilitySession.bind(this)
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
        if (event != null) BuddyScreenEyes.onWindowEvent(event)
    }

    override fun onInterrupt() = Unit

    private fun releaseEyes() {
        BuddyGlobal.detach(this)
        BuddyType.detach(writer)
        BuddyHands.detach(player)
        BuddyScreenEyes.detach(reader)
        AccessibilitySession.unbind(this)
    }
}
