package com.sandarva.kotlinapps.brain

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sandarva.kotlinapps.BuildConfig
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.accessibility.BuddyHands
import com.sandarva.kotlinapps.accessibility.BuddyType
import com.sandarva.kotlinapps.brain.agent.AgentRunner
import com.sandarva.kotlinapps.brain.agent.SpokenDesk
import com.sandarva.kotlinapps.brain.live.BuddyLive
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import com.sandarva.kotlinapps.overlay.CursorLanding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Brain facade — the doors. Voice is Gemini Live (one session). Typed words are routed: a nudge,
 * a tap-here, or a type-here runs on-device; everything else becomes a goal for the silent runner.
 * While the runner is waiting on a question from the typed sheet, the next words are its answer.
 */
object BuddyBrain {
    @Volatile private var engine: Engine? = null

    fun ensure(app: Application): Engine {
        engine?.let { return it }
        BuddyLive.ensure(app)
        AgentRunner.ensure(app)
        return Engine(app).also { engine = it }
    }

    fun ask(text: String) { engine?.ask(text) }
    fun listen() { engine?.openAsk() }
    fun openAsk() { engine?.openAsk() }
    fun openTypeAsk() { engine?.openTypeAsk() }
    fun cancel() { engine?.cancel() }

    class Engine(private val app: Application) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val voice = BuddyVoice(app)
        private var job: Job? = null
        @Volatile private var sessionActive = false

        fun ask(text: String) = heard(text)

        fun listen() = openAsk()

        /** Double-tap / Ask buddy: start a live talk, or the type sheet — or end whatever is running. */
        fun openAsk() {
            if (BuddyLive.isActive() || AgentRunner.isActive()) {
                BuddyLog.d("Brain.openAsk", "toggle off live=${BuddyLive.isActive()} agent=${AgentRunner.isActive()}")
                cancel()
                return
            }
            if (BrainSession.phase.value == BrainPhase.Thinking && sessionActive) {
                BuddyLog.d("Brain.openAsk", "ignored — already thinking")
                return
            }
            val mic = hasMic()
            val live = mic && BuildConfig.GEMINI_API_KEY.isNotBlank()
            BuddyLog.d("Brain.openAsk", "mic=$mic live=$live askOpen=${BrainSession.askOpen.value}")
            cancelJob()
            voice.cancelListen()
            if (live) {
                sessionActive = false
                BuddyLive.ensure(app).start()
                return
            }
            openSheet(note = if (mic) null else app.getString(R.string.ask_type_only), listen = mic)
        }

        fun openTypeAsk() {
            BuddyLog.d("Brain.openTypeAsk", "from live")
            AgentRunner.cancel()
            BuddyLive.stop()
            cancelJob()
            voice.cancelListen()
            BrainSession.setLiveOpen(false)
            openSheet(note = null, listen = false)
        }

        /** The runner asked something: show the question on the sheet and listen for the reply. */
        fun openAnswer(question: String) {
            BuddyLog.d("Brain.openAnswer", "q=\"${question.take(60)}\"")
            cancelJob()
            voice.cancelListen()
            openSheet(note = question, listen = hasMic())
        }

        private fun openSheet(note: String?, listen: Boolean) {
            sessionActive = true
            BrainSession.setAskOpen(true)
            BrainSession.setNote(note)
            BrainSession.setPhase(BrainPhase.Idle)
            if (listen) {
                BrainSession.setPhase(BrainPhase.Listening)
                voice.listen(onText = ::heard, onFailed = ::failListen)
            }
        }

        /** Speak or type — same door. The sheet must be gone before eyes or hands run. */
        private fun heard(text: String) {
            val trimmed = text.trim()
            BuddyLog.d("Brain.heard", "len=${trimmed.length} awaiting=${AgentRunner.isAwaitingAnswer()} askOpen=${BrainSession.askOpen.value}")
            if (trimmed.isBlank()) return
            voice.cancelListen()
            BrainSession.setAskOpen(false)
            BrainSession.setNote(null)
            if (AgentRunner.isAwaitingAnswer()) {
                sessionActive = false
                AgentRunner.answer(trimmed)
                return
            }
            sessionActive = true
            think(trimmed)
        }

