package com.sandarva.kotlinapps.brain

/** Fill a text field. Ids stay in the snapshot. */
data class TypePlan(
    val text: String,
    val elementId: String?,
    val submit: Boolean
)
