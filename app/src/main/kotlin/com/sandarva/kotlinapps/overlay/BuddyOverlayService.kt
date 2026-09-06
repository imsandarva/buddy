package com.sandarva.kotlinapps.overlay

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes

/** Foreground service that keeps BuddyCursor on screen after the activity leaves. */
class BuddyOverlayService : Service() {
    private var window: BuddyOverlayWindow? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_POINT) {
            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.postDelayed({ BuddyScreenEyes.pointToGuide() }, SHADE_SETTLE_MS)
            return START_STICKY
        }
        ServiceCompat.startForeground(this, OverlayNotification.ID, OverlayNotification.build(this), fgsType())
        if (window == null) {
            val next = BuddyOverlayWindow(this).also { it.show() }
            window = next
            BuddyCursorController.attach(next)
            OverlaySession.setActive(true)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        window?.let {
            BuddyCursorController.detach(it)
            it.dismiss()
        }
        window = null
        OverlaySession.setActive(false)
        super.onDestroy()
    }

    private fun fgsType(): Int =
        if (android.os.Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0

    companion object {
        const val ACTION_START = "com.sandarva.kotlinapps.overlay.START"
        const val ACTION_STOP = "com.sandarva.kotlinapps.overlay.STOP"
        const val ACTION_POINT = "com.sandarva.kotlinapps.overlay.POINT"
        private const val SHADE_SETTLE_MS = 320L
    }
}
