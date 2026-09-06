package com.sandarva.kotlinapps.overlay

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.brain.BrainSession
import com.sandarva.kotlinapps.brain.BuddyBrain
import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Foreground service that keeps BuddyCursor on screen after the activity leaves. */
class BuddyOverlayService : Service() {
    private var cursor: BuddyOverlayWindow? = null
    private var ask: AskOverlayWindow? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        BuddyLog.d("OverlayService", "onStartCommand action=${intent?.action} cursor=${cursor != null}")
        if (intent?.action == ACTION_STOP) {
            BuddyLog.d("OverlayService", "ACTION_STOP → stopSelf")
            stopSelf()
            return START_NOT_STICKY
        }
        BuddyBrain.ensure(application)
        presentCursor()
        watchAsk()
        if (intent?.action == ACTION_POINT) {
            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.postDelayed({ BuddyScreenEyes.pointToGuide() }, SHADE_SETTLE_MS)
            return START_STICKY
        }
        if (intent?.action == ACTION_ASK) {
            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.postDelayed({ BuddyBrain.openAsk() }, SHADE_SETTLE_MS)
            return START_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        BuddyLog.d("OverlayService", "onDestroy")
        mainHandler.removeCallbacksAndMessages(null)
        scope.cancel()
        hideAsk()
        cursor?.let {
            BuddyCursorController.detach(it)
            it.dismiss()
        }
        cursor = null
        OverlaySession.setActive(false)
        super.onDestroy()
    }

    private fun presentCursor() {
        if (cursor != null) return
        applyFgs(askOpen = false)
        val next = BuddyOverlayWindow(this, onAsk = { BuddyBrain.openAsk() }).also { it.show() }
        cursor = next
        BuddyCursorController.attach(next)
        OverlaySession.setActive(true)
    }

    private fun watchAsk() {
        if (watchingAsk) return
        watchingAsk = true
        scope.launch {
            BrainSession.askOpen.collect { open ->
                applyFgs(open)
                if (open) showAsk() else hideAsk()
            }
        }
    }

    private fun showAsk() {
        val panel = ask ?: AskOverlayWindow(this).also { ask = it }
        if (!panel.isShowing) panel.show()
        cursor?.raise()
    }

    private fun hideAsk() {
        ask?.dismiss()
        ask = null
    }

    private fun applyFgs(askOpen: Boolean) {
        ServiceCompat.startForeground(this, OverlayNotification.ID, OverlayNotification.build(this), fgsType(askOpen))
    }

    private fun fgsType(askOpen: Boolean): Int {
        val mic = askOpen && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= 34) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            if (mic) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            return type
        }
        if (Build.VERSION.SDK_INT >= 30 && mic) return ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        return 0
    }

    private var watchingAsk = false

    companion object {
        const val ACTION_START = "com.sandarva.kotlinapps.overlay.START"
        const val ACTION_STOP = "com.sandarva.kotlinapps.overlay.STOP"
        const val ACTION_POINT = "com.sandarva.kotlinapps.overlay.POINT"
        const val ACTION_ASK = "com.sandarva.kotlinapps.overlay.ASK"
        private const val SHADE_SETTLE_MS = 320L
    }
}
