package com.sandarva.kotlinapps.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sandarva.kotlinapps.MainActivity
import com.sandarva.kotlinapps.R

object OverlayNotification {
    const val ID = 1001
    private const val CHANNEL_ID = "buddy_overlay"

    fun build(context: Context): Notification {
        ensureChannel(context)
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            context, 1,
            Intent(context, BuddyOverlayService::class.java).setAction(BuddyOverlayService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val point = PendingIntent.getService(
            context, 2,
            Intent(context, BuddyOverlayService::class.java).setAction(BuddyOverlayService.ACTION_POINT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_buddy_status)
            .setContentTitle(context.getString(R.string.overlay_notification_title))
            .setContentText(context.getString(R.string.overlay_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, context.getString(R.string.point_at_something), point)
            .addAction(0, context.getString(R.string.stop_buddy), stop)
            .build()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.overlay_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
    }
}
