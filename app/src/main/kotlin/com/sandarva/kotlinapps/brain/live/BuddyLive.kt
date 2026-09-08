package com.sandarva.kotlinapps.brain.live

import android.app.Application
import com.sandarva.kotlinapps.BuildConfig
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
import com.sandarva.kotlinapps.accessibility.awaitSettledSnapshot
import com.sandarva.kotlinapps.accessibility.sceneKey
import com.sandarva.kotlinapps.brain.BrainPhase
import com.sandarva.kotlinapps.brain.BrainSession
import com.sandarva.kotlinapps.brain.Reachability
import com.sandarva.kotlinapps.brain.agent.AgentExecutor
import com.sandarva.kotlinapps.brain.agent.AgentRunner
import com.sandarva.kotlinapps.brain.agent.AppLauncher
import com.sandarva.kotlinapps.brain.agent.SceneDescriber
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
 * Gemini Live adapter. Native audio in/out; one-shot cursor tools go through [AgentExecutor];
 * anything longer is handed to [AgentRunner]. Typed asks go straight to the runner in BuddyBrain.
 */
object BuddyLive {
    @Volatile private var session: Session? = null

    fun ensure(app: Application): Session {
        session?.let { return it }
        return Session(app).also { session = it }
    }

    fun isActive(): Boolean = session?.active == true
    fun start(afterGoal: Boolean = false) { session?.start(afterGoal) }
    fun stop() { session?.stop(user = true) }

    class Session(private val app: Application) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var socket: LiveSocket? = null
        private var mic: LiveMic? = null
        private var audio: LiveAudio? = null
        private val speaker = LiveSpeaker()
        private val executor = AgentExecutor(AppLauncher(app))
        @Volatile var active = false
            private set
        private var gen = 0
        @Volatile private var lastScene = ""
        private var followJob: Job? = null
        @Volatile private var afterGoal = false
        private val listen = LiveListen()

        fun start(afterGoal: Boolean = false) {
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
            listen.reset()
            this.afterGoal = afterGoal
            BuddyScreenEyes.setWatching(true)
            BuddyLog.d("Live.start", "model=${LiveConfig.MODEL} afterGoal=$afterGoal")
            val next = LiveSocket(BuildConfig.GEMINI_API_KEY, object : LiveSocket.Listener {
                override fun onSetupComplete() {
                    if (id != gen) return
                    if (afterGoal) openEars(id)
                    else beginHello(id)
                }
                override fun onAudio(pcm: ByteArray) {
                    if (id != gen) return
                    listen.onModelAudio()
                    speaker.play(pcm)
                }
                override fun onInterrupted() {
                    if (id != gen) return
                    listen.onInterrupted()
                    speaker.interrupt()
                }
                override fun onTurnComplete() {
                    if (id != gen) return
                    speaker.endUtterance()
                    if (listen.greeting) openEars(id)
                }
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

        fun stop(user: Boolean, handoff: Boolean = false) {
            if (!active && socket == null) return
            BuddyLog.d("Live.stop", "user=$user handoff=$handoff")
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
            if (!handoff && BrainSession.phase.value == BrainPhase.Live) BrainSession.setPhase(BrainPhase.Idle)
            OverlayNotifier.sync(app)
        }

        private fun beginHello(id: Int) {
            listen.startHello()
            socket?.send(LiveMessages.hello())
            scope.launch {
                delay(HELLO_WAIT_MS)
                if (id == gen && active && listen.greeting) openEars(id)
            }
        }

        private fun openEars(id: Int) {
            if (!active || id != gen) return
            listen.startListening()
            startMic()
            pushCatalog(BuddyScreenEyes.snapshot())
            followJob?.cancel()
            followJob = scope.launch { followScreen(id) }
        }

        private fun startMic() {
            if (!active || mic != null) return
            val next = LiveMic { pcm ->
                if (socket?.isReady != true) return@LiveMic
                listen.onMic(pcm)
                socket?.send(LiveMessages.audio(pcm))
            }
            mic = next
            if (!next.start(audio)) fail("I couldn’t hear you on this phone. Type it instead.")
        }

        private suspend fun runTools(calls: List<LiveFunctionCall>) {
            for (call in calls) {
                if (!active) return
                val intent = LiveTools.intent(call)
                BuddyLog.d("Live.tool", "name=${call.name} id=${call.id} intent=$intent")
                if (!listen.allow(intent)) {
                    BuddyLog.d("Live.tool", "blocked name=${call.name} greeting=${listen.greeting} heard=${listen.theySpoke}")
                    socket?.send(LiveMessages.toolResponse(call.id, call.name, "ignored — wait until they ask", describe(BuddyScreenEyes.snapshot())))
                    continue
                }
                when (intent) {
                    is LiveIntent.RunGoal -> { handoffGoal(intent.goal); return }
                    is LiveIntent.Unknown -> socket?.send(LiveMessages.toolResponse(call.id, call.name, "not done — unknown tool or missing argument", describe(BuddyScreenEyes.snapshot())))
                    is LiveIntent.Act -> {
                        val outcome = executor.perform(intent.action, BuddyScreenEyes.snapshot())
                        val after = awaitSettledSnapshot()
                        lastScene = after.sceneKey()
                        socket?.send(LiveMessages.toolResponse(call.id, call.name, if (outcome.ok) outcome.detail else "not done — ${outcome.detail}", describe(after)))
                    }
                }
            }
        }

        private fun describe(snap: ScreenSnapshot): String = SceneDescriber.describe(snap, SceneDescriber.LIVE_LINES)

        /** Live steps aside; the runner does the job on the screen, then talk comes back on its own. */
        private fun handoffGoal(goal: String) {
            BuddyLog.d("Live.handoff", "goal=\"${goal.take(80)}\"")
            stop(user = false, handoff = true)
            AgentRunner.ensure(app).start(goal, resumeLive = true)
        }

        private suspend fun followScreen(id: Int) {
            BuddyScreenEyes.scenes.collect { snap ->
                if (id != gen || !active || socket?.isReady != true || listen.greeting) return@collect
                pushCatalog(snap)
            }
        }

        private fun pushCatalog(snap: ScreenSnapshot = BuddyScreenEyes.snapshot()) {
            val key = snap.sceneKey()
            if (key == lastScene) return
            lastScene = key
            val catalog = describe(snap)
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
            private const val SETUP_WAIT_MS = 12_000L
            private const val HELLO_WAIT_MS = 2_800L
        }
    }
}
