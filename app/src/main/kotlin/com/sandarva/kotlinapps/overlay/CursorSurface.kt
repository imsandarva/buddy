package com.sandarva.kotlinapps.overlay

import android.content.Context
import android.view.WindowManager
import com.sandarva.kotlinapps.accessibility.AccessibilitySession
import com.sandarva.kotlinapps.brain.BuddyBrain
import com.sandarva.kotlinapps.debug.BuddyLog

/**
 * One cursor window. Application overlay sits under the shade;
 * TalkBack-style [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY] sits above it.
 */
object CursorSurface {
    private var window: BuddyOverlayWindow? = null
    private var type = 0

    fun ensure(overlay: Context): BuddyOverlayWindow {
        val a11y = AccessibilitySession.service
        val want = if (a11y != null) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val host = a11y ?: overlay
        window?.let { if (type == want) return it }
        replace(host, want, overlay)
        return window!!
    }

    fun release() {
        window?.let {
            BuddyCursorController.detach(it)
            it.dismiss()
        }
        window = null
        type = 0
    }

    fun current(): BuddyOverlayWindow? = window

    private fun replace(host: Context, want: Int, overlay: Context) {
        window?.let {
            BuddyCursorController.detach(it)
            it.dismiss()
        }
        window = null
        type = 0
        val next = BuddyOverlayWindow(host, want) { BuddyBrain.openAsk() }
        try {
            next.show()
        } catch (error: Exception) {
            if (want != WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) throw error
            BuddyLog.e("CursorSurface", "a11y overlay failed — using appear-on-top", error)
            replace(overlay, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, overlay)
            return
        }
        window = next
        type = want
        BuddyCursorController.attach(next)
        BuddyLog.d("CursorSurface", "type=$want a11y=${AccessibilitySession.service != null}")
    }
}
