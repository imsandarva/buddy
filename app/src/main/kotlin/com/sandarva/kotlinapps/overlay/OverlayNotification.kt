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
    private const val CHANNEL_LIVE_ID = "buddy_live"

    fun build(context: Context, live: Boolean = false, working: Boolean = false, progress: String? = null): Notification {
        ensureChannels(context)
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = serviceIntent(context, 1, BuddyOverlayService.ACTION_STOP)
        if (working) {
            val end = serviceIntent(context, 4, BuddyOverlayService.ACTION_END_LIVE)
            val body = progress?.takeIf { it.isNotBlank() } ?: context.getString(R.string.goal_body)
            return NotificationCompat.Builder(context, CHANNEL_LIVE_ID)
                .setSmallIcon(R.drawable.ic_buddy_status)
                .setContentTitle(context.getString(R.string.goal_title))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setOnlyAlertOnce(true)
                .setContentIntent(openApp)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(0, context.getString(R.string.live_end), end)
                .addAction(0, context.getString(R.string.stop_buddy), stop)
                .build()
        }
        if (live) {
            val end = serviceIntent(context, 4, BuddyOverlayService.ACTION_END_LIVE)
            val typeInstead = serviceIntent(context, 5, BuddyOverlayService.ACTION_TYPE_INSTEAD)
            return NotificationCompat.Builder(context, CHANNEL_LIVE_ID)
                .setSmallIcon(R.drawable.ic_buddy_status)
                .setContentTitle(context.getString(R.string.live_title))
                .setContentText(context.getString(R.string.live_body))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.live_body)))
                .setContentIntent(openApp)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(0, context.getString(R.string.live_end), end)
                .addAction(0, context.getString(R.string.live_type_instead), typeInstead)
                .addAction(0, context.getString(R.string.stop_buddy), stop)
                .build()
        }
        val point = serviceIntent(context, 2, BuddyOverlayService.ACTION_POINT)
        val ask = serviceIntent(context, 3, BuddyOverlayService.ACTION_ASK)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_buddy_status)
            .setContentTitle(context.getString(R.string.overlay_notification_title))
            .setContentText(context.getString(R.string.overlay_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, context.getString(R.string.ask_buddy), ask)
            .addAction(0, context.getString(R.string.point_at_something), point)
            .addAction(0, context.getString(R.string.stop_buddy), stop)
            .build()
    }

    /** Refresh the text of the ongoing notification without restarting the foreground service. */
    fun update(context: Context, live: Boolean, working: Boolean, progress: String?) {
        context.getSystemService(NotificationManager::class.java)?.notify(ID, build(context, live, working, progress))
    }

    private fun serviceIntent(context: Context, requestCode: Int, action: String): PendingIntent =
        PendingIntent.getService(
            context, requestCode,
            Intent(context, BuddyOverlayService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.overlay_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_LIVE_ID, context.getString(R.string.overlay_live_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}
