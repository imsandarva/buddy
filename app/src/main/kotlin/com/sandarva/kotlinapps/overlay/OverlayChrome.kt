package com.sandarva.kotlinapps.overlay

import android.os.Looper
import android.view.View
import android.view.WindowManager

/**
 * Every Buddy overlay (cursor, ask sheet) is chrome — not the user’s screen.
 * Hands make all of them pass through so a stroke hits the app, not us.
 */
object OverlayChrome {
    fun interface Layer {
        fun setPassthrough(on: Boolean)
    }

    private val layers = LinkedHashSet<Layer>()

    @Synchronized fun attach(layer: Layer) { layers += layer }
    @Synchronized fun detach(layer: Layer) { layers -= layer }

    fun setPassthrough(on: Boolean) {
        val copy = synchronized(this) { layers.toList() }
        copy.forEach { it.setPassthrough(on) }
    }
}

/** Android 12+ drops touches through an opaque system-alert window; fade just enough to stay trusted. */
fun WindowManager.applyPassthrough(host: View, layout: WindowManager.LayoutParams, on: Boolean, baseFlags: Int) {
    layout.flags = if (on) baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else baseFlags
    layout.alpha = if (on) PASS_ALPHA else 1f
    val apply = Runnable {
        try { updateViewLayout(host, layout) } catch (_: IllegalArgumentException) { }
    }
    if (Looper.myLooper() == Looper.getMainLooper()) apply.run() else host.post(apply)
}

private const val PASS_ALPHA = 0.79f
