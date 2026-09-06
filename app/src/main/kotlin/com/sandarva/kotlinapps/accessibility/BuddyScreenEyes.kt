package com.sandarva.kotlinapps.accessibility

import com.sandarva.kotlinapps.overlay.BuddyCursorController

/** Eyes API. The brain (AI later) asks for a snapshot or `pointTo`; it never walks the tree. */
object BuddyScreenEyes {
    @Volatile
    private var reader: ScreenReader? = null

    fun attach(next: ScreenReader) { reader = next }
    fun detach(current: ScreenReader) { if (reader === current) reader = null }

    fun snapshot(): ScreenSnapshot = reader?.snapshot() ?: ScreenSnapshot.Empty

    fun pointTo(id: String): Boolean = snapshot().node(id)?.let { pointTo(it) } ?: false

    fun pointTo(node: ScreenNode): Boolean {
        BuddyCursorController.animateToPixels(node.bounds.centerX, node.bounds.centerY)
        return true
    }

    /** Debug path: pick one visible control and fly there. */
    fun pointToGuide(): Boolean = GuidePicker.choose(snapshot())?.let { pointTo(it) } ?: false
}
