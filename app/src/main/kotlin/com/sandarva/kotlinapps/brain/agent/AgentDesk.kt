package com.sandarva.kotlinapps.brain.agent

/**
 * How the runner talks to the world. Live keeps the voice; the typed sheet uses Android TTS.
 * The runner never speaks to the person itself.
 */
interface AgentDesk {
    /** True when the person is already in a Live talk — do not steal the session or the mic. */
    val holdsTalk: Boolean
    fun say(line: String)
    fun ask(question: String)
    fun done(message: String)
}
