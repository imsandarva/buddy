package com.sandarva.kotlinapps.accessibility

import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Follows what the person is looking at — new apps, and new pages inside the same app
 * (app drawer swipe is the same window; TalkBack watches scroll + content for that).
 */
class ScreenSceneTracker(
    private val snapshot: () -> ScreenSnapshot
) {
    private val main = Handler(Looper.getMainLooper())
    private val _scenes = MutableSharedFlow<ScreenSnapshot>(replay = 1, extraBufferCapacity = 1)
    val scenes: SharedFlow<ScreenSnapshot> = _scenes.asSharedFlow()
    @Volatile private var watching = false
    @Volatile private var lastKey = ""
    private var emptyTries = 0

    fun setWatching(on: Boolean) {
        watching = on
        if (on) schedule(0) else {
            main.removeCallbacksAndMessages(null)
            lastKey = ""
            emptyTries = 0
        }
    }

    fun onEvent(event: AccessibilityEvent) {
        if (!watching || !isSceneEvent(event)) return
        schedule(if (isPageTurn(event)) PAGE_SETTLE_MS else WINDOW_SETTLE_MS)
    }

    fun release() {
        watching = false
        main.removeCallbacksAndMessages(null)
        lastKey = ""
        emptyTries = 0
    }

    private fun schedule(delayMs: Long) {
        main.removeCallbacks(flush)
        main.postDelayed(flush, delayMs)
    }

    private val flush = Runnable {
        if (!watching) return@Runnable
        val snap = snapshot()
        if (snap.nodes.isEmpty() && emptyTries < EMPTY_RETRY_MAX) {
            emptyTries += 1
            BuddyLog.d("Eyes.scene", "empty retry=$emptyTries pkg=${snap.packageName}")
            schedule(EMPTY_RETRY_MS)
            return@Runnable
        }
        emptyTries = 0
        val key = snap.sceneKey()
        if (key == lastKey) return@Runnable
        lastKey = key
        BuddyLog.d("Eyes.scene", "pkg=${snap.packageName} nodes=${snap.nodes.size}")
        _scenes.tryEmit(snap)
    }

    companion object {
        /** Wait for a swipe/page animation to finish before reading the new icons. */
        private const val PAGE_SETTLE_MS = 380L
        private const val WINDOW_SETTLE_MS = 560L
        private const val EMPTY_RETRY_MS = 320L
        private const val EMPTY_RETRY_MAX = 4

        private fun isSceneEvent(event: AccessibilityEvent): Boolean {
            val t = event.eventType
            return t == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                t == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                t == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                t == AccessibilityEvent.TYPE_VIEW_SCROLLED
        }

        private fun isPageTurn(event: AccessibilityEvent): Boolean {
            val t = event.eventType
            return t == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || t == AccessibilityEvent.TYPE_VIEW_SCROLLED
        }
    }
}

/** Labels matter — launcher pages reuse the same view ids with different app names. */
fun ScreenSnapshot.sceneKey(): String = buildString(nodes.size * 24) {
    append(packageName.orEmpty())
    for (node in nodes) {
        append('|')
        append(node.id)
        append('=')
        append(node.label)
    }
}
