package com.sandarva.kotlinapps.brain

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sandarva.kotlinapps.BuildConfig
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.accessibility.BuddyHands
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
import com.sandarva.kotlinapps.brain.live.BuddyLive
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import com.sandarva.kotlinapps.overlay.CursorLanding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Brain facade. Typed asks use REST. Voice uses Gemini Live, same cursor tools.
 */
object BuddyBrain {
    @Volatile private var engine: Engine? = null

    fun ensure(app: Application): Engine {
        engine?.let { return it }
        BuddyLive.ensure(app)
        return Engine(app).also { engine = it }
    }

    fun ask(text: String) { engine?.ask(text) }
    fun listen() { engine?.openAsk() }
    fun openAsk() { engine?.openAsk() }
    fun openTypeAsk() { engine?.openTypeAsk() }
    fun listenAfterPrompt() { engine?.openAsk() }
    fun cancel() { engine?.cancel() }

    class Engine(private val app: Application) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val voice = BuddyVoice(app)
        private val gemini = GeminiClient(BuildConfig.GEMINI_API_KEY)
        private var job: Job? = null
        @Volatile private var sessionActive = false
        @Volatile private var reopenAskOnFail = false

        fun ask(text: String) = heard(text)

        fun listen() = openAsk()

        fun openAsk() {
            if (BuddyLive.isActive()) {
                BuddyLog.d("Brain.openAsk", "toggle live off")
                cancel()
                return
            }
            if (BrainSession.phase.value == BrainPhase.Thinking && sessionActive) {
                BuddyLog.d("Brain.openAsk", "ignored — already thinking")
                return
            }
            val mic = ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val live = mic && BuildConfig.GEMINI_API_KEY.isNotBlank()
            BuddyLog.d("Brain.openAsk", "mic=$mic live=$live askOpen=${BrainSession.askOpen.value}")
            cancelJob()
            voice.cancelListen()
            if (live) {
                sessionActive = false
                BuddyLive.ensure(app).start()
                return
            }
            sessionActive = true
            reopenAskOnFail = true
            BrainSession.setAskOpen(true)
            BrainSession.setNote(if (mic) null else app.getString(R.string.ask_type_only))
            BrainSession.setPhase(BrainPhase.Idle)
            if (mic) {
                BrainSession.setPhase(BrainPhase.Listening)
                voice.listen(onText = ::heard, onFailed = ::failListen)
            }
        }

        fun openTypeAsk() {
            BuddyLog.d("Brain.openTypeAsk", "from live")
            BuddyLive.stop()
            cancelJob()
            voice.cancelListen()
            sessionActive = true
            reopenAskOnFail = true
            BrainSession.setLiveOpen(false)
            BrainSession.setAskOpen(true)
            BrainSession.setPhase(BrainPhase.Idle)
            BrainSession.setNote(null)
        }

        /** Speak or type — same door. The sheet must be gone before eyes or hands run. */
        private fun heard(text: String) {
            val trimmed = text.trim()
            BuddyLog.d("Brain.heard", "len=${trimmed.length} sessionActive=$sessionActive askOpen=${BrainSession.askOpen.value}")
            if (trimmed.isBlank()) return
            sessionActive = true
            reopenAskOnFail = true
            voice.cancelListen()
            BrainSession.setAskOpen(false)
            BrainSession.setNote(null)
            think(trimmed)
        }

        fun cancel() {
            BuddyLog.d("Brain.cancel", "close ask panel sessionActive=$sessionActive live=${BuddyLive.isActive()}")
            sessionActive = false
            reopenAskOnFail = false
            cancelJob()
            voice.cancelAll()
            BuddyLive.stop()
            BrainSession.reset()
        }

