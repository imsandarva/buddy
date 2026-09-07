package com.sandarva.kotlinapps.brain.live

import android.app.Application
import com.sandarva.kotlinapps.BuildConfig
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
import com.sandarva.kotlinapps.accessibility.awaitReadableSnapshot
import com.sandarva.kotlinapps.accessibility.sceneKey
import com.sandarva.kotlinapps.brain.BrainPhase
import com.sandarva.kotlinapps.brain.BrainSession
import com.sandarva.kotlinapps.brain.GeminiTools
import com.sandarva.kotlinapps.brain.GuidanceActor
import com.sandarva.kotlinapps.brain.GuidanceCatalog
import com.sandarva.kotlinapps.brain.Reachability
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.OverlayNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var socket: LiveSocket? = null
        private var mic: LiveMic? = null
        private var audio: LiveAudio? = null
        private val speaker = LiveSpeaker()
        @Volatile var active = false
            private set
        private var gen = 0
        @Volatile private var lastScene = ""
        private var followJob: Job? = null

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
            OverlayNotifier.sync(app)
            lastScene = ""
            BuddyScreenEyes.setWatching(true)
            BuddyLog.d("Live.start", "model=${LiveConfig.MODEL}")
            val next = LiveSocket(BuildConfig.GEMINI_API_KEY, object : LiveSocket.Listener {
                override fun onSetupComplete() {
                    if (id != gen) return
                    startMic()
                    scope.launch { pushCatalog(awaitReadableSnapshot()) }
                    followJob?.cancel()
                    followJob = scope.launch { followScreen(id) }
                }
                override fun onAudio(pcm: ByteArray) { if (id == gen) speaker.play(pcm) }
                override fun onInterrupted() { if (id == gen) speaker.interrupt() }
                override fun onTurnComplete() { if (id == gen) speaker.endUtterance() }
                override fun onToolCall(calls: List<LiveFunctionCall>) { if (id == gen) scope.launch { runTools(calls) } }
                override fun onClosed(reason: String) {
                    if (id != gen) return
                    BuddyLog.d("Live.closed", reason)
                    if (active) fail("The live talk ended. Double-tap me to try again.")
                }
            })
            socket = next
            val links = LiveAudio(app)
            audio = links
            links.enterVoiceRoute()
            speaker.start(links.sessionId)
            next.connect()
            scope.launch {
                delay(SETUP_WAIT_MS)
                if (id == gen && active && socket?.isReady != true) {
                    BuddyLog.d("Live.setup", "timeout — no setupComplete")
                    fail("I couldn’t start a live talk. Try again in a moment.")
                }
            }
        }

        fun stop(user: Boolean) {
            if (!active && socket == null) return
            BuddyLog.d("Live.stop", "user=$user")
            gen += 1
            active = false
            BuddyScreenEyes.setWatching(false)
            lastScene = ""
            followJob?.cancel(); followJob = null
            mic?.stop(); mic = null
            speaker.stop()
            audio?.release(); audio = null
            socket?.close(); socket = null
            BrainSession.setLiveOpen(false)
            if (BrainSession.phase.value == BrainPhase.Live) BrainSession.setPhase(BrainPhase.Idle)
            OverlayNotifier.sync(app)
        }

        private fun startMic() {
            if (!active) return
            val next = LiveMic { pcm -> if (socket?.isReady == true) socket?.send(LiveMessages.audio(pcm)) }
            mic = next
            if (!next.start(audio)) fail("I couldn’t hear you on this phone. Type it instead.")
        }

        private suspend fun runTools(calls: List<LiveFunctionCall>) {
            for (call in calls) {
                if (!active) return
                val snap = BuddyScreenEyes.snapshot()
                val plan = GeminiTools.planFromCall(call.name, call.args)
                BuddyLog.d("Live.tool", "name=${call.name} id=${call.id}")
                val result = GuidanceActor.run(plan, snap)
                delay(TREE_SETTLE_MS)
                val after = awaitReadableSnapshot()
                lastScene = after.sceneKey()
                socket?.send(LiveMessages.toolResponse(call.id, call.name, result, GuidanceCatalog.format(after, LIVE_CATALOG)))
            }
        }

        private suspend fun followScreen(id: Int) {
            BuddyScreenEyes.scenes.collect { snap ->
                if (id != gen || !active || socket?.isReady != true) return@collect
                pushCatalog(snap)
            }
        }

        private fun pushCatalog(snap: ScreenSnapshot = BuddyScreenEyes.snapshot()) {
            val key = snap.sceneKey()
            if (key == lastScene) return
            lastScene = key
            val catalog = GuidanceCatalog.format(snap, LIVE_CATALOG)
            BuddyLog.d("Live.catalog", "pkg=${snap.packageName} nodes=${snap.nodes.size} chars=${catalog.length}")
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

        companion object {
            private const val TREE_SETTLE_MS = 400L
            private const val SETUP_WAIT_MS = 12_000L
            private const val LIVE_CATALOG = 72
        }
    }
}
