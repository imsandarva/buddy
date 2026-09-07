package com.sandarva.kotlinapps.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val Trimmed = PlatformTextStyle(includeFontPadding = false)
private val TightLines = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)

val BuddyTypography = Typography(
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 3.2.sp,
        platformStyle = Trimmed,
        lineHeightStyle = TightLines
    ),
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.9).sp,
        platformStyle = Trimmed,
        lineHeightStyle = TightLines
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.35).sp,
        platformStyle = Trimmed,
        lineHeightStyle = TightLines
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.25).sp,
        platformStyle = Trimmed,
        lineHeightStyle = TightLines
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.1.sp,
        platformStyle = Trimmed,
        lineHeightStyle = TightLines
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.08.sp,
        platformStyle = Trimmed,
        lineHeightStyle = TightLines
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.15.sp,
        platformStyle = Trimmed,
        lineHeightStyle = TightLines
    )
)
