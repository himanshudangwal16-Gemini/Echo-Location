package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AcousticNeonGreen,
    secondary = TactileSonarAmber,
    tertiary = SonicLightCyan,
    background = DeepAcousticBlack,
    surface = CharcoalSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextPrimaryWhite,
    onSurface = TextPrimaryWhite,
    surfaceVariant = SecondaryCharcoal,
    onSurfaceVariant = TextSecondaryGray,
    outline = AcousticNeonGreen
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for specialized high-fidelity radar styling
    dynamicColor: Boolean = false, // Disable dynamic colors to protect high-contrast accessibility color tuning
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
