package com.example.landguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LandGuardLightColors = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = Color.White,
    primaryContainer = SoftMint,
    onPrimaryContainer = ForestDark,
    secondary = SatelliteSky,
    onSecondary = Color.White,
    secondaryContainer = SatelliteSkyContainer,
    onSecondaryContainer = Color(0xFF0369A1),
    background = LightBackground,
    onBackground = TextCharcoal,
    surface = CardSurface,
    onSurface = TextCharcoal,
    surfaceVariant = SurfaceCream,
    onSurfaceVariant = TextMuted,
    outline = BorderSubtle,
    error = RiskCriticalRed
)

@Composable
fun LandGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LandGuardLightColors,
        typography = Typography,
        content = content
    )
}
