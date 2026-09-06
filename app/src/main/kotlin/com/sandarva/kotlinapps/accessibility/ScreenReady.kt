package com.sandarva.kotlinapps.accessibility

import android.os.SystemClock
import kotlinx.coroutines.delay

/** UI Automator’s “wait for new window”: the first tree after an app open is often hollow. */
suspend fun awaitReadableSnapshot(timeoutMs: Long = READY_MS): ScreenSnapshot {
    val deadline = SystemClock.elapsedRealtime() + timeoutMs
    var last = BuddyScreenEyes.snapshot()
    while (last.nodes.isEmpty() && SystemClock.elapsedRealtime() < deadline) {
        delay(RETRY_MS)
        last = BuddyScreenEyes.snapshot()
    }
    return last
}

private const val READY_MS = 1600L
private const val RETRY_MS = 280L
