package com.sandarva.kotlinapps.accessibility

import com.sandarva.kotlinapps.debug.BuddyLog

/**
 * Type API. The brain asks to fill a field — never injects keys.
 * Voice Access / Switch Access do the same: set the field, then IME enter.
 */
object BuddyType {
    @Volatile private var writer: FieldWriter? = null

    fun attach(next: FieldWriter) { writer = next }
    fun detach(current: FieldWriter) { if (writer === current) writer = null }
    fun isReady(): Boolean = writer != null

    suspend fun typeHere(text: String, submit: Boolean = false): Boolean =
        write(text, FieldTarget(), submit)

    suspend fun typeAt(text: String, target: FieldTarget, submit: Boolean = false): Boolean =
        write(text, target, submit)

    suspend fun submitHere(): Boolean {
        val hands = writer
        if (hands == null) {
            BuddyLog.d("Type.submit", "no writer")
            return false
        }
        return hands.submit(FieldTarget())
    }

    private suspend fun write(text: String, target: FieldTarget, submit: Boolean): Boolean {
        val hands = writer
        if (hands == null) {
            BuddyLog.d("Type.write", "no writer")
            return false
        }
        BuddyLog.d("Type.write", "len=${text.length} id=${target.id} submit=$submit")
        return hands.write(text, target, submit)
    }
}

/** How to find the live field. Bounds let the cursor land first. */
data class FieldTarget(
    val id: String? = null,
    val viewId: String? = null,
    val bounds: ScreenBounds? = null
)
