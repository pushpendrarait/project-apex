package com.example.projectapex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TurfEmerald,
    onPrimary = BgMidnight,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = TurfEmerald,
    secondary = BmsCrimson,
    onSecondary = TextWhite,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = BmsCrimson,
    tertiary = AccentElectricBlue,
    background = BgMidnight,
    onBackground = TextWhite,
    surface = SurfaceDark,
    onSurface = TextWhite,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = ErrorRed,
    onError = TextWhite
)

@Composable
fun ProjectApexTheme(
    darkTheme: Boolean = true, // Default to BookMyShow dark mode
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
