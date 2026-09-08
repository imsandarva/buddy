package com.sandarva.kotlinapps.ui.cursor

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.sandarva.kotlinapps.overlay.OverlaySession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Touch target wraps the full cursor bounds; transforms pivot on the tip. */
@Composable
fun BuddyCursorHandle(
    onDrag: (Float, Float) -> Unit,
    onRelease: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onGrab: () -> Unit = {},
    onDoubleTap: () -> Unit = {}
) {
    var phase by remember { mutableStateOf(CursorPhase.Idle) }
    var velocity by remember { mutableStateOf(Offset.Zero) }
    val pressing by OverlaySession.pressing.collectAsState()
    val thinking by OverlaySession.thinking.collectAsState()
    val effectivePhase = when {
        pressing -> CursorPhase.Lifted
        phase == CursorPhase.Drifting -> CursorPhase.Drifting
        thinking && phase == CursorPhase.Idle -> CursorPhase.Thinking
        else -> phase
    }
    val motion = rememberCursorMotion(effectivePhase, velocity)
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .size(CursorGeometry.touchWidth, CursorGeometry.touchHeight)
            .graphicsLayer {
                scaleX = motion.scale
                scaleY = motion.scale
                rotationZ = motion.rotation
                translationY = motion.liftY
                transformOrigin = TransformOrigin(CursorGeometry.tipFractionX(), CursorGeometry.tipFractionY())
                clip = false
            }
            .pointerInput(Unit) {
                cursorGestures(
                    onTap = {
                        phase = CursorPhase.Tap
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        scope.launch { delay(520); if (phase == CursorPhase.Tap) phase = CursorPhase.Idle }
                    },
                    onSummon = {
                        phase = CursorPhase.Summon
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onDoubleTap()
                        scope.launch { delay(900); if (phase == CursorPhase.Summon) phase = CursorPhase.Idle }
                    },
                    onLift = {
                        if (phase != CursorPhase.Drifting) phase = CursorPhase.Lifted
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    onDragStart = {
                        onGrab()
                        phase = CursorPhase.Drifting
                    },
                    onDrag = { dx, dy ->
                        velocity = Offset(dx, dy) * 0.4f + velocity * 0.6f
                        onDrag(dx, dy)
                    },
                    onRelease = {
                        phase = CursorPhase.Land
                        velocity = Offset.Zero
                        onRelease()
                        scope.launch { delay(380); if (phase == CursorPhase.Land) phase = CursorPhase.Idle }
                    }
                )
            }
    ) {
        BuddyCursor(motion = motion, contentDescription = label)
    }
}
