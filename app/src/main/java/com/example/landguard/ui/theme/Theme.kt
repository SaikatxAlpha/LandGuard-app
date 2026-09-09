package com.example.landguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LandGuardLightColors = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = ForestDark,
    secondary = SatellitePrimary,
    onSecondary = Color.White,
    secondaryContainer = SatelliteContainer,
    onSecondaryContainer = Color(0xFF0369A1),
    background = LightBackground,
    onBackground = PrimaryText,
    surface = Surface,
    onSurface = PrimaryText,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = SecondaryText,
    outline = BorderLight,
    error = RiskCritical
)

@Composable
fun LandGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LandGuardLightColors,
        typography = Typography,
        content = content
    )
}