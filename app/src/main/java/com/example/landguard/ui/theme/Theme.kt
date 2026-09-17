package com.example.landguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LandGuardLightColors = lightColorScheme(

    // ─────────────────────────────────────────────
    // Primary
    // ─────────────────────────────────────────────

    primary = BrandPrimary,
    onPrimary = Color.White,

    primaryContainer = BrandContainer,
    onPrimaryContainer = BrandPrimaryDark,

    // ─────────────────────────────────────────────
    // Secondary
    // ─────────────────────────────────────────────

    secondary = BrandSecondary,
    onSecondary = Color.White,

    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldDim,

    // ─────────────────────────────────────────────
    // Tertiary
    // ─────────────────────────────────────────────

    tertiary = PurplePrimary,
    onTertiary = Color.White,

    tertiaryContainer = PurpleContainer,
    onTertiaryContainer = Color(0xFF34286F),

    // ─────────────────────────────────────────────
    // Background / Surface
    // ─────────────────────────────────────────────

    background = BgDeep,
    onBackground = TextPrimary,

    surface = BgSurface,
    onSurface = TextPrimary,

    surfaceVariant = BgElevated,
    onSurfaceVariant = TextSecondary,

    surfaceContainerLowest = BgSurface,
    surfaceContainerLow = BgSurface,
    surfaceContainer = BgSurface,
    surfaceContainerHigh = BgSurface,
    surfaceContainerHighest = BgElevated,

    // ─────────────────────────────────────────────
    // Borders
    // ─────────────────────────────────────────────

    outline = BgBorder,
    outlineVariant = BgDivider,

    // ─────────────────────────────────────────────
    // Error
    // ─────────────────────────────────────────────

    error = RiskCritical,
    onError = Color.White,

    errorContainer = RiskCriticalContainer,
    onErrorContainer = Color(0xFF991B1B),

    // ─────────────────────────────────────────────
    // Inverse
    // ─────────────────────────────────────────────

    inverseSurface = Color(0xFF102219),
    inverseOnSurface = Color.White,
    inversePrimary = BrandPrimaryLight,

    surfaceTint = BrandPrimary,

    scrim = Color(0x66000000)
)

@Composable
fun LandGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LandGuardLightColors,
        typography = Typography,
        content = content
    )
}
