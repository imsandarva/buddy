package com.sandarva.kotlinapps.brain.live

import android.app.Application
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
import com.sandarva.kotlinapps.accessibility.awaitSettledSnapshot
import com.sandarva.kotlinapps.accessibility.sceneKey
import com.sandarva.kotlinapps.brain.BrainPhase
import com.sandarva.kotlinapps.brain.BrainSession
import com.sandarva.kotlinapps.brain.Reachability
import com.sandarva.kotlinapps.brain.GeminiClient
import com.sandarva.kotlinapps.brain.agent.AgentAction
import com.sandarva.kotlinapps.brain.agent.AgentExecutor
import com.sandarva.kotlinapps.brain.agent.AgentRunner
import com.sandarva.kotlinapps.brain.agent.AppLauncher
import com.sandarva.kotlinapps.brain.agent.SceneDescriber
import com.sandarva.kotlinapps.brain.agent.WebSearch
import com.sandarva.kotlinapps.data.ActivityLog
import com.sandarva.kotlinapps.data.ApiKeyStore
import com.sandarva.kotlinapps.data.LanguagePrefs
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
 * Gemini Live adapter. One socket for the whole talk. One-shot tools go through [AgentExecutor].
 * A real job starts [AgentRunner] in the background — this session stays up, remembers the talk,
 * and is the only voice. The runner reports through [LiveDesk].
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
        private val executor = AgentExecutor(AppLauncher(app))
        private val search = WebSearch(GeminiClient(ApiKeyStore::currentKey))
        @Volatile var active = false
            private set
        private var gen = 0
        @Volatile private var lastScene = ""
        private var followJob: Job? = null
        private val listen = LiveListen()
        private var language = LanguagePrefs.current()

        fun start() {
            if (active) return
            if (ApiKeyStore.currentKey.isBlank()) {
                fail("I don’t have a way to talk live yet.")
                return
            }
            if (!Reachability.online(app)) {
                fail("I can’t reach the internet just now. Check the connection and try again.")
                return
            }
            val id = ++gen
            language = LanguagePrefs.current()
            active = true
            BrainSession.setAskOpen(false)
            BrainSession.setLiveOpen(true)
            BrainSession.setPhase(BrainPhase.Live)
            BrainSession.setNote(null)
            OverlayNotifier.sync(app)
            lastScene = ""
            listen.reset()
            BuddyScreenEyes.setWatching(true)
            BuddyLog.d("Live.start", "model=${LiveConfig.MODEL} lang=${language.language}")
            val next = LiveSocket(ApiKeyStore.currentKey, object : LiveSocket.Listener {
                override fun onSetupComplete() {
                    if (id != gen) return
                    beginHello(id)
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
                    if (LiveFail.isAuthError(reason)) ApiKeyStore.markInvalid()
                    if (active) fail(LiveFail.speak(reason))
                }
            }, setup = LiveMessages.setup(language))
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
            AgentRunner.cancel()
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

        private fun beginHello(id: Int) {
            listen.startHello()
            socket?.send(LiveMessages.hello(language))
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
                if (listen.onMic(pcm)) pushCatalog(force = true)
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
                    is LiveIntent.RunGoal -> dispatchGoal(call, intent.goal)
                    is LiveIntent.AnswerJob -> replyJob(call, intent.text)
                    is LiveIntent.CancelJob -> stopJob(call)
                    is LiveIntent.SearchWeb -> lookup(call, intent.query)
                    is LiveIntent.Unknown -> socket?.send(LiveMessages.toolResponse(call.id, call.name, "not done — unknown tool or missing argument", describe(BuddyScreenEyes.snapshot())))
                    is LiveIntent.Act -> {
                        if (AgentRunner.isActive()) {
                            socket?.send(LiveMessages.toolResponse(call.id, call.name, "ignored — a job has the screen. Just talk.", describe(BuddyScreenEyes.snapshot())))
                        } else perform(call, intent.action)
                    }
                }
            }
        }

        private fun describe(snap: ScreenSnapshot): String = SceneDescriber.describe(snap, SceneDescriber.LIVE_LINES)

        /** The model asked for a job — the router decides if it really is one. The socket stays up. */
        private suspend fun dispatchGoal(call: LiveFunctionCall, goal: String) {
            when (val route = LiveRouter.decide(goal)) {
                is LiveRouter.Route.Talk -> {
                    BuddyLog.d("Live.route", "talk detail=${route.detail} goal=\"${goal.take(80)}\"")
                    socket?.send(LiveMessages.toolResponse(call.id, call.name, LiveRouter.talkReply(), describe(BuddyScreenEyes.snapshot())))
                }
                is LiveRouter.Route.Act -> {
                    BuddyLog.d("Live.route", "act ${route.action.describe()} goal=\"${goal.take(80)}\"")
                    if (AgentRunner.isActive()) {
                        socket?.send(LiveMessages.toolResponse(call.id, call.name, "ignored — a job has the screen. Just talk.", describe(BuddyScreenEyes.snapshot())))
                    } else perform(call, route.action)
                }
                is LiveRouter.Route.Goal -> startJob(call, route.goal)
            }
        }

        /** Ack at once so Gemini 3.1 Flash Live can keep talking (sync tools only), then run the worker. */
        private fun startJob(call: LiveFunctionCall, goal: String) {
            if (AgentRunner.isActive()) {
                socket?.send(LiveMessages.toolResponse(call.id, call.name, "already working — keep talking, or cancel_job if they asked to stop", describe(BuddyScreenEyes.snapshot())))
                return
            }
            BuddyLog.d("Live.job", "start=\"${goal.take(80)}\"")
            ActivityLog.record(goal)
            socket?.send(LiveMessages.toolResponse(call.id, call.name, "started — keep this talk. Do not use screen tools. JOB DONE will arrive when it finishes.", describe(BuddyScreenEyes.snapshot())))
            AgentRunner.ensure(app).start(goal, LiveDesk(send = ::emit, onFinished = ::afterJob))
        }

        private fun replyJob(call: LiveFunctionCall, text: String) {
            if (!AgentRunner.isAwaitingAnswer()) {
                socket?.send(LiveMessages.toolResponse(call.id, call.name, "no question pending — just talk", describe(BuddyScreenEyes.snapshot())))
                return
            }
            BuddyLog.d("Live.job", "answer=\"${text.take(60)}\"")
            AgentRunner.answer(text)
            socket?.send(LiveMessages.toolResponse(call.id, call.name, "got it — the job will go on", describe(BuddyScreenEyes.snapshot())))
        }

        private fun stopJob(call: LiveFunctionCall) {
            if (!AgentRunner.isActive()) {
                socket?.send(LiveMessages.toolResponse(call.id, call.name, "no job is running", describe(BuddyScreenEyes.snapshot())))
                return
            }
            BuddyLog.d("Live.job", "cancel")
            AgentRunner.cancel()
            afterJob()
            socket?.send(LiveMessages.toolResponse(call.id, call.name, "stopped — tell them you left that alone", describe(BuddyScreenEyes.snapshot())))
        }

        private fun emit(text: String) { if (active) socket?.send(text) }

        private fun afterJob() {
            lastScene = ""
            if (active) pushCatalog(force = true)
        }

        /** Same grounded lookup as the runner — not Live's built-in search, which can kill the socket. */
        private suspend fun lookup(call: LiveFunctionCall, query: String) {
            val outcome = search.lookup(query)
            val result = if (outcome.ok) outcome.detail else "search failed — ${outcome.detail}. Answer from what you know, or say you could not look it up."
            socket?.send(LiveMessages.toolResponse(call.id, call.name, result, describe(BuddyScreenEyes.snapshot())))
        }

        private suspend fun perform(call: LiveFunctionCall, action: AgentAction) {
            val outcome = executor.perform(action, BuddyScreenEyes.snapshot())
            val after = awaitSettledSnapshot()
            lastScene = after.sceneKey()
            socket?.send(LiveMessages.toolResponse(call.id, call.name, if (outcome.ok) outcome.detail else "not done — ${outcome.detail}", describe(after)))
        }

        private suspend fun followScreen(id: Int) {
            BuddyScreenEyes.scenes.collect { snap ->
                if (id != gen || !active || socket?.isReady != true || listen.greeting) return@collect
                pushCatalog(snap)
            }
        }

        /** Latest SCREEN only — force on each utterance so a leftover Wi‑Fi dump cannot linger. */
        private fun pushCatalog(snap: ScreenSnapshot = BuddyScreenEyes.snapshot(), force: Boolean = false) {
            val key = snap.sceneKey()
            if (!force && key == lastScene) return
            lastScene = key
            val catalog = describe(snap)
            BuddyLog.d("Live.catalog", "pkg=${snap.packageName} nodes=${snap.nodes.size} chars=${catalog.length} force=$force")
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
