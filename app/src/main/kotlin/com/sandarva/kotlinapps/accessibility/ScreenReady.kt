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

/**
 * UI Automator’s `waitForIdle`: after an action, wait until the accessibility event stream
 * has been quiet for a moment (the animation finished, the list stopped populating), then read.
 * Event-driven — no extra tree walks while the screen is still moving.
 */
suspend fun awaitSettledSnapshot(minMs: Long = SETTLE_MIN_MS, quietMs: Long = SETTLE_QUIET_MS, maxMs: Long = SETTLE_MAX_MS): ScreenSnapshot {
    val start = SystemClock.elapsedRealtime()
    delay(minMs)
    while (true) {
        val now = SystemClock.elapsedRealtime()
        val sinceEvent = now - BuddyScreenEyes.lastEventAt
        if (sinceEvent >= quietMs || now - start >= maxMs) break
        delay((quietMs - sinceEvent).coerceIn(30L, quietMs))
    }
    return awaitReadableSnapshot()
}

private const val READY_MS = 1600L
private const val RETRY_MS = 280L
private const val SETTLE_MIN_MS = 260L
private const val SETTLE_QUIET_MS = 240L
private const val SETTLE_MAX_MS = 1500L