        fun cancel() {
            BuddyLog.d("Brain.cancel", "sessionActive=$sessionActive live=${BuddyLive.isActive()} agent=${AgentRunner.isActive()}")
            sessionActive = false
            cancelJob()
            voice.cancelAll()
            AgentRunner.cancel()
            BuddyLive.stop()
            BrainSession.reset()
        }

        /** On-device verbs first (no network); everything else is a goal for the runner. */
        private fun think(request: String) {
            if (!sessionActive) return
            voice.cancelListen()
            cancelJob()
            BrainSession.setPhase(BrainPhase.Thinking)
            BrainSession.setNote(null)
            job = scope.launch {
                delay(TREE_SETTLE_MS)
                if (!sessionActive) return@launch
                if (tryLocalMove(request) || tryLocalType(request) || tryLocalHand(request)) return@launch
                sessionActive = false
                AgentRunner.start(request, SpokenDesk(voice, ::openAnswer) { BrainSession.reset() })
            }
        }

        private suspend fun tryLocalType(request: String): Boolean {
            val action = BuddyTypeIntent.parse(request) ?: return false
            BuddyLog.d("Brain.localType", "q=\"${request.take(80)}\" type=$action ready=${BuddyType.isReady()}")
            if (!BuddyType.isReady()) { failQuiet(HANDS_OFF_NOTE); return true }
            voice.speak(action.say)
            val ok = when (action) {
                is BuddyTypeIntent.Action.Type -> BuddyType.typeHere(action.text, action.submit)
                is BuddyTypeIntent.Action.Submit -> BuddyType.submitHere()
            }
            BuddyLog.d("Brain.localType", "ok=$ok")
            finishTurn()
            return true
        }

        private suspend fun tryLocalHand(request: String): Boolean {
            val hand = BuddyHandIntent.parse(request) ?: return false
            BuddyLog.d("Brain.localHand", "q=\"${request.take(80)}\" hand=$hand ready=${BuddyHands.isReady()}")
            if (!BuddyHands.isReady()) { failQuiet(HANDS_OFF_NOTE); return true }
            voice.speak(hand.say)
            val ok = when (hand) {
                is BuddyHandIntent.Action.Tap -> BuddyHands.tapHere()
                is BuddyHandIntent.Action.Hold -> BuddyHands.holdHere()
                is BuddyHandIntent.Action.Swipe -> BuddyHands.swipeHere(hand.dx, hand.dy)
                is BuddyHandIntent.Action.Drag -> BuddyHands.dragHere(hand.dx, hand.dy)
            }
            BuddyLog.d("Brain.localHand", "ok=$ok")
            finishTurn()
            return true
        }

        private fun tryLocalMove(request: String): Boolean {
            val move = BuddyMoveIntent.parse(request) ?: return false
            BuddyLog.d("Brain.localMove", "q=\"${request.take(80)}\" move=$move hands=${BuddyCursorController.isAttached()}")
            when (move) {
                is BuddyMoveIntent.Move.ToPlace -> CursorLanding.normalized(move.place)?.let { BuddyCursorController.animateToNormalized(it.first, it.second) }
                is BuddyMoveIntent.Move.Nudge -> BuddyCursorController.nudgeNormalized(move.dx, move.dy)
            }
            voice.speak(move.say)
            finishTurn()
            return true
        }

        private fun finishTurn() {
            sessionActive = false
            BrainSession.reset()
        }

        /** Keep the panel up with the note so they can see what went wrong and try again. */
        private fun failQuiet(message: String) {
            BuddyLog.d("Brain.failQuiet", "sessionActive=$sessionActive msg=$message")
            if (!sessionActive) return
            BrainSession.setPhase(BrainPhase.Idle)
            BrainSession.setNote(message)
            BrainSession.setAskOpen(true)
        }

        private fun failListen(message: String) {
            if (job?.isActive == true) {
                BuddyLog.d("Brain.failListen", "ignored — think already running")
                return
            }
            failQuiet(message)
        }

        private fun hasMic(): Boolean = ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        private fun cancelJob() { job?.cancel(); job = null }

        companion object {
            /** Let the ask panel leave the accessibility tree before we snapshot. */
            private const val TREE_SETTLE_MS = 320L
            private const val HANDS_OFF_NOTE = "Turn on Buddy Assistant so I can tap and type for you."
        }
    }
}