        private fun think(question: String) {
            if (!sessionActive) return
            voice.cancelListen()
            cancelJob()
            BrainSession.setPhase(BrainPhase.Thinking)
            BrainSession.setNote(null)
            job = scope.launch {
                delay(TREE_SETTLE_MS)
                if (!sessionActive) return@launch
                if (tryLocalMove(question)) return@launch
                if (tryLocalHand(question)) return@launch
                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    failQuiet("I don’t have a way to think yet.")
                    return@launch
                }
                if (!Reachability.online(app)) {
                    failQuiet(OFFLINE_NOTE)
                    return@launch
                }
                val snap = GuidanceCatalog.forModel(BuddyScreenEyes.snapshot(), question)
                BuddyLog.d("Brain.runGuide", "q=\"${question.take(80)}\" nodes=${snap.nodes.size} pkg=${snap.packageName} ids=${snap.nodes.take(12).joinToString { it.id }} hands=${BuddyCursorController.isAttached()}")
                try {
                    val plan = gemini.guide(question, GuidanceCatalog.format(snap))
                    BuddyLog.d("Brain.plan", "say=\"${plan.say?.take(80)}\" place=${plan.place} elementId=${plan.elementId} hand=${plan.hand}")
                    apply(plan, snap)
                } catch (error: CancellationException) {
                    BuddyLog.d("Brain.guideCancel", "job cancelled — not a think failure")
                    throw error
                } catch (error: Exception) {
                    BuddyLog.e("Brain.guideFail", error.message ?: "unknown", error)
                    failQuiet(if (Reachability.isNetworkFailure(error)) OFFLINE_NOTE else THINK_FAIL_NOTE)
                }
            }
        }

        private suspend fun tryLocalHand(question: String): Boolean {
            val hand = BuddyHandIntent.parse(question) ?: return false
            BuddyLog.d("Brain.localHand", "q=\"${question.take(80)}\" hand=$hand ready=${BuddyHands.isReady()}")
            if (!sessionActive) return true
            if (!BuddyHands.isReady()) {
                failQuiet(HANDS_OFF_NOTE)
                return true
            }
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

        private fun tryLocalMove(question: String): Boolean {
            val move = BuddyMoveIntent.parse(question) ?: return false
            BuddyLog.d("Brain.localMove", "q=\"${question.take(80)}\" move=$move hands=${BuddyCursorController.isAttached()}")
            if (!sessionActive) return true
            when (move) {
                is BuddyMoveIntent.Move.ToPlace -> fly(move.place)
                is BuddyMoveIntent.Move.Nudge -> {
                    val ok = BuddyCursorController.nudgeNormalized(move.dx, move.dy)
                    BuddyLog.d("Brain.nudge", "dx=${move.dx} dy=${move.dy} ok=$ok")
                }
            }
            voice.speak(move.say)
            finishTurn()
            return true
        }

        private suspend fun apply(plan: GuidancePlan, snapshot: ScreenSnapshot) {
            if (!sessionActive) {
                BuddyLog.d("Brain.apply", "ignored — session already closed")
                return
            }
            if (plan.hand != null && !BuddyHands.isReady()) {
                failQuiet(HANDS_OFF_NOTE)
                return
            }
            plan.say?.takeIf { it.isNotBlank() }?.let { voice.speak(it) }
            GuidanceActor.run(plan, snapshot)
            finishTurn()
        }

        private fun finishTurn() {
            sessionActive = false
            reopenAskOnFail = false
            BrainSession.reset()
        }

        private fun fly(place: String) {
            val xy = CursorLanding.normalized(place)
            val ok = xy != null && BuddyCursorController.animateToNormalized(xy.first, xy.second)
            BuddyLog.d("Brain.fly", "place=$place xy=$xy ok=$ok")
        }

        /** Keep the panel as-is if it is open; reopen it after a typed ask so they can see the note. */
        private fun failQuiet(message: String) {
            BuddyLog.d("Brain.failQuiet", "sessionActive=$sessionActive askOpen=${BrainSession.askOpen.value} reopen=$reopenAskOnFail msg=$message")
            if (!sessionActive) return
            BrainSession.setPhase(BrainPhase.Idle)
            BrainSession.setNote(message)
            if (reopenAskOnFail) BrainSession.setAskOpen(true)
        }

        private fun failListen(message: String) {
            if (job?.isActive == true) {
                BuddyLog.d("Brain.failListen", "ignored — think already running")
                return
            }
            failQuiet(message)
        }

        private fun cancelJob() { job?.cancel(); job = null }

        companion object {
            /** Let the ask panel leave the accessibility tree before we snapshot. */
            private const val TREE_SETTLE_MS = 320L
            private const val OFFLINE_NOTE = "I can’t reach the internet just now. Check the connection and try again."
            private const val THINK_FAIL_NOTE = "I couldn’t think that through just now. Try once more in a moment."
            private const val HANDS_OFF_NOTE = "Turn on Buddy Assistant so I can tap for you."
        }
    }
}
