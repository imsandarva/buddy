package com.sandarva.kotlinapps.session

/** Normalized cursor placement. Fractions stay stable across screen sizes; (0.5, 0.5) is center. */
data class BuddyCursorState(
    val visible: Boolean = false,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f
)
