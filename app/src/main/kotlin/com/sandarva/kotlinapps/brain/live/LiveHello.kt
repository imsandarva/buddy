package com.sandarva.kotlinapps.brain.live

import android.app.Application
import com.sandarva.kotlinapps.brain.Reachability
import com.sandarva.kotlinapps.data.ApiKeyStore
import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One-shot Gemini Live hello — same Fenrir voice as a real talk, no mic, no overlay, no tools.
 * Speaks once, hangs up. Used for the first-meeting beat so onboarding never falls back to device TTS.
 */
object LiveHello {
    @Volatile private var run: Run? = null

    fun play(app: Application, onFinished: () -> Unit) {
        cancel()
        if (ApiKeyStore.currentKey.isBlank() || !Reachability.online(app)) {
            onFinished()
            return
        }
        Run(app, onFinished).also { run = it }.start()
    }

    fun cancel() {
        run?.stop()
        run = null
    }

    private class Run(private val app: Application, private val onFinished: () -> Unit) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val speaker = LiveSpeaker()
        private var socket: LiveSocket? = null
        private var audio: LiveAudio? = null
        private var gen = 0
        @Volatile private var done = false
        @Volatile private var heard = false

        fun start() {
            val id = ++gen
            val links = LiveAudio(app)
            audio = links
            links.enterVoiceRoute()
            speaker.start(links.sessionId)
            val next = LiveSocket(
                apiKey = ApiKeyStore.currentKey,
                listener = object : LiveSocket.Listener {
                    override fun onSetupComplete() { if (id == gen) socket?.send(LiveMessages.meet()) }
                    override fun onAudio(pcm: ByteArray) { if (id == gen) { heard = true; speaker.play(pcm) } }
                    override fun onInterrupted() { if (id == gen) speaker.interrupt() }
                    override fun onTurnComplete() {
                        if (id != gen || !heard) return
                        speaker.endUtterance { scope.launch { delay(LiveConfig.TRACK_BUFFER_MS.toLong() + 80); finish() } }
                    }
                    override fun onToolCall(calls: List<LiveFunctionCall>) = Unit
                    override fun onClosed(reason: String) { if (id == gen) finish() }
                },
                setup = LiveMessages.setupHello()
            )
            socket = next
            next.connect()
            BuddyLog.d("Live.hello", "start")
            scope.launch {
                delay(SETUP_WAIT_MS)
                if (id == gen) finish()
            }
        }

        fun stop() {
            if (done && socket == null) return
            gen += 1
            speaker.stop()
            audio?.release(); audio = null
            socket?.close(); socket = null
            scope.cancel()
        }

        private fun finish() {
            if (done) return
            done = true
            stop()
            BuddyLog.d("Live.hello", "done heard=$heard")
            CoroutineScope(Dispatchers.Main.immediate).launch { onFinished() }
        }

        companion object { private const val SETUP_WAIT_MS = 12_000L }
    }
}
