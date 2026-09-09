package com.example.landguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LandGuardDarkColors = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    primaryContainer = CyberBlue,
    onPrimaryContainer = Color.White,
    secondary = CyberGreen,
    background = CoreBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = CyberRed
)

@Composable
fun LandGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LandGuardDarkColors,
        typography = Typography,
        content = content
    )
}
