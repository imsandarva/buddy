package com.sandarva.kotlinapps.brain.agent

/** What the run is doing right now — for the notification, the cursor mood, and the home screen. */
sealed class AgentState {
    object Idle : AgentState()
    data class Working(val step: Int, val note: String?) : AgentState()
    data class Asking(val question: String) : AgentState()
    data class Finishing(val message: String) : AgentState()

    val busy: Boolean get() = this !is Idle
}
