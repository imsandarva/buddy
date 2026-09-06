package com.sandarva.kotlinapps.brain.live

import android.app.Application
import com.sandarva.kotlinapps.BuildConfig
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.brain.BrainPhase
import com.sandarva.kotlinapps.brain.BrainSession
import com.sandarva.kotlinapps.brain.GeminiTools
import com.sandarva.kotlinapps.brain.GuidanceActor
import com.sandarva.kotlinapps.brain.GuidanceCatalog
import com.sandarva.kotlinapps.brain.Reachability
import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gemini Live adapter. Native audio in/out; cursor tools go through [GuidanceActor].
 * Chat REST stays in BuddyBrain for typed asks.
 */
object BuddyLive {
    @Volatile private var session: Session? = null

    fun ensure(app: Application): Session {
        session?.let { return it }
        return Session(app).also { session = it }
    }

    fun isActive(): Boolean = session?.active == true
    fun start() { session?.start() }
    fun stop() { session?.stop(user = true) }

    class Session(private val app: Application) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var socket: LiveSocket? = null
        private var mic: LiveMic? = null
        private val speaker = LiveSpeaker()
        @Volatile var active = false
            private set
        private var gen = 0

        fun start() {
            if (active) return
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                fail("I don’t have a way to talk live yet.")
                return
            }
            if (!Reachability.online(app)) {
                fail("I can’t reach the internet just now. Check the connection and try again.")
                return
            }
            val id = ++gen
            active = true
            BrainSession.setAskOpen(false)
            BrainSession.setLiveOpen(true)
            BrainSession.setPhase(BrainPhase.Live)
            BrainSession.setNote(null)
            BuddyLog.d("Live.start", "model=${LiveConfig.MODEL}")
            val next = LiveSocket(BuildConfig.GEMINI_API_KEY, object : LiveSocket.Listener {
                override fun onSetupComplete() {
                    if (id != gen) return
                    scope.launch {
                        pushCatalog()
                        startMic()
                    }
                }
                override fun onAudio(pcm: ByteArray) { if (id == gen) speaker.play(pcm) }
                override fun onInterrupted() { if (id == gen) speaker.interrupt() }
                override fun onToolCall(calls: List<LiveFunctionCall>) { if (id == gen) scope.launch { runTools(calls) } }
                override fun onTranscript(text: String, fromUser: Boolean) {
                    if (id != gen) return
                    BuddyLog.d("Live.transcript", "user=$fromUser text=\"${text.take(80)}\"")
                    if (fromUser) BrainSession.setNote(text)
                }
                override fun onClosed(reason: String) {
                    if (id != gen) return
                    BuddyLog.d("Live.closed", reason)
                    if (active) fail("The live talk ended. Double-tap me to try again.")
                }
            })
            socket = next
            speaker.start()
            next.connect()
            scope.launch {
                delay(12_000L)
                if (id == gen && active && socket?.isReady != true) fail("I couldn’t start a live talk. Try again in a moment.")
            }
        }

        fun stop(user: Boolean) {
            if (!active && socket == null) return
            BuddyLog.d("Live.stop", "user=$user")
            gen += 1
            active = false
            mic?.stop(); mic = null
            speaker.stop()
            socket?.close(); socket = null
            BrainSession.setLiveOpen(false)
            if (BrainSession.phase.value == BrainPhase.Live) BrainSession.setPhase(BrainPhase.Idle)
        }

        private fun startMic() {
            if (!active) return
            val next = LiveMic { pcm -> if (socket?.isReady == true) socket?.send(LiveMessages.audio(pcm)) }
            mic = next
            if (!next.start()) fail("I couldn’t hear you on this phone. Type it instead.")
        }

        private suspend fun runTools(calls: List<LiveFunctionCall>) {
            for (call in calls) {
                if (!active) return
                val snap = BuddyScreenEyes.snapshot()
                val plan = GeminiTools.planFromCall(call.name, call.args)
                BuddyLog.d("Live.tool", "name=${call.name} id=${call.id}")
                val result = GuidanceActor.run(plan, snap)
                delay(TREE_SETTLE_MS)
                val screen = GuidanceCatalog.format(BuddyScreenEyes.snapshot())
                socket?.send(LiveMessages.toolResponse(call.id, call.name, result, screen))
            }
        }

        private fun pushCatalog() {
            val catalog = GuidanceCatalog.format(BuddyScreenEyes.snapshot())
            BuddyLog.d("Live.catalog", "chars=${catalog.length}")
            socket?.send(LiveMessages.catalog(catalog))
        }

        private fun fail(message: String) {
            stop(user = false)
            BrainSession.setPhase(BrainPhase.Idle)
            BrainSession.setNote(message)
            BrainSession.setAskOpen(true)
        }

        fun release() {
            stop(user = true)
            scope.cancel()
        }

        companion object { private const val TREE_SETTLE_MS = 320L }
    }
}
