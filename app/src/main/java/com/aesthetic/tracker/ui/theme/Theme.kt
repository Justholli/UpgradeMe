package com.aesthetic.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF7CFFCB),
    onPrimary = Color(0xFF003827),
    secondary = Color(0xFF8FB8FF),
    tertiary = Color(0xFFFFC46B),
    background = Color(0xFF080B12),
    surface = Color(0xFF101827),
    surfaceVariant = Color(0xFF1A2333),
    onBackground = Color(0xFFE7EDF7),
    onSurface = Color(0xFFE7EDF7),
    onSurfaceVariant = Color(0xFFB5C2D6),
    error = Color(0xFFFF7A90),
)

@Composable
fun AestheticTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
