// app/src/main/java/com/example/landguard/ui/theme/Color.kt

package com.example.landguard.ui.theme

import androidx.compose.ui.graphics.Color

// ╔══════════════════════════════════════════════════════════════╗
// ║   LandGuard Pro • Daylight Command — Light Design System    ║
// ║   Palette: Cloud White + Cyber Cyan + Eco Emerald           ║
// ╚══════════════════════════════════════════════════════════════╝

// ──────────────────────── App Layer Backgrounds ────────────────
val BgDeep         = Color(0xFFF0F5FC)   // Main app background — soft blue-white
val BgSurface      = Color(0xFFFFFFFF)   // Card surface — pure white
val BgElevated     = Color(0xFFF5F9FF)   // Elevated card — barely-blue tint
val BgHigh         = Color(0xFFEBF2FA)   // Dialogs / sheets
val BgBorder       = Color(0xFFC8D8EC)   // Default border stroke
val BgDivider      = Color(0xFFE4EDF8)   // Subtle list divider

// ─────────────────────── Primary — Cyber Cyan ──────────────────
// Darkened from original for 4.5:1+ contrast on white backgrounds
val CyanPrimary    = Color(0xFF007BA8)
val CyanLight      = Color(0xFF0099CC)
val CyanDim        = Color(0xFF005A7A)
val CyanContainer  = Color(0xFFDDEEFF)
val CyanGlow       = Color(0x3300A8DC)

// ─────────────────────── Secondary — Eco Emerald ───────────────
val EmeraldPrimary   = Color(0xFF008A5C)
val EmeraldLight     = Color(0xFF33B380)
val EmeraldDim       = Color(0xFF006040)
val EmeraldContainer = Color(0xFFDDFFF2)
val EmeraldGlow      = Color(0x3300B87E)

// ─────────────────────── Tertiary — Analytics Purple ───────────
val PurplePrimary   = Color(0xFF6040CC)
val PurpleLight     = Color(0xFF8870EE)
val PurpleContainer = Color(0xFFEEEAFF)
val PurpleGlow      = Color(0x336040CC)

// ───────────────────────── Risk Spectrum ───────────────────────
// All darkened to maintain WCAG AA contrast on white surfaces

val RiskCritical          = Color(0xFFC0184A)
val RiskCriticalContainer = Color(0xFFFFECF2)
val RiskCriticalGlow      = Color(0x40C0184A)

val RiskHigh              = Color(0xFFC04800)
val RiskHighContainer     = Color(0xFFFFF3EC)
val RiskHighGlow          = Color(0x40C04800)

val RiskModerate          = Color(0xFFA07000)
val RiskModerateContainer = Color(0xFFFFF8E0)
val RiskModerateGlow      = Color(0x40A07000)

val RiskLow               = Color(0xFF008A5C)
val RiskLowContainer      = Color(0xFFDDFFF2)
val RiskLowGlow           = Color(0x40008A5C)

// ─────────────────────── Satellite Brand Colors ────────────────
val Alos4Brand     = Color(0xFFC04800)   // JAXA ALOS-4 — dark orange
val SentinelBrand  = Color(0xFF0070A8)   // ESA Sentinel-2 — dark sky blue
val FusionBrand    = Color(0xFF6040CC)   // Hybrid fusion — purple

// ──────────────────────────── Text ─────────────────────────────
val TextWhite      = Color(0xFFFFFFFF)
val TextPrimary    = Color(0xFF0A1520)   // Near-black for maximum readability
val TextSecondary  = Color(0xFF3A5570)   // Dark blue-gray
val TextMuted      = Color(0xFF7A9AB8)   // Medium blue-gray

// ─────────────────────── Utility / Overlay ─────────────────────
val OverlayDark    = Color(0x88000000)
val OverlayLight   = Color(0x33FFFFFF)
val GlassBg        = Color(0xF0FFFFFF)