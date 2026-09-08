package com.sandarva.kotlinapps.brain.goal

import android.app.Application
import com.sandarva.kotlinapps.BuildConfig
import com.sandarva.kotlinapps.accessibility.BuddyHands
import com.sandarva.kotlinapps.accessibility.BuddyType
import com.sandarva.kotlinapps.accessibility.awaitReadableSnapshot
import com.sandarva.kotlinapps.accessibility.sceneKey
import com.sandarva.kotlinapps.brain.BrainPhase
import com.sandarva.kotlinapps.brain.BrainSession
import com.sandarva.kotlinapps.brain.BuddyVoice
import com.sandarva.kotlinapps.brain.GeminiClient
import com.sandarva.kotlinapps.brain.GuidanceActor
import com.sandarva.kotlinapps.brain.GuidanceCatalog
import com.sandarva.kotlinapps.brain.GuidancePlan
import com.sandarva.kotlinapps.brain.Reachability
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.OverlayNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Chat-loop hands for a multi-step goal. Live only hands off the goal string.
 * Eyes and hands stay the same; the app owns continue / stop.
 */
object GoalRunner {
    @Volatile private var session: Session? = null

    fun ensure(app: Application): Session {
        session?.let { return it }
        return Session(app).also { session = it }
    }

    fun isActive(): Boolean = session?.active == true
    fun start(goal: String) { session?.start(goal) }
    fun cancel() { session?.cancel() }

    class Session(private val app: Application) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val gemini = GeminiClient(BuildConfig.GEMINI_API_KEY)
        private val voice = BuddyVoice(app)
        private val launcher = AppLauncher(app)
        private var job: Job? = null
        @Volatile var active = false
            private set

        fun start(goal: String) {
            val trimmed = goal.trim()
            if (trimmed.isBlank()) return
            cancel()
            active = true
            BrainSession.setAskOpen(false)
            BrainSession.setLiveOpen(false)
            BrainSession.setGoalOpen(true)
            BrainSession.setPhase(BrainPhase.Working)
            BrainSession.setNote(null)
            OverlayNotifier.sync(app)
            BuddyLog.d("Goal.start", "goal=\"${trimmed.take(80)}\"")
            job = scope.launch { run(trimmed) }
        }

        fun cancel() {
            if (!active && job == null) return
            BuddyLog.d("Goal.cancel", "wasActive=$active")
            job?.cancel(); job = null
            finish(speak = false)
        }

        private suspend fun run(goal: String) {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                stopSpeaking("I don’t have a way to think yet.")
                return
            }
            if (!Reachability.online(app)) {
                stopSpeaking(OFFLINE)
                return
            }
            if (!BuddyHands.isReady() && !BuddyType.isReady()) {
                stopSpeaking(HANDS_OFF)
                return
            }
            voice.speak("On it.")
            val trail = ArrayList<String>(8)
            var stuck = 0
            try {
                for (step in 1..MAX_STEPS) {
                    if (!active) return
                    delay(SETTLE_MS)
                    val snap = GuidanceCatalog.forModel(awaitReadableSnapshot(), goal)
                    val catalog = GuidanceCatalog.format(snap)
                    val key = snap.sceneKey()
                    BuddyLog.d("Goal.step", "n=$step pkg=${snap.packageName} nodes=${snap.nodes.size} stuck=$stuck")
                    val plan = gemini.goalStep(goal, catalog, formatTrail(trail), step)
                    when {
                        !plan.done.isNullOrBlank() -> {
                            stopSpeaking(plan.say ?: doneLine(plan.done))
                            return
                        }
                        !plan.openApp.isNullOrBlank() -> {
                            val result = launcher.open(plan.openApp)
                            remember(trail, "open_app ${plan.openApp} → $result")
                            delay(APP_SETTLE_MS)
                            awaitReadableSnapshot()
                        }
                        else -> {
                            val result = GuidanceActor.run(plan, snap)
                            remember(trail, actionLine(plan, result))
                            delay(SETTLE_MS)
                            val after = awaitReadableSnapshot()
                            val afterKey = after.sceneKey()
                            stuck = if (afterKey == key) stuck + 1 else 0
                            if (result.endsWith("failed")) stuck += 1
                            if (stuck >= STUCK_LIMIT) {
                                stopSpeaking("I got stuck on this screen. Try telling me the next step.")
                                return
                            }
                        }
                    }
                }
                stopSpeaking("That’s as far as I could take it. Ask me again if you want me to keep going.")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                BuddyLog.e("Goal.fail", error.message ?: "unknown", error)
                stopSpeaking(if (Reachability.isNetworkFailure(error)) OFFLINE else THINK_FAIL)
            }
        }

        private fun stopSpeaking(message: String) {
            if (!active) return
            voice.speak(message)
            finish(speak = false)
        }

        private fun finish(speak: Boolean) {
            val was = active
            active = false
            job = null
            if (!was && !BrainSession.goalOpen.value) return
            BrainSession.setGoalOpen(false)
            if (BrainSession.phase.value == BrainPhase.Working) BrainSession.setPhase(BrainPhase.Idle)
            OverlayNotifier.sync(app)
            if (speak) { /* reserved — voice already handled */ }
        }

        fun release() {
            cancel()
            voice.cancelAll()
            scope.cancel()
        }
    }

    private fun remember(trail: ArrayList<String>, line: String) {
        trail += line
        if (trail.size > TRAIL_KEEP) trail.removeAt(0)
    }

    private fun formatTrail(trail: List<String>): String =
        if (trail.isEmpty()) "(none yet)" else trail.mapIndexed { i, line -> "${i + 1}. $line" }.joinToString("\n")

    private fun actionLine(plan: GuidancePlan, result: String): String = when {
        plan.type != null -> "type → $result"
        plan.hand != null -> "${plan.hand} → $result"
        !plan.place.isNullOrBlank() -> "fly_to ${plan.place} → $result"
        !plan.elementId.isNullOrBlank() -> "point_to ${plan.elementId} → $result"
        else -> result
    }

    private fun doneLine(status: String): String = when (status) {
        "succeeded" -> "That’s done."
        "stuck" -> "I got stuck. Try telling me the next step."
        else -> "I couldn’t finish that."
    }

    private const val MAX_STEPS = 20
    private const val STUCK_LIMIT = 3
    private const val TRAIL_KEEP = 6
    private const val SETTLE_MS = 420L
    private const val APP_SETTLE_MS = 700L
    private const val OFFLINE = "I can’t reach the internet just now. Check the connection and try again."
    private const val THINK_FAIL = "I couldn’t think that through just now. Try once more in a moment."
    private const val HANDS_OFF = "Turn on Buddy Assistant so I can tap and type for you."
}
