package com.sandarva.kotlinapps.debug

import android.util.Log

/** High-visibility logcat lines. Filter: Buddy===TRACE */
object BuddyLog {
    const val TAG = "Buddy===TRACE"

    fun d(where: String, message: String) {
        Log.d(TAG, "==================== $where ==================== $message")
    }

    fun e(where: String, message: String, error: Throwable? = null) {
        Log.e(TAG, "==================== $where ==================== $message", error)
    }
}
