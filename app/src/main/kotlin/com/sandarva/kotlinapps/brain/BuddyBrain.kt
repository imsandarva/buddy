package com.sandarva.kotlinapps.brain

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sandarva.kotlinapps.BuildConfig
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
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

        fun ask(text: String) {
            val trimmed = text.trim()
            if (trimmed.isBlank()) return
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                fail("I don’t have a way to think yet.")
                return
            }
            runGuide(trimmed, BuddyScreenEyes.snapshot())
        }

        fun listen() {
            cancelJob()
            val snap = BuddyScreenEyes.snapshot()
            BrainSession.setAskOpen(true)
            BrainSession.setNote(null)
            BrainSession.setPhase(BrainPhase.Listening)
            voice.listen(onText = { runGuide(it, snap) }, onFailed = ::fail)
        }

        fun listenAfterPrompt() {
            if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                voice.speak("Open Buddy and allow the microphone first.")
                return
            }
            cancelJob()
            val snap = BuddyScreenEyes.snapshot()
            BrainSession.setNote(null)
            BrainSession.setPhase(BrainPhase.Listening)
            voice.speak("What do you need?") {
                voice.listen(onText = { runGuide(it, snap) }, onFailed = { voice.speak(it); BrainSession.setPhase(BrainPhase.Idle) })
            }
        }

        fun cancel() {
            cancelJob()
            voice.stopListening()
            BrainSession.reset()
        }

        private fun runGuide(question: String, snapshot: ScreenSnapshot) {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                fail("I don’t have a way to think yet.")
                return
            }
            cancelJob()
            BrainSession.setPhase(BrainPhase.Thinking)
            BrainSession.setNote(null)
            job = scope.launch {
                try {
                    apply(gemini.guide(question, GuidanceCatalog.format(snapshot)), snapshot)
                } catch (_: Exception) {
                    fail("I couldn’t think that through just now. Try once more in a moment.")
                }
            }
        }

        private fun apply(plan: GuidancePlan, snapshot: ScreenSnapshot) {
            plan.elementId?.let { id ->
                val node = snapshot.node(id)
                if (node != null) BuddyScreenEyes.pointTo(node) else BuddyScreenEyes.pointTo(id)
            }
            plan.say?.takeIf { it.isNotBlank() }?.let { voice.speak(it) }
            BrainSession.setPhase(BrainPhase.Idle)
            BrainSession.setAskOpen(false)
        }

        private fun fail(message: String) {
            BrainSession.setPhase(BrainPhase.Idle)
            BrainSession.setNote(message)
            BrainSession.setAskOpen(true)
        }

        private fun cancelJob() { job?.cancel(); job = null }
    }
}
