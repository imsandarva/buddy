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
import com.sandarva.kotlinapps.brain.BuddyVoice
import com.sandarva.kotlinapps.brain.GeminiClient
import com.sandarva.kotlinapps.brain.Reachability
import com.sandarva.kotlinapps.brain.live.BuddyLive
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
 * The agent loop: see → decide → check → act → let the screen settle → see what changed → repeat.
 * Live and typed asks hand a goal here; the runner owns continue, ask, and stop. Eyes and hands stay dumb.
 */
object AgentRunner {
    /** Opens the ask panel and listens; the reply comes back through [answer]. Set by the brain door. */
    fun interface AnswerDoor { fun open(question: String) }

    @Volatile private var session: Session? = null
    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    fun ensure(app: Application): Session {
        session?.let { return it }
        return Session(app).also { session = it }
    }

    fun isActive(): Boolean = _state.value.busy
    fun isAwaitingAnswer(): Boolean = _state.value is AgentState.Asking
    fun start(goal: String, resumeLive: Boolean) { session?.start(goal, resumeLive) }
    fun answer(text: String) { session?.answer(text) }
    fun cancel() { session?.cancel() }

    class Session(private val app: Application) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val decider = AgentDecider(GeminiClient(BuildConfig.GEMINI_API_KEY))
        private val executor = AgentExecutor(AppLauncher(app))
        private val voice = BuddyVoice(app)
        private var job: Job? = null
        private var wrap = 0
        private var pendingAnswer: CompletableDeferred<String>? = null
        var door: AnswerDoor? = null

        fun start(goal: String, resumeLive: Boolean) {
            val trimmed = goal.trim()
            if (trimmed.isBlank()) return
            cancel()
            wrap += 1
            _state.value = AgentState.Working(0, null)
            BrainSession.setAskOpen(false)
            BrainSession.setLiveOpen(false)
            BrainSession.setGoalOpen(true)
            BrainSession.setPhase(BrainPhase.Working)
            BrainSession.setNote(null)
            BrainSession.setProgress(null)
            OverlayNotifier.sync(app)
            BuddyLog.d("Agent.start", "goal=\"${trimmed.take(80)}\" resumeLive=$resumeLive")
            job = scope.launch { run(trimmed, resumeLive) }
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
            voice.cancelAll()
            pendingAnswer?.cancel(); pendingAnswer = null
            job?.cancel(); job = null
            OverlaySession.setThinking(false)
            _state.value = AgentState.Idle
            BrainSession.setGoalOpen(false)
            BrainSession.setProgress(null)
            if (BrainSession.phase.value == BrainPhase.Working) BrainSession.setPhase(BrainPhase.Idle)
            OverlayNotifier.sync(app)
        }

        private suspend fun run(goal: String, resumeLive: Boolean) {
            when {
                BuildConfig.GEMINI_API_KEY.isBlank() -> return end("I don’t have a way to think yet.", resumeLive)
                !Reachability.online(app) -> return end(OFFLINE, resumeLive)
                !BuddyScreenEyes.isReady() || !BuddyHands.isReady() -> return end(HANDS_OFF, resumeLive)
            }
            voice.speak("On it.")
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
                    decision.say?.let { narrate(it) }
                    when (val verdict = guard.review(step, decision, scene)) {
                        is AgentGuard.Verdict.Stop -> return end(verdict.message, resumeLive)
                        is AgentGuard.Verdict.Confirm -> {
                            val reply = askPerson(verdict.question) ?: return end(NO_ANSWER, resumeLive)
                            if (isNo(reply)) return end("Okay — I’ll leave that alone.", resumeLive)
                            if (isYes(reply)) guard.confirmed = true
                            memory.recordAnswer(reply)
                            scene = SceneDescriber.withoutEcho(awaitReadableSnapshot(), goal)
                            hint = null
                        }
                        AgentGuard.Verdict.Proceed -> {
                            val action = decision.action
                            if (action is AgentAction.Ask) {
                                val reply = askPerson(action.question) ?: return end(NO_ANSWER, resumeLive)
                                if (isYes(reply)) guard.confirmed = true
                                memory.recordAnswer(reply)
                                scene = SceneDescriber.withoutEcho(awaitReadableSnapshot(), goal)
                                hint = null
                                continue
                            }
                            val outcome = executor.perform(action, scene)
                            val finish = decision.finish
                            if (finish != null && (outcome.ok || action == AgentAction.None)) return end(finish.message.ifBlank { if (finish.succeeded) "That’s done." else "I couldn’t finish that." }, resumeLive)
                            val after = if (action.changesPhone) awaitSettledSnapshot() else BuddyScreenEyes.snapshot()
                            val change = if (action.changesPhone) SceneDiff.describe(scene, after) else null
                            memory.record(action, outcome, change)
                            guard.observe(action, outcome, change)
                            hint = guard.hint()
                            scene = SceneDescriber.withoutEcho(after, goal)
                        }
                    }
                }
                end("That’s as far as I could take it on my own.", resumeLive)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                BuddyLog.e("Agent.fail", error.message ?: "unknown", error)
                end(if (Reachability.isNetworkFailure(error)) OFFLINE else THINK_FAIL, resumeLive)
            }
        }

        /** One model call with the cursor in its thinking mood. A reply the app cannot read counts against the guard. */
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

        /** Speak the question, then open the door and wait for their words (or give up after a while). */
        private suspend fun askPerson(question: String): String? {
            BuddyLog.d("Agent.ask", "\"${question.take(80)}\"")
            _state.value = AgentState.Asking(question)
            BrainSession.setProgress(question)
            val waiting = CompletableDeferred<String>()
            pendingAnswer = waiting
            voice.speak(question) { door?.open(question) }
            val reply = withTimeoutOrNull(ASK_TIMEOUT_MS) { waiting.await() }
            pendingAnswer = null
            BrainSession.setAskOpen(false)
            BrainSession.setNote(null)
            BrainSession.setPhase(BrainPhase.Working)
            _state.value = AgentState.Working(0, null)
            return reply
        }

        /** Milestone lines only — never talk over a sentence still being said. */
        private fun narrate(line: String) {
            BrainSession.setProgress(line)
            if (!voice.isSpeaking()) voice.speak(line)
        }

        /** Wrap up: say the closing line, then hand the conversation back to Live if that is where it came from. */
        private fun end(message: String, resumeLive: Boolean) {
            val id = wrap
            job = null
            _state.value = AgentState.Finishing(message)
            BrainSession.setGoalOpen(false)
            BrainSession.setProgress(null)
            BrainSession.setPhase(BrainPhase.Idle)
            OverlayNotifier.sync(app)
            BuddyLog.d("Agent.end", "resumeLive=$resumeLive message=\"${message.take(80)}\"")
            voice.speak(message) {
                if (id != wrap) return@speak
                _state.value = AgentState.Idle
                if (resumeLive) BuddyLive.ensure(app).start(afterGoal = true) else BrainSession.reset()
            }
        }

        fun release() {
            cancel()
            voice.release()
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
