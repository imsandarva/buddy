package com.sandarva.kotlinapps.brain.agent

import android.app.Application
import com.sandarva.kotlinapps.BuildConfig
import com.sandarva.kotlinapps.accessibility.BuddyHands
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
import com.sandarva.kotlinapps.accessibility.awaitReadableSnapshot
import com.sandarva.kotlinapps.accessibility.awaitSettledSnapshot
import com.sandarva.kotlinapps.brain.BrainPhase
import com.sandarva.kotlinapps.brain.BrainSession
import com.sandarva.kotlinapps.brain.GeminiClient
import com.sandarva.kotlinapps.brain.Reachability
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.OverlayNotifier
import com.sandarva.kotlinapps.overlay.OverlaySession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Silent screen worker: see → decide → check → act → settle → see what changed → repeat.
 * It never talks to the person. [AgentDesk] is how a result or a question leaves this loop.
 */
object AgentRunner {
    @Volatile private var session: Session? = null
    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    fun ensure(app: Application): Session {
        session?.let { return it }
        return Session(app).also { session = it }
    }

    fun isActive(): Boolean = _state.value.busy
    fun isAwaitingAnswer(): Boolean = _state.value is AgentState.Asking
    fun start(goal: String, desk: AgentDesk) { session?.start(goal, desk) }
    fun answer(text: String) { session?.answer(text) }
    fun cancel() { session?.cancel() }

    class Session(private val app: Application) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val gemini = GeminiClient(BuildConfig.GEMINI_API_KEY)
        private val decider = AgentDecider(gemini)
        private val search = WebSearch(gemini)
        private val executor = AgentExecutor(AppLauncher(app))
        private var job: Job? = null
        private var wrap = 0
        private var pendingAnswer: CompletableDeferred<String>? = null
        @Volatile private var desk: AgentDesk? = null

        fun start(goal: String, desk: AgentDesk) {
            val trimmed = goal.trim()
            if (trimmed.isBlank()) return
            cancel()
            wrap += 1
            this.desk = desk
            _state.value = AgentState.Working(0, null)
            BrainSession.setAskOpen(false)
            BrainSession.setGoalOpen(true)
            BrainSession.setNote(null)
            BrainSession.setProgress(null)
            if (!desk.holdsTalk) {
                BrainSession.setLiveOpen(false)
                BrainSession.setPhase(BrainPhase.Working)
            }
            OverlayNotifier.sync(app)
            BuddyLog.d("Agent.start", "goal=\"${trimmed.take(80)}\" talk=${desk.holdsTalk}")
            job = scope.launch { run(trimmed, desk) }
        }

        fun answer(text: String) {
            val reply = text.trim()
            if (reply.isBlank()) return
            BuddyLog.d("Agent.answer", "\"${reply.take(60)}\"")
            pendingAnswer?.complete(reply)
        }

        fun cancel() {
            if (!_state.value.busy && job == null) return
            BuddyLog.d("Agent.cancel", "state=${_state.value}")
            wrap += 1
            val held = desk?.holdsTalk == true
            desk = null
            pendingAnswer?.cancel(); pendingAnswer = null
            job?.cancel(); job = null
            OverlaySession.setThinking(false)
            _state.value = AgentState.Idle
            BrainSession.setGoalOpen(false)
            BrainSession.setProgress(null)
            if (!held && BrainSession.phase.value == BrainPhase.Working) BrainSession.setPhase(BrainPhase.Idle)
            OverlayNotifier.sync(app)
        }

