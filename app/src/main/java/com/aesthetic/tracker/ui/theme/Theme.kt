package com.aesthetic.tracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0B6B5D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F1E8),
    onPrimaryContainer = Color(0xFF06211C),
    secondary = Color(0xFF6E5668),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4DCEB),
    onSecondaryContainer = Color(0xFF281823),
    tertiary = Color(0xFF8A5A2B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDBF),
    onTertiaryContainer = Color(0xFF2E1600),
    background = Color(0xFFF7F8F3),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EEE7),
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A),
    onSurfaceVariant = Color(0xFF555D56),
    outline = Color(0xFF7A827A),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF77D8C2),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFF9DF4DD),
    secondary = Color(0xFFD9BED0),
    onSecondary = Color(0xFF3C2936),
    secondaryContainer = Color(0xFF554050),
    onSecondaryContainer = Color(0xFFF6DAEC),
    tertiary = Color(0xFFFFB978),
    onTertiary = Color(0xFF4B2800),
    tertiaryContainer = Color(0xFF6A3B08),
    onTertiaryContainer = Color(0xFFFFDDBF),
    background = Color(0xFF111612),
    surface = Color(0xFF181F1A),
    surfaceVariant = Color(0xFF29342E),
    onBackground = Color(0xFFE1E6E0),
    onSurface = Color(0xFFE1E6E0),
    onSurfaceVariant = Color(0xFFC2CBC2),
    outline = Color(0xFF8C958D),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AestheticTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        shapes = AppShapes,
        content = content,
    )
}
