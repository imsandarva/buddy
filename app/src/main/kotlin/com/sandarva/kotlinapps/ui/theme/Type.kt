package com.sandarva.kotlinapps.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val Trimmed = PlatformTextStyle(includeFontPadding = false)
private val LineRhythm = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)

/**
 * Multi-word styles must leave [letterSpacing] unspecified. OEM generic fonts (Samsung One UI
 * especially) collapse the space glyph once Compose writes a tracking value onto the paint —
 * even a tiny `0.08.sp` — which is why "Allow it" rendered as "Allowit". Eyebrows stay tracked
 * because they are single uppercase tokens.
 */
private fun buddyStyle(
    family: FontFamily,
    weight: FontWeight,
    size: TextUnit,
    line: TextUnit,
    tracking: TextUnit = TextUnit.Unspecified
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size,
    lineHeight = line,
    letterSpacing = tracking,
    platformStyle = Trimmed,
    lineHeightStyle = LineRhythm
)

val BuddyTypography = Typography(
    labelSmall = buddyStyle(FontFamily.SansSerif, FontWeight.Medium, 11.sp, 14.sp, tracking = 2.8.sp),
    displayLarge = buddyStyle(FontFamily.Serif, FontWeight.Light, 40.sp, 48.sp),
    titleLarge = buddyStyle(FontFamily.Serif, FontWeight.Normal, 26.sp, 34.sp),
    titleMedium = buddyStyle(FontFamily.Serif, FontWeight.Normal, 17.sp, 24.sp),
    bodyLarge = buddyStyle(FontFamily.SansSerif, FontWeight.Normal, 17.sp, 26.sp),
    labelLarge = buddyStyle(FontFamily.SansSerif, FontWeight.Medium, 16.sp, 22.sp),
    bodySmall = buddyStyle(FontFamily.SansSerif, FontWeight.Normal, 13.sp, 20.sp)
)
