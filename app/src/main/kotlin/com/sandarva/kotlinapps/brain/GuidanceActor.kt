package com.sandarva.kotlinapps.brain

import com.sandarva.kotlinapps.accessibility.BuddyHands
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.accessibility.BuddyType
import com.sandarva.kotlinapps.accessibility.FieldTarget
import com.sandarva.kotlinapps.accessibility.HandReach
import com.sandarva.kotlinapps.accessibility.ScreenSnapshot
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import com.sandarva.kotlinapps.overlay.CursorLanding

/**
 * Executes a [GuidancePlan] on eyes, hands, and type. Chat REST and Live both call this.
 * Does not speak — Live already has a voice; REST TTS stays in the brain.
 */
object GuidanceActor {
    suspend fun run(plan: GuidancePlan, snapshot: ScreenSnapshot): String {
        if ((plan.hand != null || plan.type != null) && !BuddyHands.isReady() && !BuddyType.isReady()) {
            return "Buddy Assistant is off — I cannot tap or type."
        }
        val result = when {
            plan.type != null && !BuddyType.isReady() -> "Buddy Assistant is off — I cannot type."
            plan.type != null -> type(plan.type, snapshot)
            plan.hand != null && !BuddyHands.isReady() -> "Buddy Assistant is off — I cannot tap."
            else -> when (val hand = plan.hand) {
                is HandPlan.Tap -> tap(hand.elementId, snapshot)
                is HandPlan.Hold -> hold(hand.elementId, snapshot)
                is HandPlan.Stroke -> stroke(hand, snapshot)
                null -> when {
                    !plan.place.isNullOrBlank() -> fly(plan.place)
                    !plan.elementId.isNullOrBlank() -> point(plan.elementId, snapshot)
                    else -> "ok"
                }
            }
        }
        BuddyLog.d("Actor.run", "result=$result place=${plan.place} elementId=${plan.elementId} hand=${plan.hand} type=${plan.type}")
        return result
    }

    private suspend fun type(plan: TypePlan, snapshot: ScreenSnapshot): String {
        if (plan.text.isBlank() && !plan.submit) return "type failed"
        val node = plan.elementId?.let { snapshot.node(it) }
        val target = FieldTarget(plan.elementId, node?.viewId, node?.bounds)
        val ok = if (plan.text.isBlank()) BuddyType.submitHere() else BuddyType.typeAt(plan.text, target, plan.submit)
        return if (ok) "typed ${plan.elementId ?: "here"}" else "type failed"
    }

    private suspend fun tap(id: String?, snapshot: ScreenSnapshot): String {
        val node = id?.let { snapshot.node(it) }
        val ok = if (node != null) BuddyHands.tapAt(node.bounds.centerX, node.bounds.centerY) else BuddyHands.tapHere()
        return if (ok) "tapped ${id ?: "here"}" else "tap failed"
    }

    private suspend fun hold(id: String?, snapshot: ScreenSnapshot): String {
        val node = id?.let { snapshot.node(it) }
        val ok = if (node != null) BuddyHands.holdAt(node.bounds.centerX, node.bounds.centerY) else BuddyHands.holdHere()
        return if (ok) "held ${id ?: "here"}" else "hold failed"
    }

    private suspend fun stroke(hand: HandPlan.Stroke, snapshot: ScreenSnapshot): String {
        val from = hand.fromId?.let { snapshot.node(it)?.bounds }?.let { it.centerX to it.centerY }
        val to = destination(hand, snapshot)
        val ok = when {
            from != null && to != null && hand.holdFirst -> BuddyHands.dragFromTo(from.first, from.second, to.first, to.second)
            from != null && to != null -> BuddyHands.swipeFromTo(from.first, from.second, to.first, to.second)
            to != null && hand.holdFirst -> BuddyHands.dragTo(to.first, to.second)
            to != null -> BuddyHands.swipeTo(to.first, to.second)
            hand.direction != null -> {
                val (dx, dy) = directionDelta(hand.direction)
                if (hand.holdFirst) BuddyHands.dragHere(dx, dy) else BuddyHands.swipeHere(dx, dy)
            }
            else -> false
        }
        return if (ok) "stroke ok" else "stroke failed"
    }

    private fun fly(place: String): String {
        val xy = CursorLanding.normalized(place)
        val ok = xy != null && BuddyCursorController.animateToNormalized(xy.first, xy.second)
        return if (ok) "flew to $place" else "fly failed"
    }

    private fun point(id: String, snapshot: ScreenSnapshot): String {
        val node = snapshot.node(id)
        val ok = node != null && BuddyScreenEyes.pointTo(node)
        return if (ok) "pointed at $id" else "point failed"
    }

    private fun destination(hand: HandPlan.Stroke, snapshot: ScreenSnapshot): Pair<Float, Float>? {
        hand.toId?.let { id -> snapshot.node(id)?.bounds?.let { return it.centerX to it.centerY } }
        hand.toPlace?.let { place ->
            val xy = CursorLanding.normalized(place) ?: return@let
            val screen = BuddyCursorController.screenPixels() ?: return@let
            return screen.first * xy.first to screen.second * xy.second
        }
        return null
    }

    private fun directionDelta(direction: String): Pair<Float, Float> = when (direction.lowercase()) {
        "left" -> -STEP to 0f
        "right" -> STEP to 0f
        "up" -> 0f to -STEP
        "down" -> 0f to STEP
        else -> 0f to 0f
    }

    private const val STEP = HandReach.DRAG
}
