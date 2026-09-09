package com.sandarva.kotlinapps.ui.cursor

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.overlay.DismissZone
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.CursorMotion

/** Compact circular X at the bottom — chat-head dismiss scale while repositioning Buddy. */
@Composable
fun DismissTarget(
    visible: Boolean,
    armed: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val presence by animateFloatAsState(if (visible) 1f else 0f, if (visible) CursorMotion.dismissEnter() else CursorMotion.dismissExit(), label = "dismissPresence")
    val arm by animateFloatAsState(if (armed) 1f else 0f, CursorMotion.DismissArm, label = "dismissArm")
    LaunchedEffect(armed) { if (armed) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) }
    val box = (DismissZone.ARMED_DP + DismissZone.GLOW_PAD_DP).dp
    Canvas(
        modifier
            .size(box)
            .semantics { this.contentDescription = contentDescription }
            .graphicsLayer {
                val shown = presence
                alpha = shown
                translationY = (1f - shown) * 14.dp.toPx()
                val enter = 0.88f + 0.12f * shown
                scaleX = enter
                scaleY = enter
            }
    ) {
        if (presence < 0.01f) return@Canvas
        val restR = (DismissZone.REST_DP / 2f).dp.toPx()
        val armedR = (DismissZone.ARMED_DP / 2f).dp.toPx()
        val radius = restR + (armedR - restR) * arm
        val fill = lerp(BuddyColors.DismissFill, BuddyColors.DismissFillArmed, arm)
        val glow = lerp(BuddyColors.DismissGlow, BuddyColors.DismissGlowArmed, arm)
        drawCircle(
            brush = Brush.radialGradient(0f to glow, 1f to Color.Transparent, radius = radius * 1.55f),
            radius = radius * 1.55f
        )
        drawCircle(
            brush = Brush.radialGradient(0f to fill.copy(alpha = fill.alpha * 0.92f), 0.78f to fill, 1f to fill.copy(alpha = fill.alpha * 0.88f), radius = radius),
            radius = radius
        )
        drawCircle(color = BuddyColors.DismissRim.copy(alpha = 0.35f + 0.25f * arm), radius = radius, style = Stroke(width = radius * 0.028f))
        val inset = radius * 0.38f
        val stroke = radius * 0.11f
        val cross = BuddyColors.DismissCross.copy(alpha = 0.92f + 0.08f * arm)
        drawLine(cross, Offset(center.x - inset, center.y - inset), Offset(center.x + inset, center.y + inset), stroke, StrokeCap.Round)
        drawLine(cross, Offset(center.x + inset, center.y - inset), Offset(center.x - inset, center.y + inset), stroke, StrokeCap.Round)
    }
}
