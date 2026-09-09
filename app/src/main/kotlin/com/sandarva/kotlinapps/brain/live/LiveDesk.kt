package com.sandarva.kotlinapps.brain.live

import com.sandarva.kotlinapps.brain.agent.AgentDesk

/**
 * Live stays on the same socket. The runner reports here; we turn that into a turn the model speaks.
 * Industry shape: supervisor voice + silent worker (Gemini Live ack-then-complete, Alexa skill-while-talking).
 */
class LiveDesk(
    private val send: (String) -> Unit,
    private val onFinished: () -> Unit,
) : AgentDesk {
    override val holdsTalk: Boolean = true
    override fun say(line: String) { /* milestones stay on the notification — Live is the only voice */ }
    override fun ask(question: String) { send(LiveMessages.jobAsk(question)) }
    override fun done(message: String) {
        onFinished()
        send(LiveMessages.jobDone(message))
    }
}
