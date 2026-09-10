// app/src/main/java/com/example/landguard/ui/theme/Theme.kt

package com.example.landguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LandGuardLightColors = lightColorScheme(
    primary               = CyanPrimary,
    onPrimary             = Color.White,
    primaryContainer      = CyanContainer,
    onPrimaryContainer    = CyanDim,
    secondary             = EmeraldPrimary,
    onSecondary           = Color.White,
    secondaryContainer    = EmeraldContainer,
    onSecondaryContainer  = EmeraldDim,
    tertiary              = PurplePrimary,
    onTertiary            = Color.White,
    tertiaryContainer     = PurpleContainer,
    onTertiaryContainer   = Color(0xFF301890),
    background            = BgDeep,
    onBackground          = TextPrimary,
    surface               = BgSurface,
    onSurface             = TextPrimary,
    surfaceVariant        = BgElevated,
    onSurfaceVariant      = TextSecondary,
    outline               = BgBorder,
    outlineVariant        = BgDivider,
    error                 = RiskCritical,
    onError               = Color.White,
    errorContainer        = RiskCriticalContainer,
    onErrorContainer      = Color(0xFF800028),
    inverseSurface        = Color(0xFF0A1520),
    inverseOnSurface      = Color.White,
    inversePrimary        = CyanLight,
    surfaceTint           = CyanPrimary,
    scrim                 = Color(0x88000000),
)

@Composable
fun LandGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LandGuardLightColors,
        typography  = Typography,
        content     = content,
    )
}