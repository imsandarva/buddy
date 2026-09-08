package com.sandarva.kotlinapps.brain.live

import android.os.SystemClock
import com.sandarva.kotlinapps.brain.agent.AgentAction
import com.sandarva.kotlinapps.debug.BuddyLog

/**
 * When Live may move the phone. Official Live clients treat **server VAD** as “they spoke”
 * (the model only answers after the server commits a user turn). A raw energy gate on
 * `VOICE_COMMUNICATION` PCM is too deaf — AEC/NS punch holes in speech, so consecutive-loud
 * checks never trip and every tool is blocked after “On it.”
 *
 * Greeting tools stay blocked. After the mic is open, we arm on: hangover energy, leftover-hello
 * grace then model audio, barge-in, or a tool that already holds their words (`run_goal`, `open_app`, `type`).
 */
class LiveListen {
    private val speech = LiveSpeech()
    @Volatile private var hello = true
    @Volatile private var heard = false
    private var earsAt = 0L

    fun reset() {
        speech.reset()
        hello = true
        heard = false
        earsAt = 0L
    }

    fun startHello() { hello = true; heard = false }

    fun startListening() {
        hello = false
        earsAt = SystemClock.elapsedRealtime()
    }

    val theySpoke: Boolean get() = heard
    val greeting: Boolean get() = hello

    /** First time the mic energy looks like speech after hello. */
    fun onMic(pcm: ByteArray): Boolean = arm(speech.feed(pcm), "mic")

    /** Model audio after the hello-grace window — server VAD already decided they talked. */
    fun onModelAudio() {
        if (hello || heard) return
        if (SystemClock.elapsedRealtime() - earsAt < HELLO_GRACE_MS) return
        arm(true, "model-reply")
    }

    fun onInterrupted() { arm(!hello, "barge-in") }

    fun allow(intent: LiveIntent): Boolean {
        if (hello) return false
        if (heard) return true
        if (!spokenRequest(intent)) return false
        arm(true, "tool-words")
        return true
    }

    private fun arm(ok: Boolean, why: String): Boolean {
        if (!ok || hello || heard) return false
        heard = true
        BuddyLog.d("Live.heard", why)
        return true
    }

    /** Args that only exist if the model heard them — not an eager fly/tap from SCREEN. */
    private fun spokenRequest(intent: LiveIntent): Boolean = when (intent) {
        is LiveIntent.RunGoal -> intent.goal.isNotBlank()
        is LiveIntent.Act -> when (val action = intent.action) {
            is AgentAction.OpenApp -> action.name.isNotBlank()
            is AgentAction.Type -> action.text.isNotBlank()
            is AgentAction.Ask -> action.question.isNotBlank()
            else -> false
        }
        is LiveIntent.Unknown -> false
    }

    companion object {
        /** Hello audio can still arrive a beat after we open the mic — do not treat it as their ask. */
        private const val HELLO_GRACE_MS = 900L
    }
}
