package com.sandarva.kotlinapps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BuddyScheme = lightColorScheme(
    background = BuddyColors.Paper,
    surface = BuddyColors.Snow,
    primary = BuddyColors.Violet,
    onPrimary = BuddyColors.OnViolet,
    secondary = BuddyColors.Lilac,
    onSecondary = BuddyColors.OnViolet,
    onBackground = BuddyColors.Ink,
    onSurface = BuddyColors.Ink,
    onSurfaceVariant = BuddyColors.Mist,
    outline = BuddyColors.Line
)

@Composable
fun BuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BuddyScheme, typography = BuddyTypography, content = content)
}
