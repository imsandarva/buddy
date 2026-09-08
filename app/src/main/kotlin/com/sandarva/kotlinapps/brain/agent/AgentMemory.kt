package com.sandarva.kotlinapps.brain.agent

/**
 * Short-term memory for one run. The model rewrites `progress` every turn (its own notes);
 * the app keeps the last few steps with what each one did to the screen, plus anything the person said.
 * Compact by design — the latest SCREEN is always the truth, memory only says how we got here.
 */
class AgentMemory(val goal: String) {
    var progress: String = ""
        private set
    private val steps = ArrayDeque<String>(KEEP + 1)
    private val said = ArrayList<String>(2)
    var stepCount = 0
        private set

    fun update(progress: String) { if (progress.isNotBlank()) this.progress = progress.take(PROGRESS_MAX) }

    fun record(action: AgentAction, outcome: Outcome, change: SceneDiff.Change?) {
        stepCount += 1
        val effect = when {
            !outcome.ok -> "FAILED: ${outcome.detail}"
            change != null -> "${outcome.detail} → ${change.summary}"
            else -> outcome.detail
        }
        steps += "$stepCount. ${action.describe()} → $effect"
        if (steps.size > KEEP) steps.removeFirst()
    }

    fun recordAnswer(text: String) {
        said += text.take(160)
        steps += "${stepCount}. asked them → they said: \"${text.take(120)}\""
        if (steps.size > KEEP) steps.removeFirst()
    }

    /** What the person told us mid-run — a Wi‑Fi name, a yes, a choice. */
    fun answers(): List<String> = said

    fun renderProgress(): String = progress.ifBlank { "(just started)" }

    fun renderSteps(): String = if (steps.isEmpty()) "(none yet)" else steps.joinToString("\n")

    companion object {
        const val PROGRESS_MAX = 220
        private const val KEEP = 8
    }
}