        private suspend fun run(goal: String, desk: AgentDesk) {
            when {
                BuildConfig.GEMINI_API_KEY.isBlank() -> return end("I don’t have a way to think yet.", desk)
                !Reachability.online(app) -> return end(OFFLINE, desk)
                !BuddyScreenEyes.isReady() || !BuddyHands.isReady() -> return end(HANDS_OFF, desk)
            }
            if (!desk.holdsTalk) desk.say("On it.")
            val memory = AgentMemory(goal)
            val guard = AgentGuard(goal)
            var scene = SceneDescriber.withoutEcho(awaitReadableSnapshot(), goal)
            var hint: String? = null
            try {
                for (step in 1..AgentGuard.MAX_STEPS + 1) {
                    _state.value = AgentState.Working(step, memory.progress.ifBlank { null })
                    val decision = think(memory, scene, hint, guard)
                    if (decision == null) { hint = guard.hint(); continue }
                    memory.update(decision.progress)
                    decision.say?.let { narrate(it, desk) }
                    when (val verdict = guard.review(step, decision, scene)) {
                        is AgentGuard.Verdict.Stop -> return end(verdict.message, desk)
                        is AgentGuard.Verdict.Confirm -> {
                            val reply = askPerson(verdict.question, desk) ?: return end(NO_ANSWER, desk)
                            if (isNo(reply)) return end("Okay — I’ll leave that alone.", desk)
                            if (isYes(reply)) guard.confirmed = true
                            memory.recordAnswer(reply)
                            scene = SceneDescriber.withoutEcho(awaitReadableSnapshot(), goal)
                            hint = null
                        }
                        AgentGuard.Verdict.Proceed -> {
                            val action = decision.action
                            if (action is AgentAction.Ask) {
                                val reply = askPerson(action.question, desk) ?: return end(NO_ANSWER, desk)
                                if (isYes(reply)) guard.confirmed = true
                                memory.recordAnswer(reply)
                                scene = SceneDescriber.withoutEcho(awaitReadableSnapshot(), goal)
                                hint = null
                                continue
                            }
                            if (action is AgentAction.SearchWeb) {
                                val outcome = search.lookup(action.query)
                                memory.record(action, outcome, change = null)
                                guard.observe(action, outcome, change = null)
                                hint = if (outcome.ok) null else "Search did not help. Try a different query, or continue from SCREEN."
                                continue
                            }
                            val outcome = executor.perform(action, scene)
                            val finish = decision.finish
                            if (finish != null && (outcome.ok || action == AgentAction.None)) return end(finish.message.ifBlank { if (finish.succeeded) "That’s done." else "I couldn’t finish that." }, desk)
                            val after = if (action.changesPhone) awaitSettledSnapshot() else BuddyScreenEyes.snapshot()
                            val change = if (action.changesPhone) SceneDiff.describe(scene, after) else null
                            memory.record(action, outcome, change)
                            guard.observe(action, outcome, change)
                            hint = guard.hint()
                            scene = SceneDescriber.withoutEcho(after, goal)
                        }
                    }
                }
                end("That’s as far as I could take it on my own.", desk)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                BuddyLog.e("Agent.fail", error.message ?: "unknown", error)
                end(if (Reachability.isNetworkFailure(error)) OFFLINE else THINK_FAIL, desk)
            }
        }

        private suspend fun think(memory: AgentMemory, scene: ScreenSnapshot, hint: String?, guard: AgentGuard): AgentDecision? {
            OverlaySession.setThinking(true)
            val decision = try {
                decider.decide(memory, SceneDescriber.describe(scene), hint, guard.deeperThought())
            } finally {
                OverlaySession.setThinking(false)
            }
            val empty = decision.action == AgentAction.None && decision.finish == null
            if (empty) { guard.observeInvalidReply(); return null }
            guard.observeValidReply()
            return decision
        }

        /** Ask through the desk, then wait for their words (or give up after a while). */
        private suspend fun askPerson(question: String, desk: AgentDesk): String? {
            BuddyLog.d("Agent.ask", "\"${question.take(80)}\"")
            _state.value = AgentState.Asking(question)
            BrainSession.setProgress(question)
            val waiting = CompletableDeferred<String>()
            pendingAnswer = waiting
            desk.ask(question)
            val reply = withTimeoutOrNull(ASK_TIMEOUT_MS) { waiting.await() }
            pendingAnswer = null
            BrainSession.setAskOpen(false)
            BrainSession.setNote(null)
            if (!desk.holdsTalk) BrainSession.setPhase(BrainPhase.Working)
            _state.value = AgentState.Working(0, null)
            return reply
        }

        private fun narrate(line: String, desk: AgentDesk) {
            BrainSession.setProgress(line)
            desk.say(line)
        }

        private fun end(message: String, desk: AgentDesk) {
            job = null
            this.desk = null
            _state.value = AgentState.Idle
            BrainSession.setGoalOpen(false)
            BrainSession.setProgress(null)
            if (!desk.holdsTalk && BrainSession.phase.value == BrainPhase.Working) BrainSession.setPhase(BrainPhase.Idle)
            OverlayNotifier.sync(app)
            BuddyLog.d("Agent.end", "talk=${desk.holdsTalk} message=\"${message.take(80)}\"")
            desk.done(message)
        }

        fun release() {
            cancel()
            scope.cancel()
        }
    }

    private fun isYes(reply: String): Boolean = YES.containsMatchIn(reply.lowercase()) && !isNo(reply)
    private fun isNo(reply: String): Boolean = NO.containsMatchIn(reply.lowercase())

    private val YES = Regex("""\b(yes|yeah|yep|sure|ok|okay|go ahead|do it|please do|fine|correct)\b""")
    private val NO = Regex("""\b(no|nope|don't|dont|do not|stop|cancel|never mind|nevermind|leave it)\b""")
    private const val ASK_TIMEOUT_MS = 90_000L
    private const val OFFLINE = "I can’t reach the internet just now. Check the connection and try again."
    private const val THINK_FAIL = "I couldn’t think that through just now. Try once more in a moment."
    private const val HANDS_OFF = "Turn on Buddy Assistant so I can see and tap for you."
    private const val NO_ANSWER = "I didn’t catch an answer, so I’ve stopped for now. Ask me again whenever you like."
}
