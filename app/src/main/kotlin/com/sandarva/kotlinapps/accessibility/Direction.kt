package com.sandarva.kotlinapps.accessibility

/** A finger or content direction. `dx`/`dy` are unit vectors in screen space. */
enum class Direction(val dx: Float, val dy: Float) {
    Up(0f, -1f), Down(0f, 1f), Left(-1f, 0f), Right(1f, 0f);

    val opposite: Direction get() = when (this) { Up -> Down; Down -> Up; Left -> Right; Right -> Left }
    val horizontal: Boolean get() = dx != 0f
    val word: String get() = name.lowercase()

    companion object {
        fun parse(raw: String?): Direction? = when (raw?.trim()?.lowercase()) {
            "up", "upward", "upwards" -> Up
            "down", "downward", "downwards" -> Down
            "left" -> Left
            "right" -> Right
            else -> null
        }
    }
}
