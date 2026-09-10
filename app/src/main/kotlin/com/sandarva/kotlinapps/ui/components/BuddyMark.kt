package com.sandarva.kotlinapps.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.cursor.CursorMaterial
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/**
 * In-app brand mark — the same held/drag BuddyCursor body as the launcher icon, without the white
 * plate. A quiet breathe is the only extra life; this is never Voice/live form. See docs/brand.md.
 */
@Composable
fun BuddyMark(alive: Boolean = false, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "mark")
    val lo = if (alive) 0.98f else 0.96f
    val hi = if (alive) 1.05f else 1.02f
    val breathe by infinite.animateFloat(lo, hi, infiniteRepeatable(BuddyMotion.breathe(), RepeatMode.Reverse), label = "breathe")
    Canvas(modifier.size(76.dp).graphicsLayer { scaleX = breathe; scaleY = breathe }) {
        val radiusPx = size.minDimension * ORB_FRACTION
        with(CursorMaterial) {
            drawGlow(radiusPx, CursorMaterial.HELD_GLOW, CursorMaterial.LOGO_GLOW_SPREAD)
            drawContactShadow(radiusPx)
            drawSharedMaterial(radiusPx, CursorMaterial.ACTION_MORPH)
            drawLogoSheen(radiusPx)
        }
    }
}

/** Orb radius as a fraction of the canvas — sized so glow still fits, close to the launcher glyph. */
private const val ORB_FRACTION = 0.29f
