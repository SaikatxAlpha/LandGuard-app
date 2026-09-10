// app/src/main/java/com/example/landguard/ui/theme/Type.kt

package com.example.landguard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 32.sp,
        lineHeight   = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 28.sp,
        lineHeight   = 36.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 24.sp,
        lineHeight   = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 22.sp,
        lineHeight   = 30.sp,
        letterSpacing = 0.1.sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 18.sp,
        lineHeight   = 26.sp,
        letterSpacing = 0.1.sp
    ),
    headlineSmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 16.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.15.sp
    ),
    titleLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 15.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),
    titleMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 10.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 10.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 9.sp,
        lineHeight   = 12.sp,
        letterSpacing = 1.0.sp
    )
)