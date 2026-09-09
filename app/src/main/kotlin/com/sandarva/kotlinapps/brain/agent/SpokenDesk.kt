package com.sandarva.kotlinapps.brain.agent

import com.sandarva.kotlinapps.brain.BuddyVoice

/** Typed / STT path — the runner has no Live talk, so Android TTS and the ask sheet are the voice. */
class SpokenDesk(
    private val voice: BuddyVoice,
    private val onAsk: (String) -> Unit,
    private val onFinished: () -> Unit,
) : AgentDesk {
    override val holdsTalk: Boolean = false
    override fun say(line: String) { if (!voice.isSpeaking()) voice.speak(line) }
    override fun ask(question: String) { voice.speak(question) { onAsk(question) } }
    override fun done(message: String) { voice.speak(message) { onFinished() } }
}
