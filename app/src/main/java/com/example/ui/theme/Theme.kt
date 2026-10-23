package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001F24),
    primaryContainer = Color(0xFF004D56),
    onPrimaryContainer = Color(0xFF9CF0FF),

    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFE9D5FF),

    tertiary = CoralRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF881337),
    onTertiaryContainer = Color(0xFFFFD1DC),

    background = DeepNavy,
    onBackground = TextWhite,

    surface = SurfaceNavy,
    onSurface = TextWhite,
    surfaceVariant = SurfaceNavyCard,
    onSurfaceVariant = TextMuted,

    outline = BorderNavy,
    outlineVariant = Color(0xFF1E283E),

    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We enforce the sleek premium dark navy theme requested by the user
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
