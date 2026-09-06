package com.sandarva.kotlinapps.brain

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sandarva.kotlinapps.BuildConfig
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
import com.sandarva.kotlinapps.debug.BuddyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Brain facade. Snapshot → Gemini tools → speak + point.
 * Live Mode is a later adapter on this same string.
 */
object BuddyBrain {
    @Volatile private var engine: Engine? = null

    fun ensure(app: Application): Engine {
        engine?.let { return it }
        return Engine(app).also { engine = it }
    }

    fun ask(text: String) { engine?.ask(text) }
    fun listen() { engine?.listen() }
    fun listenAfterPrompt() { engine?.listenAfterPrompt() }
    fun cancel() { engine?.cancel() }

    class Engine(private val app: Application) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val voice = BuddyVoice(app)
        private val gemini = GeminiClient(BuildConfig.GEMINI_API_KEY)
        private var job: Job? = null
        @Volatile private var sessionActive = false

        fun ask(text: String) {
            val trimmed = text.trim()
            BuddyLog.d("Brain.ask", "len=${trimmed.length} sessionActive=$sessionActive")
            if (trimmed.isBlank()) return
            sessionActive = true
            runGuide(trimmed, BuddyScreenEyes.snapshot())
        }

        fun listen() {
            BuddyLog.d("Brain.listen", "open ask panel")
            cancelJob()
            sessionActive = true
            val snap = BuddyScreenEyes.snapshot()
            BrainSession.setAskOpen(true)
            BrainSession.setNote(null)
            BrainSession.setPhase(BrainPhase.Listening)
            voice.listen(onText = { runGuide(it, snap) }, onFailed = ::failQuiet)
        }

        fun listenAfterPrompt() {
            val mic = ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            BuddyLog.d("Brain.listenAfterPrompt", "mic=$mic")
            if (!mic) {
                voice.speak("Open Buddy and allow the microphone first.")
                return
            }
            cancelJob()
            sessionActive = true
            val snap = BuddyScreenEyes.snapshot()
            BrainSession.setNote(null)
            BrainSession.setPhase(BrainPhase.Listening)
            voice.speak("What do you need?") {
                if (!sessionActive) return@speak
                voice.listen(onText = { runGuide(it, snap) }, onFailed = { voice.speak(it); if (sessionActive) BrainSession.setPhase(BrainPhase.Idle) })
            }
        }

        fun cancel() {
            BuddyLog.d("Brain.cancel", "close ask panel sessionActive=$sessionActive")
            sessionActive = false
            cancelJob()
            voice.cancelAll()
            BrainSession.reset()
        }

        private fun runGuide(question: String, snapshot: ScreenSnapshot) {
            BuddyLog.d("Brain.runGuide", "q=\"${question.take(80)}\" nodes=${snapshot.nodes.size} pkg=${snapshot.packageName} keyBlank=${BuildConfig.GEMINI_API_KEY.isBlank()}")
            if (!sessionActive) return
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                failQuiet("I don’t have a way to think yet.")
                return
            }
            cancelJob()
            BrainSession.setPhase(BrainPhase.Thinking)
            BrainSession.setNote(null)
            job = scope.launch {
                try {
                    val plan = gemini.guide(question, GuidanceCatalog.format(snapshot))
                    BuddyLog.d("Brain.plan", "say=\"${plan.say?.take(80)}\" elementId=${plan.elementId}")
                    apply(plan, snapshot)
                } catch (error: Exception) {
                    BuddyLog.e("Brain.guideFail", error.message ?: "unknown", error)
                    failQuiet("I couldn’t think that through just now. Try once more in a moment.")
                }
            }
        }

        private fun apply(plan: GuidancePlan, snapshot: ScreenSnapshot) {
            if (!sessionActive) {
                BuddyLog.d("Brain.apply", "ignored — session already closed")
                return
            }
            plan.elementId?.let { id ->
                val node = snapshot.node(id)
                val ok = if (node != null) BuddyScreenEyes.pointTo(node) else BuddyScreenEyes.pointTo(id)
                BuddyLog.d("Brain.point", "id=$id found=${node != null} ok=$ok")
            }
            plan.say?.takeIf { it.isNotBlank() }?.let { voice.speak(it) }
            sessionActive = false
            BrainSession.reset()
        }

        /** Keep the panel as-is if it is open; never reopen it after the user closed it. */
        private fun failQuiet(message: String) {
            BuddyLog.d("Brain.failQuiet", "sessionActive=$sessionActive askOpen=${BrainSession.askOpen.value} msg=$message")
            if (!sessionActive) return
            BrainSession.setPhase(BrainPhase.Idle)
            BrainSession.setNote(message)
        }

        private fun cancelJob() { job?.cancel(); job = null }
    }
}
