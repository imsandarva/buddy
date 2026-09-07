package com.sandarva.kotlinapps.overlay

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Nudge the overlay service to refresh its foreground notification from [BrainSession] state. */
object OverlayNotifier {
    fun sync(context: Context) {
        val app = context.applicationContext
        val intent = Intent(app, BuddyOverlayService::class.java).setAction(BuddyOverlayService.ACTION_SYNC)
        ContextCompat.startForegroundService(app, intent)
    }
}
