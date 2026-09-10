// app/src/main/java/com/example/landguard/ui/home/HomeScreen.kt

package com.example.landguard.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.ForecastDay
import com.example.landguard.domain.model.LandParcel
import com.example.landguard.domain.model.SatelliteObservation
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.theme.Alos4Brand
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.CyanContainer
import com.example.landguard.ui.theme.CyanGlow
import com.example.landguard.ui.theme.CyanPrimary
import com.example.landguard.ui.theme.EmeraldContainer
import com.example.landguard.ui.theme.EmeraldGlow
import com.example.landguard.ui.theme.EmeraldPrimary
import com.example.landguard.ui.theme.PurpleContainer
import com.example.landguard.ui.theme.PurpleGlow
import com.example.landguard.ui.theme.PurplePrimary
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.RiskCriticalContainer
import com.example.landguard.ui.theme.RiskCriticalGlow
import com.example.landguard.ui.theme.RiskHigh
import com.example.landguard.ui.theme.RiskHighContainer
import com.example.landguard.ui.theme.RiskHighGlow
import com.example.landguard.ui.theme.RiskLow
import com.example.landguard.ui.theme.RiskLowContainer
import com.example.landguard.ui.theme.RiskLowGlow
import com.example.landguard.ui.theme.RiskModerate
import com.example.landguard.ui.theme.RiskModerateContainer
import com.example.landguard.ui.theme.RiskModerateGlow
import com.example.landguard.ui.theme.SentinelBrand
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary
import com.example.landguard.ui.theme.TextWhite

