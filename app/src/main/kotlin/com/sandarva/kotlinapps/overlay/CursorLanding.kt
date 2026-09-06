package com.sandarva.kotlinapps.overlay

/** Named spots for `fly_to`. The model never sends pixels. */
object CursorLanding {
    val PLACES = listOf("top_left", "top", "top_right", "left", "center", "right", "bottom_left", "bottom", "bottom_right")

    private val spots = mapOf(
        "top_left" to (0.04f to 0.04f),
        "top" to (0.50f to 0.04f),
        "top_right" to (0.96f to 0.04f),
        "left" to (0.04f to 0.50f),
        "center" to (0.50f to 0.42f),
        "right" to (0.96f to 0.50f),
        "bottom_left" to (0.04f to 0.92f),
        "bottom" to (0.50f to 0.92f),
        "bottom_right" to (0.96f to 0.92f)
    )

    private val aliases = mapOf(
        "topleft" to "top_left",
        "upper_left" to "top_left",
        "topright" to "top_right",
        "upper_right" to "top_right",
        "bottomleft" to "bottom_left",
        "bottomright" to "bottom_right",
        "middle" to "center",
        "top_center" to "top",
        "bottom_center" to "bottom"
    )

    fun normalized(place: String): Pair<Float, Float>? {
        val key = place.trim().lowercase().replace('-', '_').replace(' ', '_')
        val name = if (key in spots) key else aliases[key]
        return name?.let { spots[it] }
    }
}
