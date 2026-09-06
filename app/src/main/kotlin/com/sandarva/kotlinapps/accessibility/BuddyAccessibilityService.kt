package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** System accessibility service — entry point once the user enables Buddy in Settings. */
class BuddyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Future: wire screen-reading and gesture logic here.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Future: react to UI changes for step-by-step guidance.
    }

    override fun onInterrupt() {
        // Future: pause guidance when the system interrupts the service.
    }
}