// ─── Root Screen ──────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onOpenAlert: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenAlertHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {

        // ── Live Status Ticker ────────────────────────────────────────────────
        item { LiveStatusTicker() }

        // ── Search Bar ───────────────────────────────────────────────────────
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                placeholder = {
                    Text(
                        "Search parcels, alerts, coordinates…",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = BgSurface,
                    unfocusedContainerColor = BgSurface,
                    focusedBorderColor      = CyanPrimary,
                    unfocusedBorderColor    = BgBorder,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                    cursorColor             = CyanPrimary
                )
            )
        }

        // ── Mission Control Hero Card ─────────────────────────────────────────
        item {
            MissionControlCard(
                parcels   = uiState.parcels,
                alerts    = uiState.alerts,
                totalHa   = uiState.totalMonitoredHa,
                onOpenMap = onOpenMap,
                modifier  = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Quick Stats Row ───────────────────────────────────────────────────
        item {
            QuickStatsRow(
                monitored  = uiState.parcels.size,
                active     = uiState.alerts.count { it.severity != Severity.LOW },
                maxRisk    = uiState.parcels.maxOfOrNull { it.riskScore } ?: 0,
                totalHa    = uiState.totalMonitoredHa.toInt(),
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }

        // ── Satellite Pass Countdown ──────────────────────────────────────────
        item {
            SatellitePassCard(
                alos4Time     = "ALOS-4 PALSAR-3",
                alos4Eta      = "3h 42m",
                sentinelTime  = "Sentinel-2C",
                sentinelEta   = "11h 09m",
                modifier      = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── 7-Day Risk Forecast Strip ─────────────────────────────────────────
        if (uiState.forecastDays.isNotEmpty()) {
            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PurpleContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Timelapse, null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("7-Day Risk Forecast", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Surface(color = PurpleContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "AI-POWERED",
                            color = PurplePrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.forecastDays) { day ->
                        ForecastDayChip(day = day)
                    }
                }
            }
        }

        // ── Latest Satellite Observation ──────────────────────────────────────
        uiState.latestObservation?.let { obs ->
            item {
                Spacer(Modifier.height(14.dp))
                LatestObservationCard(
                    obs      = obs,
                    onClick  = onOpenMap,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // ── Alerts Section ────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(14.dp))
            ProSectionHeader(
                title      = "Active Risk Alerts",
                icon       = Icons.Filled.Notifications,
                accentColor = RiskCritical,
                badgeCount = uiState.alerts.count { it.severity != Severity.LOW },
                actionLabel = "All ${uiState.alerts.size}",
                onAction   = onOpenAlertHistory,
                modifier   = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        items(uiState.alerts.take(3), key = { it.id }) { alert ->
            AlertItemCard(
                alert    = alert,
                onClick  = { onOpenAlert(alert.id) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // ── Land Parcels Section ──────────────────────────────────────────────
        item {
            Spacer(Modifier.height(14.dp))
            ProSectionHeader(
                title       = "Monitored Land Parcels",
                icon        = Icons.Filled.Landscape,
                accentColor = EmeraldPrimary,
                modifier    = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        items(uiState.parcels, key = { it.id }) { parcel ->
            ParcelItemCard(
                parcel   = parcel,
                onClick  = onOpenMap,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

// ─── Live Status Ticker ─────────────────────────────────────────────────────

@Composable
private fun LiveStatusTicker() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = alpha))
        )
        Spacer(Modifier.width(8.dp))
        Text("LIVE", color = EmeraldPrimary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp)
        Spacer(Modifier.width(14.dp))
        Text(
            "•  ALOS-4 Active on Path 114  •  5 Zones Online  •  Last sync: 8m ago  •  Sentinel-2 Overpass: 3h 42m  •  InSAR Analysis: Running  •  Monsoon Alert Active",
            color = TextSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .basicMarquee()
        )
    }
}

// ─── Mission Control Hero Card ──────────────────────────────────────────────

@Composable
private fun MissionControlCard(
    parcels: List<LandParcel>,
    alerts: List<Alert>,
    totalHa: Double,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val criticalCount = parcels.count { it.riskCategory == Severity.CRITICAL }
    val highCount     = parcels.count { it.riskCategory == Severity.HIGH }
    val maxRisk       = parcels.maxOfOrNull { it.riskScore } ?: 0
    val isCriticalState = criticalCount > 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        if (isCriticalState) Color(0xFF200010) else Color(0xFF001828),
                        BgElevated,
                        BgSurface
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isCriticalState) RiskCritical.copy(alpha = 0.5f) else CyanPrimary.copy(alpha = 0.4f),
                        BgBorder,
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "Mission Control",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        "Land Risk Command Center",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    color = if (isCriticalState) RiskCriticalContainer else EmeraldContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isCriticalState) RiskCritical else EmeraldPrimary)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isCriticalState) "CRITICAL" else "STABLE",
                            color = if (isCriticalState) RiskCritical else EmeraldPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Risk distribution bar
            val total = parcels.size.toFloat().coerceAtLeast(1f)
            val cFrac = criticalCount / total
            val hFrac = highCount / total
            val mFrac = parcels.count { it.riskCategory == Severity.MODERATE } / total
            val lFrac = parcels.count { it.riskCategory == Severity.LOW } / total

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Risk Distribution", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                    Text("${parcels.size} parcels", color = TextMuted, fontSize = 10.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                ) {
                    if (cFrac > 0) Box(Modifier.weight(cFrac).fillMaxHeight().background(RiskCritical))
                    if (hFrac > 0) Box(Modifier.weight(hFrac).fillMaxHeight().background(RiskHigh))
                    if (mFrac > 0) Box(Modifier.weight(mFrac).fillMaxHeight().background(RiskModerate))
                    if (lFrac > 0) Box(Modifier.weight(lFrac).fillMaxHeight().background(RiskLow))
                }
            }

            Spacer(Modifier.height(18.dp))

            // Mini metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetricChip("MAX RISK", "$maxRisk%",    RiskCritical,  RiskCriticalContainer, Modifier.weight(1f))
                HeroMetricChip("ALERTS",  "${alerts.size}", RiskModerate,  RiskModerateContainer, Modifier.weight(1f))
                HeroMetricChip("ZONES",   "${parcels.size}", CyanPrimary,  CyanContainer,          Modifier.weight(1f))
                HeroMetricChip("HECTARES","${totalHa.toInt()}", EmeraldPrimary, EmeraldContainer, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // Open Map CTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyanPrimary.copy(alpha = 0.10f))
                    .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenMap),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Radar, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "OPEN RISK MAP",
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetricChip(
    label: String, value: String,
    accent: Color, bg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
    }
}

// ─── Quick Stats Row ─────────────────────────────────────────────────────────

@Composable
private fun QuickStatsRow(
    monitored: Int, active: Int,
    maxRisk: Int, totalHa: Int,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("MONITORED", "$monitored", EmeraldPrimary,   EmeraldContainer, Modifier.weight(1f))
        StatCard("ALERTS",    "$active",    RiskCritical,     RiskCriticalContainer, Modifier.weight(1f))
        StatCard("MAX RISK",  "$maxRisk%",  RiskModerate,     RiskModerateContainer, Modifier.weight(1f))
        StatCard("HA",        "$totalHa",   CyanPrimary,      CyanContainer,    Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    label: String, value: String,
    accent: Color, bg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
    }
}

// ─── Satellite Pass Countdown ────────────────────────────────────────────────

@Composable
private fun SatellitePassCard(
    alos4Time: String, alos4Eta: String,
    sentinelTime: String, sentinelEta: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = BgSurface),
        shape    = RoundedCornerShape(18.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, BgBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Satellite, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Upcoming Satellite Passes", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PassChip(
                    name    = alos4Time,
                    eta     = alos4Eta,
                    sub     = "L-Band SAR • InSAR Active",
                    accent  = Alos4Brand,
                    bg      = RiskHighContainer,
                    modifier = Modifier.weight(1f)
                )
                PassChip(
                    name    = sentinelTime,
                    eta     = sentinelEta,
                    sub     = "10m Optical • VNIR+SWIR",
                    accent  = SentinelBrand,
                    bg      = CyanContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PassChip(
    name: String, eta: String, sub: String,
    accent: Color, bg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(name, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(eta, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(sub, color = TextMuted, fontSize = 9.sp)
        }
    }
}

// ─── 7-Day Forecast Chip ────────────────────────────────────────────────────

@Composable
private fun ForecastDayChip(day: ForecastDay) {
    val (accent, bg, glow) = riskColors(day.riskCategory)
    val isToday = day.dayLabel == "TODAY"

    Box(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isToday) bg else BgSurface)
            .border(
                width = if (isToday) 1.5.dp else 1.dp,
                color = if (isToday) accent else BgBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                day.dayLabel,
                color      = if (isToday) accent else TextSecondary,
                fontSize   = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(day.conditionEmoji, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "${day.riskScore}%",
                color      = accent,
                fontSize   = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WaterDrop, null, tint = CyanPrimary, modifier = Modifier.size(9.dp))
                Text("${day.rainChancePct}%", color = CyanPrimary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(2.dp))
            Text("${day.tempCelsius}°C", color = TextMuted, fontSize = 9.sp)
        }
    }
}

// ─── Latest Observation Card ─────────────────────────────────────────────────

@Composable
private fun LatestObservationCard(
    obs: SatelliteObservation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = BgSurface),
        shape    = RoundedCornerShape(20.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Provider row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = CyanContainer, shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Satellite, null, tint = CyanPrimary, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            obs.provider.take(28),
                            color = CyanPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Surface(color = EmeraldContainer, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "LIVE DATA",
                        color = EmeraldPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(obs.locationName, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(obs.detectedChange, color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(obs.observationDate, color = TextMuted, fontSize = 10.sp)

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = BgBorder)
            Spacer(Modifier.height(14.dp))

            // Telemetry chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryChip("SHIFT",    "${obs.groundShiftMmPerYr} mm/y", RiskCritical,  RiskCriticalContainer, Modifier.weight(1f))
                TelemetryChip("NDVI",     "${obs.ndviIndex}",                EmeraldPrimary, EmeraldContainer,      Modifier.weight(1f))
                TelemetryChip("MOISTURE", "${(obs.soilMoistureIndex * 100).toInt()}%", CyanPrimary, CyanContainer, Modifier.weight(1f))
                TelemetryChip("SLOPE",    "${obs.slopeAngleDegrees}°",       PurplePrimary, PurpleContainer,        Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TelemetryChip(
    label: String, value: String,
    accent: Color, bg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
    }
}

// ─── Pro Section Header ──────────────────────────────────────────────────────

@Composable
private fun ProSectionHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    badgeCount: Int = 0,
    actionLabel: String = "",
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (badgeCount > 0) {
            Spacer(Modifier.width(8.dp))
            Surface(color = RiskCriticalContainer, shape = CircleShape) {
                Text(
                    "$badgeCount",
                    color = RiskCritical,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (onAction != null && actionLabel.isNotBlank()) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.ChevronRight, null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─── Alert Item Card ─────────────────────────────────────────────────────────

@Composable
private fun AlertItemCard(
    alert: Alert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (accent, bg, glow) = riskColors(alert.severity)

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = BgSurface),
        shape    = RoundedCornerShape(16.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, BgBorder)
    ) {
        Row(modifier = Modifier.height(86.dp)) {
            // Left severity strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.4f)))
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Warning, null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        alert.title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        alert.affectedLocation,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                alert.severity.name,
                                color = accent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(alert.timestamp, color = TextMuted, fontSize = 10.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("${alert.confidencePercentage}% conf.", color = TextMuted, fontSize = 10.sp)
                    }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─── Parcel Item Card ────────────────────────────────────────────────────────

@Composable
private fun ParcelItemCard(
    parcel: LandParcel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (accent, bg, glow) = riskColors(parcel.riskCategory)

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = BgSurface),
        shape    = RoundedCornerShape(16.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, BgBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    parcel.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(10.dp))
                // Risk score badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "${parcel.riskScore}%",
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${parcel.villageOrDistrict}, ${parcel.stateName}  •  ${parcel.areaHectares} ha",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = BgBorder)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = EmeraldContainer,
                    shape = RoundedCornerShape(7.dp)
                ) {
                    Text(
                        parcel.landType,
                        color = EmeraldPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("View on Radar", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.ChevronRight, null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private data class RiskColors(val accent: Color, val bg: Color, val glow: Color)

private fun riskColors(severity: Severity): RiskColors = when (severity) {
    Severity.CRITICAL -> RiskColors(RiskCritical, RiskCriticalContainer, RiskCriticalGlow)
    Severity.HIGH     -> RiskColors(RiskHigh,     RiskHighContainer,     RiskHighGlow)
    Severity.MODERATE -> RiskColors(RiskModerate, RiskModerateContainer, RiskModerateGlow)
    Severity.LOW      -> RiskColors(RiskLow,      RiskLowContainer,      RiskLowGlow)
}