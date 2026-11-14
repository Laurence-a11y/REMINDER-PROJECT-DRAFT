package com.rence.reminders.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Monochrome with a single red accent, in the spirit of Nothing OS.
private val Red = Color(0xFFD71921)

private val DarkColors = darkColorScheme(
    primary = Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A0A0C),
    onPrimaryContainer = Color(0xFFFFDAD7),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color(0xFFEDEDED),
    surface = Color.Black,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFA8A8A8),
    surfaceContainerLowest = Color(0xFF050505),
    surfaceContainerLow = Color(0xFF0E0E0E),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF242424),
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF2E2E2E),
    error = Color(0xFFFF6B6B),
)

private val LightColors = lightColorScheme(
    primary = Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD7),
    onPrimaryContainer = Color(0xFF410003),
    secondary = Color(0xFF424242),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E6E6),
    onSecondaryContainer = Color.Black,
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFF7F7F7),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE9E9E9),
    onSurfaceVariant = Color(0xFF5E5E5E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFBFB),
    surfaceContainer = Color(0xFFF1F1F1),
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE4E4E4),
    outline = Color(0xFF9E9E9E),
    outlineVariant = Color(0xFFDDDDDD),
    error = Color(0xFFB3261E),
)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
