package com.sandarva.kotlinapps.ui.theme

import androidx.compose.ui.graphics.Color

/** Paper field, iris accent, ink for words — light only. */
object BuddyColors {
    val Paper = Color(0xFFF7F6FB)
    val Snow = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1C1730)
    val InkMuted = Color(0xFF6B6578)
    val Mist = Color(0xFF9A94A8)
    val Line = Color(0xFFE6E2F0)
    val Violet = Color(0xFF635BFF)
    val VioletDeep = Color(0xFF4E46E5)
    val OnViolet = Color(0xFFFFFFFF)
    val Lilac = Color(0xFF8B83F4)
    val Glow = Color(0x3D635BFF)
    val GlowSoft = Color(0x18635BFF)
    val GlowLilac = Color(0x248B83F4)
    val Scrim = Color(0x3D1C1730)

    // BuddyCursor — one glassy blue-violet material, denser/focused in Action form,
    // ambient/diffuse in Voice form. See docs/cursor.md.
    val CursorCore = Color(0xFF4740D6) // deep periwinkle-blue, lit from within
    val CursorMid = Color(0xFF6C63F2)
    val CursorRim = Color(0xFFC9C3FB) // lavender rim the glow fades into
    val CursorFocus = Color(0xFF5850FF) // brighter, denser core for the Action point
    val CursorHighlight = Color(0xFFEFECFF) // inner sheen catching light like glass
    val CursorGlow = Color(0x445850FF) // scarce resource — only bloom for real signal
    val CursorGlowFaint = Color(0x1A5850FF)
    val CursorShadow = Color(0x332A1F55) // neutral contact shadow, legible on any host app
    val CursorLightRim = Color(0x99FFFFFF) // thin outer rim so the shape reads on dark hosts too

    // Drag-to-dismiss target — ink glass at rest, muted rose when the cursor is over it.
    val DismissFill = Color(0xE81C1730)
    val DismissFillArmed = Color(0xF2C45D6A)
    val DismissCross = Color(0xFFF7F6FB)
    val DismissGlow = Color(0x331C1730)
    val DismissGlowArmed = Color(0x66C45D6A)
    val DismissRim = Color(0x73FFFFFF)
}
