package com.sandarva.kotlinapps.accessibility

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Eyes API. The brain asks for a snapshot or `pointTo`; it never walks the tree. */
object BuddyScreenEyes {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var reader: ScreenReader? = null
    @Volatile private var tracker: ScreenSceneTracker? = null
    private val idle = MutableSharedFlow<ScreenSnapshot>(replay = 0)

    /** Last time the screen reported a change — lets a run wait for quiet instead of a fixed delay. */
    @Volatile var lastEventAt: Long = 0L
        private set

    fun attach(next: ScreenReader, watch: ScreenSceneTracker) {
        reader = next
        tracker = watch
    }

    fun detach(current: ScreenReader) {
        if (reader !== current) return
        tracker?.release()
        tracker = null
        reader = null
    }

    val scenes: SharedFlow<ScreenSnapshot> get() = tracker?.scenes ?: idle

    fun isReady(): Boolean = reader != null

    fun setWatching(on: Boolean) { tracker?.setWatching(on) }

    fun onWindowEvent(event: AccessibilityEvent) {
        if (ScreenSceneTracker.isSceneEvent(event)) lastEventAt = SystemClock.elapsedRealtime()
        tracker?.onEvent(event)
    }

    fun snapshot(): ScreenSnapshot {
        val current = reader ?: return ScreenSnapshot.Empty
        if (Looper.myLooper() == Looper.getMainLooper()) return current.snapshot()
        val latch = CountDownLatch(1)
        val box = arrayOfNulls<ScreenSnapshot>(1)
        main.post { box[0] = current.snapshot(); latch.countDown() }
        latch.await(800, TimeUnit.MILLISECONDS)
        return box[0] ?: ScreenSnapshot.Empty
    }

    fun pointTo(id: String): Boolean = snapshot().node(id)?.let { pointTo(it) } ?: false

    fun pointTo(node: ScreenNode): Boolean {
        BuddyLog.d("Eyes.pointTo", "id=${node.id} x=${node.bounds.centerX} y=${node.bounds.centerY} hands=${BuddyCursorController.isAttached()}")
        return BuddyCursorController.animateToPixels(node.bounds.centerX, node.bounds.centerY)
    }

    fun pointToGuide(): Boolean = GuidePicker.choose(snapshot())?.let { pointTo(it) } ?: false
}
