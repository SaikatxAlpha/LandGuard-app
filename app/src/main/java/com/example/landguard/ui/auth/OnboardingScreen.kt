// app/src/main/java/com/example/landguard/ui/auth/OnboardingScreen.kt

package com.example.landguard.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.landguard.ui.theme.Alos4Brand
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.CyanContainer
import com.example.landguard.ui.theme.CyanGlow
import com.example.landguard.ui.theme.CyanPrimary
import com.example.landguard.ui.theme.EmeraldContainer
import com.example.landguard.ui.theme.EmeraldPrimary
import com.example.landguard.ui.theme.PurpleContainer
import com.example.landguard.ui.theme.PurplePrimary
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.RiskCriticalContainer
import com.example.landguard.ui.theme.SentinelBrand
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    onVerified: () -> Unit,
    @Suppress("UNUSED_PARAMETER")
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var phoneNumber  by remember { mutableStateOf("") }
    var otp          by remember { mutableStateOf("") }
    var otpRequested by remember { mutableStateOf(false) }
    var isVerified   by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "ob_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ob_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Background ambient glow
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyanPrimary.copy(alpha = glowAlpha * 0.3f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // ── Logo ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(CyanContainer, EmeraldContainer))
                    )
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(CyanPrimary.copy(alpha = 0.7f), EmeraldPrimary.copy(alpha = 0.4f))),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Landscape, null, tint = CyanPrimary, modifier = Modifier.size(42.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "LandGuard Pro",
                color         = TextPrimary,
                fontSize      = 32.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "SATELLITE RISK INTELLIGENCE PLATFORM",
                color         = CyanPrimary,
                fontSize      = 9.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                textAlign     = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Real-time land risk monitoring powered by JAXA ALOS-4 & ESA Sentinel-2 satellite data",
                color      = TextSecondary,
                fontSize   = 12.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(32.dp))

            // ── Feature Highlights ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureChip(Icons.Filled.Radar,        "InSAR",       "Ground\nDisplacement",    CyanPrimary,    CyanContainer,    Modifier.weight(1f))
                FeatureChip(Icons.Filled.Satellite,    "NDVI",        "Vegetation\nHealth",      EmeraldPrimary, EmeraldContainer, Modifier.weight(1f))
                FeatureChip(Icons.Filled.Shield,       "Risk AI",     "7-Day\nForecast",         PurplePrimary,  PurpleContainer,  Modifier.weight(1f))
                FeatureChip(Icons.Filled.Notifications,"Alerts",      "Real-time\nWarnings",     RiskCritical,   RiskCriticalContainer, Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = BgBorder)
            Spacer(Modifier.height(28.dp))

            if (!isVerified) {
                // ── Auth Form ─────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(BgSurface)
                        .border(1.dp, BgBorder, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        if (!otpRequested) "Verify Your Identity" else "Enter Verification Code",
                        color      = TextPrimary,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (!otpRequested)
                            "Receive zone-based landslide alerts via SMS"
                        else
                            "A 6-digit code has been sent to +91 ${phoneNumber.takeLast(7).replace(Regex("\\d(?=\\d{4})"), "*")}",
                        color    = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    // Phone number field
                    OutlinedTextField(
                        value         = phoneNumber,
                        onValueChange = { if (it.length <= 10) phoneNumber = it },
                        modifier      = Modifier.fillMaxWidth(),
                        label         = { Text("Phone Number", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon   = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text("+91", color = CyanPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.width(6.dp))
                                Box(Modifier.size(1.dp, 20.dp).background(BgBorder))
                                Spacer(Modifier.width(4.dp))
                            }
                        },
                        enabled       = !otpRequested,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = CyanPrimary,
                            unfocusedBorderColor    = BgBorder,
                            focusedTextColor        = TextPrimary,
                            unfocusedTextColor      = TextPrimary,
                            cursorColor             = CyanPrimary,
                            focusedContainerColor   = BgElevated,
                            unfocusedContainerColor = BgElevated,
                            disabledBorderColor     = BgBorder,
                            disabledTextColor       = TextSecondary,
                            disabledContainerColor  = BgElevated
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (otpRequested) {
                        Spacer(Modifier.height(12.dp))
                        // OTP field
                        OutlinedTextField(
                            value         = otp,
                            onValueChange = { if (it.length <= 6) otp = it },
                            modifier      = Modifier.fillMaxWidth(),
                            label         = { Text("6-Digit OTP Code", color = TextMuted, fontSize = 12.sp) },
                            leadingIcon   = {
                                Icon(Icons.Filled.Lock, null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = CyanPrimary,
                                unfocusedBorderColor    = BgBorder,
                                focusedTextColor        = TextPrimary,
                                unfocusedTextColor      = TextPrimary,
                                cursorColor             = CyanPrimary,
                                focusedContainerColor   = BgElevated,
                                unfocusedContainerColor = BgElevated
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Primary Action Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (!otpRequested || otp.length < 6)
                                    Brush.linearGradient(listOf(CyanPrimary, EmeraldPrimary))
                                else
                                    Brush.linearGradient(listOf(EmeraldPrimary, CyanPrimary))
                            )
                            .clickable(
                                enabled = phoneNumber.length == 10 || (otpRequested && otp.length == 6)
                            ) {
                                if (!otpRequested) {
                                    otpRequested = true
                                } else {
                                    isVerified = true
                                    onVerified()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (!otpRequested) Icons.Filled.Phone else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF001822),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (!otpRequested) "SEND VERIFICATION CODE" else "VERIFY & ACTIVATE",
                                color         = Color(0xFF001822),
                                fontWeight    = FontWeight.ExtraBold,
                                fontSize      = 13.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    if (otpRequested) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Didn't receive the code? Resend",
                            color     = CyanPrimary,
                            fontSize  = 12.sp,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .clickable { otp = "" }
                                .padding(vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ── Verified Success State ────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(EmeraldContainer)
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CheckCircle, null, tint = EmeraldPrimary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Identity Verified", color = EmeraldPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Welcome to LandGuard Pro", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Zone Status Indicators ────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BgSurface)
                    .border(1.dp, BgBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("SYSTEM STATUS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp)
                Spacer(Modifier.height(2.dp))
                StatusRow("ALOS-4 PALSAR-3",      "Operational  •  Path 114",    EmeraldPrimary)
                StatusRow("Sentinel-2C MSI",       "Scheduled  •  Tile T45RVP",   EmeraldPrimary)
                StatusRow("Risk Engine v3.0",      "Active  •  AI Forecast ON",   CyanPrimary)
                StatusRow("Alert Dispatcher",      "Standby  •  5 Zones Active",  CyanPrimary)
                StatusRow("InSAR Processing Node", "Running  •  3 Detections",    RiskCritical)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "By continuing you agree to LandGuard's Terms of Use and Privacy Policy. Satellite data is provided by JAXA and ESA under their respective open-data licenses.",
                color     = TextMuted,
                fontSize  = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Feature Highlight Chip ──────────────────────────────────────────────────

@Composable
private fun FeatureChip(
    icon: ImageVector,
    label: String,
    description: String,
    accent: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(description, color = TextMuted, fontSize = 8.sp, textAlign = TextAlign.Center, lineHeight = 11.sp)
        }
    }
}

// ─── Status Row ──────────────────────────────────────────────────────────────

@Composable
private fun StatusRow(name: String, detail: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.width(10.dp))
        Text(name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(detail, color = TextSecondary, fontSize = 10.sp)
    }
}