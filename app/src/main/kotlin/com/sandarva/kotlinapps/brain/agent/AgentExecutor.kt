package com.sandarva.kotlinapps.brain.agent

import com.sandarva.kotlinapps.accessibility.BuddyGlobal
import com.sandarva.kotlinapps.accessibility.BuddyHands
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.accessibility.BuddyType
import com.sandarva.kotlinapps.accessibility.FieldTarget
import com.sandarva.kotlinapps.accessibility.ScreenNode
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import com.sandarva.kotlinapps.overlay.CursorLanding
import kotlinx.coroutines.delay

/** What an action did, in words the model (and the log) can use. */
data class Outcome(val ok: Boolean, val detail: String) {
    companion object {
        fun ok(detail: String) = Outcome(true, detail)
        fun fail(detail: String) = Outcome(false, detail)
    }
}

/**
 * The body. Resolves ids against the snapshot the model was looking at and drives eyes, hands,
 * type, and the phone's keys. Shared by the agent loop and Live's one-shot tools. Never speaks.
 */
class AgentExecutor(private val launcher: AppLauncher) {

    suspend fun perform(action: AgentAction, snapshot: ScreenSnapshot): Outcome {
        val outcome = when (action) {
            is AgentAction.Tap -> action.target?.let { id -> withNode(id, snapshot) { node -> hands { BuddyHands.tapAt(node.bounds.centerX, node.bounds.centerY) }.named("tapped", node) } }
                ?: hands { BuddyHands.tapHere() }.words("tapped here")
            is AgentAction.LongPress -> action.target?.let { id -> withNode(id, snapshot) { node -> hands { BuddyHands.holdAt(node.bounds.centerX, node.bounds.centerY) }.named("held", node) } }
                ?: hands { BuddyHands.holdHere() }.words("held here")
            is AgentAction.Type -> type(action, snapshot)
            is AgentAction.Scroll -> scroll(action, snapshot)
            is AgentAction.Swipe -> hands { BuddyHands.swipePage(action.direction) }.words("swiped ${action.direction.word}")
            is AgentAction.Drag -> drag(action, snapshot)
            is AgentAction.Press -> if (!BuddyGlobal.isReady()) ASSISTANT_OFF else if (BuddyGlobal.press(action.key)) Outcome.ok("pressed ${action.key.word}") else Outcome.fail("could not press ${action.key.word}")
            is AgentAction.OpenApp -> launcher.open(action.name)
            is AgentAction.Point -> withNode(action.target, snapshot) { node -> if (BuddyScreenEyes.pointTo(node)) Outcome.ok("pointed at \"${node.label.take(40)}\"") else Outcome.fail("the cursor is not on screen") }
            is AgentAction.MoveCursor -> moveCursor(action.place)
            is AgentAction.NudgeCursor -> nudgeCursor(action.dx, action.dy)
            AgentAction.Wait -> { delay(WAIT_MS); Outcome.ok("waited a moment") }
            is AgentAction.Ask, AgentAction.None -> Outcome.ok("")
        }
        BuddyLog.d("Agent.act", "${action.describe()} → ok=${outcome.ok} ${outcome.detail}")
        return outcome
    }

    private suspend fun type(action: AgentAction.Type, snapshot: ScreenSnapshot): Outcome {
        if (!BuddyType.isReady()) return ASSISTANT_OFF
        val node = action.target?.let { snapshot.node(it) }
        if (action.target != null && node == null) return missing(action.target)
        if (action.text.isBlank()) return if (BuddyType.submitHere()) Outcome.ok("pressed enter") else Outcome.fail("no field to submit")
        val ok = BuddyType.typeAt(action.text, FieldTarget(node?.id, node?.viewId, node?.bounds), action.submit)
        val where = node?.let { " into \"${it.label.take(32)}\"" } ?: ""
        return if (ok) Outcome.ok("typed \"${action.text.take(32)}\"$where${if (action.submit) " and submitted" else ""}") else Outcome.fail("could not type$where — is a text field on screen?")
    }

    private suspend fun scroll(action: AgentAction.Scroll, snapshot: ScreenSnapshot): Outcome {
        if (!BuddyHands.isReady()) return ASSISTANT_OFF
        val list = action.target?.let { snapshot.node(it) } ?: mainList(snapshot)
        if (action.target != null && list == null) return missing(action.target)
        val ok = BuddyHands.scrollWithin(list?.bounds, action.direction)
        return if (ok) Outcome.ok("scrolled ${action.direction.word}${list?.let { " in \"${it.label.take(24)}\"" } ?: ""}") else Outcome.fail("scroll did not go through")
    }

    private suspend fun drag(action: AgentAction.Drag, snapshot: ScreenSnapshot): Outcome {
        val from = snapshot.node(action.from) ?: return missing(action.from)
        val to = snapshot.node(action.to) ?: return missing(action.to)
        return hands { BuddyHands.dragFromTo(from.bounds.centerX, from.bounds.centerY, to.bounds.centerX, to.bounds.centerY) }
            .words("dragged \"${from.label.take(24)}\" to \"${to.label.take(24)}\"")
    }

    private fun moveCursor(place: String): Outcome {
        val xy = CursorLanding.normalized(place) ?: return Outcome.fail("unknown place $place")
        return if (BuddyCursorController.animateToNormalized(xy.first, xy.second)) Outcome.ok("moved to $place") else Outcome.fail("the cursor is not on screen")
    }

    private fun nudgeCursor(dx: Float, dy: Float): Outcome =
        if (BuddyCursorController.nudgeNormalized(dx, dy)) Outcome.ok("moved the buddy") else Outcome.fail("the cursor is not on screen")

    /** The tallest scrollable on screen is the page itself when the model does not name a list. */
    private fun mainList(snapshot: ScreenSnapshot): ScreenNode? = snapshot.scrollables().maxByOrNull { it.bounds.width.toLong() * it.bounds.height }

    private suspend inline fun withNode(id: String, snapshot: ScreenSnapshot, block: (ScreenNode) -> Outcome): Outcome =
        snapshot.node(id)?.let(block) ?: missing(id)

    private suspend inline fun hands(block: suspend () -> Boolean): Boolean? = if (BuddyHands.isReady()) block() else null

    private fun Boolean?.named(verb: String, node: ScreenNode): Outcome = when (this) {
        null -> ASSISTANT_OFF
        true -> Outcome.ok("$verb \"${node.label.take(40)}\"")
        false -> Outcome.fail("$verb nothing — the press did not go through")
    }

    private fun Boolean?.words(done: String): Outcome = when (this) {
        null -> ASSISTANT_OFF
        true -> Outcome.ok(done)
        false -> Outcome.fail("the stroke did not go through")
    }

    private fun missing(id: String) = Outcome.fail("no control with id [$id] on this screen — use an id from SCREEN NOW")

    private companion object {
        val ASSISTANT_OFF = Outcome.fail("Buddy Assistant is off, so I cannot touch the screen")
        const val WAIT_MS = 1200L
    }
}
