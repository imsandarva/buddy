package com.sandarva.kotlinapps.brain

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sandarva.kotlinapps.BuildConfig
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
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
 * Brain facade. Snapshot → Gemini tools → speak + fly or point.
 * Live Mode is a later adapter on this same string.
 */
object BuddyBrain {
    @Volatile private var engine: Engine? = null

    fun ensure(app: Application): Engine {
        engine?.let { return it }
        return Engine(app).also { engine = it }
    }

    fun ask(text: String) { engine?.ask(text) }
    fun listen() { engine?.openAsk() }
    fun openAsk() { engine?.openAsk() }
    fun listenAfterPrompt() { engine?.openAsk() }
    fun cancel() { engine?.cancel() }

    class Engine(private val app: Application) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val voice = BuddyVoice(app)
        private val gemini = GeminiClient(BuildConfig.GEMINI_API_KEY)
        private var job: Job? = null
        @Volatile private var sessionActive = false
        @Volatile private var reopenAskOnFail = false

        fun ask(text: String) {
            val trimmed = text.trim()
            BuddyLog.d("Brain.ask", "len=${trimmed.length} sessionActive=$sessionActive")
            if (trimmed.isBlank()) return
            sessionActive = true
            reopenAskOnFail = true
            voice.cancelListen()
            // The ask panel covers the screen; eyes must see the real controls, not the typed field.
            BrainSession.setAskOpen(false)
            BrainSession.setNote(null)
            think(trimmed, settleMs = TREE_SETTLE_MS) { BuddyScreenEyes.snapshot() }
        }

        fun listen() = openAsk()

        fun openAsk() {
            if (BrainSession.phase.value == BrainPhase.Thinking && sessionActive) {
                BuddyLog.d("Brain.openAsk", "ignored — already thinking")
                return
            }
            val mic = ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            BuddyLog.d("Brain.openAsk", "mic=$mic askOpen=${BrainSession.askOpen.value}")
            cancelJob()
            voice.cancelListen()
            sessionActive = true
            reopenAskOnFail = true
            val early = BuddyScreenEyes.snapshot()
            BrainSession.setAskOpen(true)
            BrainSession.setNote(if (mic) null else app.getString(R.string.ask_type_only))
            if (mic) {
                BrainSession.setPhase(BrainPhase.Listening)
                voice.listen(onText = { think(it) { richerSnap(early) } }, onFailed = ::failListen)
            } else {
                BrainSession.setPhase(BrainPhase.Idle)
            }
        }

        fun cancel() {
            BuddyLog.d("Brain.cancel", "close ask panel sessionActive=$sessionActive")
            sessionActive = false
            reopenAskOnFail = false
            cancelJob()
            voice.cancelAll()
            BrainSession.reset()
        }

        private fun think(question: String, settleMs: Long = 0, snapshot: () -> ScreenSnapshot) {
            if (!sessionActive) return
            voice.cancelListen()
            cancelJob()
            BrainSession.setPhase(BrainPhase.Thinking)
            BrainSession.setNote(null)
            job = scope.launch {
                if (tryLocalMove(question)) return@launch
                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    failQuiet("I don’t have a way to think yet.")
                    return@launch
                }
                if (!Reachability.online(app)) {
                    failQuiet(OFFLINE_NOTE)
                    return@launch
                }
                if (settleMs > 0) delay(settleMs)
                if (!sessionActive) return@launch
                val snap = GuidanceCatalog.forModel(snapshot(), question)
                BuddyLog.d("Brain.runGuide", "q=\"${question.take(80)}\" nodes=${snap.nodes.size} pkg=${snap.packageName} ids=${snap.nodes.take(12).joinToString { it.id }} hands=${BuddyCursorController.isAttached()}")
                try {
                    val plan = gemini.guide(question, GuidanceCatalog.format(snap))
                    BuddyLog.d("Brain.plan", "say=\"${plan.say?.take(80)}\" place=${plan.place} elementId=${plan.elementId}")
                    apply(plan, snap)
                } catch (error: Exception) {
                    BuddyLog.e("Brain.guideFail", error.message ?: "unknown", error)
                    failQuiet(if (Reachability.isNetworkFailure(error)) OFFLINE_NOTE else THINK_FAIL_NOTE)
                }
            }
        }

        /** Prefer the live tree (launcher under the ask overlay) over a tap-time empty snap. */
        private fun richerSnap(early: ScreenSnapshot): ScreenSnapshot {
            val live = BuddyScreenEyes.snapshot()
            return if (live.nodes.size >= early.nodes.size) live else early
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
            sessionActive = false
            reopenAskOnFail = false
            BrainSession.reset()
            return true
        }

        private fun apply(plan: GuidancePlan, snapshot: ScreenSnapshot) {
            if (!sessionActive) {
                BuddyLog.d("Brain.apply", "ignored — session already closed")
                return
            }
            when {
                !plan.place.isNullOrBlank() -> fly(plan.place)
                !plan.elementId.isNullOrBlank() -> point(plan.elementId, snapshot)
            }
            plan.say?.takeIf { it.isNotBlank() }?.let { voice.speak(it) }
            sessionActive = false
            reopenAskOnFail = false
            BrainSession.reset()
        }

        private fun fly(place: String) {
            val xy = CursorLanding.normalized(place)
            val ok = xy != null && BuddyCursorController.animateToNormalized(xy.first, xy.second)
            BuddyLog.d("Brain.fly", "place=$place xy=$xy ok=$ok")
        }

        private fun point(id: String, snapshot: ScreenSnapshot) {
            val node = snapshot.node(id)
            val ok = node != null && BuddyScreenEyes.pointTo(node)
            BuddyLog.d("Brain.point", "id=$id found=${node != null} ok=$ok")
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
        }
    }
}
