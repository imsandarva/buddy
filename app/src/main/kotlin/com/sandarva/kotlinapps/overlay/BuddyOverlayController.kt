package com.sandarva.kotlinapps.overlay

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sandarva.kotlinapps.debug.BuddyLog

/** Starts/stops the overlay service. Permission is requested from the activity; the service owns the window. */
object BuddyOverlayController {
    fun requestStart(context: Context) {
        if (!OverlayPermission.canDraw(context)) {
            OverlaySession.setAwaitingPermission(true)
            OverlayPermission.openSettings(context)
            return
        }
        OverlaySession.setAwaitingPermission(false)
        startService(context)
    }

    fun onHostResumed(context: Context) {
        if (!OverlaySession.awaitingPermission.value) return
        if (!OverlayPermission.canDraw(context)) return
        OverlaySession.setAwaitingPermission(false)
        startService(context)
    }

    fun stop(context: Context) {
        BuddyLog.d("Overlay.stop", "active=${OverlaySession.active.value}")
        OverlaySession.setAwaitingPermission(false)
        val stop = Intent(context, BuddyOverlayService::class.java).setAction(BuddyOverlayService.ACTION_STOP)
        runCatching { context.startService(stop) }
        context.stopService(Intent(context, BuddyOverlayService::class.java))
    }

    private fun startService(context: Context) {
        ContextCompat.startForegroundService(context, Intent(context, BuddyOverlayService::class.java).setAction(BuddyOverlayService.ACTION_START))
    }
}
