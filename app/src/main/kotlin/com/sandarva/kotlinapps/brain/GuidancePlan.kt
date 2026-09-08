package com.sandarva.kotlinapps.brain

/** Structured brain output. The user only ever hears `say`. */
data class GuidancePlan(
    val say: String?,
    val elementId: String?,
    val place: String?,
    val hand: HandPlan? = null,
    val type: TypePlan? = null,
    val runGoal: String? = null,
    val openApp: String? = null,
    val done: String? = null
)
