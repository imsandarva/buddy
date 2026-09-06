package com.sandarva.kotlinapps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BuddyScheme = darkColorScheme(
    background = BuddyColors.Ink,
    surface = BuddyColors.Ink,
    primary = BuddyColors.Honey,
    onPrimary = BuddyColors.OnHoney,
    secondary = BuddyColors.Sage,
    onSecondary = BuddyColors.Ink,
    onBackground = BuddyColors.Bone,
    onSurface = BuddyColors.Bone,
    onSurfaceVariant = BuddyColors.Mist
)

@Composable
fun BuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BuddyScheme, typography = BuddyTypography, content = content)
}
