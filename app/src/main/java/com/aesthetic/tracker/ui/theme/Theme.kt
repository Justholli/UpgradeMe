package com.aesthetic.tracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF006B58),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F2DF),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4B5F78),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E4FF),
    tertiary = Color(0xFF7A5A00),
    background = Color(0xFFF8FAF7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7EEE8),
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A),
    onSurfaceVariant = Color(0xFF424941),
    outline = Color(0xFF737970),
    error = Color(0xFFBA1A1A),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AestheticTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        shapes = AppShapes,
        content = content,
    )
}
