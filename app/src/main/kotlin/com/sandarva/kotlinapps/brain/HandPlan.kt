package com.sandarva.kotlinapps.brain

/** What the hands should do after (or instead of) pointing. Ids stay in the snapshot. */
sealed class HandPlan {
    data class Tap(val elementId: String?) : HandPlan()
    data class Hold(val elementId: String?) : HandPlan()
    data class Stroke(
        val fromId: String?,
        val toId: String?,
        val toPlace: String?,
        val direction: String?,
        val holdFirst: Boolean
    ) : HandPlan()
}
