package com.example.landguard.ui.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.LandCoverBreakdown
import com.example.landguard.domain.model.SatelliteObservation
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.theme.BorderSubtle
import com.example.landguard.ui.theme.CardSurface
import com.example.landguard.ui.theme.ForestDark
import com.example.landguard.ui.theme.ForestPrimary
import com.example.landguard.ui.theme.LightBackground
import com.example.landguard.ui.theme.RiskCriticalRed
import com.example.landguard.ui.theme.RiskLowGreen
import com.example.landguard.ui.theme.RiskModerateYellow
import com.example.landguard.ui.theme.RiskWarningAmber
import com.example.landguard.ui.theme.SatelliteSky
import com.example.landguard.ui.theme.SatelliteSkyContainer
import com.example.landguard.ui.theme.SoftMint
import com.example.landguard.ui.theme.SoftMintContainer
import com.example.landguard.ui.theme.TextCharcoal
import com.example.landguard.ui.theme.TextMuted

@Composable
fun HomeScreen(
    onOpenAlert: (String) -> Unit = {},
    onOpenMap: () -> Unit = {},
    onOpenAlertHistory: () -> Unit = {},
    onOpenReport: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedObservationDetails by remember { mutableStateOf<SatelliteObservation?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ForestPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Header Greeting & Global Search
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Good morning, Explorer 🌱",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextCharcoal
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Monitor land. Detect risks. Enable a sustainable tomorrow.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Search by location, parcel ID, or village...", fontSize = 13.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = ForestPrimary) },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SoftMintContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Ctrl K", fontSize = 10.sp, color = ForestPrimary, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface,
                            focusedBorderColor = ForestPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextCharcoal,
                            unfocusedTextColor = TextCharcoal
                        )
                    )
                }

                // 2. 4 Top KPI Stat Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiStatCard("Monitored Parcels", "12,482", "↑ 12% (+1,328)", Icons.Filled.Landscape, ForestPrimary, SoftMintContainer, modifier = Modifier.weight(1f))
                        KpiStatCard("Active Alerts", "37", "↑ 8% (+3 this week)", Icons.Filled.Notifications, RiskWarningAmber, Color(0xFFFEF3C7), modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiStatCard("High-Risk Zones", "18", "↓ 22% (-5 this month)", Icons.Filled.Warning, RiskCriticalRed, Color(0xFFFEE2E2), modifier = Modifier.weight(1f))
                        KpiStatCard("Satellite Updates", "2,846", "↑ 16% (New scenes)", Icons.Filled.Layers, SatelliteSky, SatelliteSkyContainer, modifier = Modifier.weight(1f))
                    }
                }

                // 3. Land Risk Map Banner Card
                item {
                    Card(
                        onClick = onOpenMap,
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SoftMint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Map, null, tint = ForestPrimary, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Land Risk Map",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextCharcoal,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Explore land parcels, risk zones and satellite insights.",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SoftMintContainer)
                                        .border(1.dp, ForestPrimary, RoundedCornerShape(10.dp))
                                        .clickable { onOpenMap() }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Fullscreen, null, tint = ForestPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Full Screen", fontSize = 11.sp, color = ForestPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Embedded Interactive Map Box Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ForestDark)
                            ) {
                                // Simulated Satellite Map Texture
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFF1E3A2B)))
                                    Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFF142E23)))
                                }
                                
                                // Floating Timestamp Badge (Top Right)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CardSurface.copy(alpha = 0.92f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Last updated: 12 Oct 2024, 10:30 AM", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                                }

                                // Floating Overlay Legend (Bottom Left)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CardSurface.copy(alpha = 0.95f))
                                        .padding(8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LegendItem("Low Risk (56%)", RiskLowGreen)
                                        LegendItem("Moderate Risk (24%)", RiskModerateYellow)
                                        LegendItem("High Risk (14%)", RiskWarningAmber)
                                        LegendItem("Critical Risk (6%)", RiskCriticalRed)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Risk Distribution Donut Chart Card
                item {
                    SectionHeader("Risk Distribution")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Donut Chart
                            RiskDonutChart(
                                modifier = Modifier.size(130.dp)
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            // Legend List
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RiskLegendRow("Low Risk", "56%", "6,990", RiskLowGreen)
                                RiskLegendRow("Moderate Risk", "24%", "2,996", RiskModerateYellow)
                                RiskLegendRow("High Risk", "14%", "1,747", RiskWarningAmber)
                                RiskLegendRow("Critical Risk", "6%", "749", RiskCriticalRed)
                            }
                        }
                    }
                }

                // 5. Extended Satellite Observation Card
                state.latestObservation?.let { obs ->
                    item {
                        SectionHeader("Latest Satellite Observation")
                        SatelliteObservationCard(
                            obs = obs,
                            onExpandDetails = { selectedObservationDetails = obs }
                        )
                    }
                }

                // 6. Land Use / Cover Breakdown Card
                item {
                    SectionHeader("Land Use / Cover Breakdown")
                    LandCoverCard(state.landCover)
                }

                // 7. Recent Threat Alerts Log
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("Recent Threat Alerts")
                        Text(
                            text = "View All →",
                            color = ForestPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onOpenAlertHistory() }
                        )
                    }
                }

                items(state.alerts) { alert ->
                    AlertItemCard(alert = alert, onClick = { onOpenAlert(alert.id) })
                }

                // 8. Scenic Nature Quote Banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ForestDark),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Land today.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text("A safer tomorrow.", color = SoftMint, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Data-driven decisions for people, nature and progress.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    selectedObservationDetails?.let { obs ->
        ObservationDetailDialog(
            obs = obs,
            onDismiss = { selectedObservationDetails = null }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextCharcoal
    )
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    containerBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(containerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RiskDonutChart(
    lowPct: Float = 0.56f,
    modPct: Float = 0.24f,
    highPct: Float = 0.14f,
    critPct: Float = 0.06f,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 32f
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f

            // Low Risk (Green)
            val sweepLow = lowPct * 360f
            drawArc(color = RiskLowGreen, startAngle = startAngle, sweepAngle = sweepLow - 4f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            startAngle += sweepLow

            // Moderate Risk (Yellow)
            val sweepMod = modPct * 360f
            drawArc(color = RiskModerateYellow, startAngle = startAngle, sweepAngle = sweepMod - 4f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            startAngle += sweepMod

            // High Risk (Orange)
            val sweepHigh = highPct * 360f
            drawArc(color = RiskWarningAmber, startAngle = startAngle, sweepAngle = sweepHigh - 4f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            startAngle += sweepHigh

            // Critical Risk (Red)
            val sweepCrit = critPct * 360f
            drawArc(color = RiskCriticalRed, startAngle = startAngle, sweepAngle = sweepCrit - 4f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("12,482", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextCharcoal)
            Text("Parcels", fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun RiskLegendRow(label: String, percentage: String, count: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 11.sp, color = TextCharcoal, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(percentage, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
        Spacer(modifier = Modifier.width(6.dp))
        Text("($count)", fontSize = 10.sp, color = TextMuted)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 8.sp, color = TextCharcoal, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SatelliteObservationCard(
    obs: SatelliteObservation,
    onExpandDetails: () -> Unit
) {
    Card(
        onClick = onExpandDetails,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Info & Before/After Image Thumbnail
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Split Thumbnail Box
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ForestDark)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFF2D5A27)), contentAlignment = Alignment.BottomStart) {
                            Text("10 Sep", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFF8B4513)), contentAlignment = Alignment.BottomEnd) {
                            Text("10 Oct", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CompareArrows, null, tint = ForestDark, modifier = Modifier.size(12.dp))
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = obs.locationName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextCharcoal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Acquired: ${obs.observationDate}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = obs.provider,
                        fontSize = 10.sp,
                        color = ForestPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricColumn("Detected Change", obs.detectedChange, SatelliteSky, modifier = Modifier.weight(1.2f))
                MetricColumn("Confidence", "${obs.confidencePercentage}%", ForestPrimary, modifier = Modifier.weight(0.8f))
                MetricColumn("InSAR Shift", "${obs.groundShiftMmPerYr} mm/y", RiskCriticalRed, modifier = Modifier.weight(1.0f))
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            // Extended Environmental Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniTelemetryChip("NDVI", "${obs.ndviIndex}", Icons.Filled.Landscape, ForestPrimary)
                MiniTelemetryChip("Moisture", "${(obs.soilMoistureIndex * 100).toInt()}%", Icons.Filled.Waves, SatelliteSky)
                MiniTelemetryChip("Backscatter", "${obs.radarBackscatterDb} dB", Icons.Filled.Radar, RiskWarningAmber)
                MiniTelemetryChip("Slope", "${obs.slopeAngleDegrees}°", Icons.Filled.Speed, ForestDark)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actionable Technical Recommendation Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftMintContainer)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = ForestPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Action: ${obs.recommendedAction}",
                        fontSize = 11.sp,
                        color = ForestDark,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View Full Details Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View full telemetry & orbit specs",
                    fontSize = 11.sp,
                    color = ForestPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Filled.ChevronRight, null, tint = ForestPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MiniTelemetryChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
            Text(label, fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MetricColumn(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 10.sp,
            color = TextMuted,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LandCoverCard(cover: LandCoverBreakdown) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LandTypeBar("Agricultural Land", cover.agriculturalPercentage, 5991, ForestPrimary)
            LandTypeBar("Forest & Canopy", cover.forestPercentage, 3494, RiskLowGreen)
            LandTypeBar("Urban / Built-up", cover.urbanPercentage, 1497, SatelliteSky)
            LandTypeBar("Water Bodies", cover.waterPercentage, 873, Color(0xFF0284C7))
            LandTypeBar("Others", cover.otherPercentage, 627, TextMuted)
        }
    }
}

@Composable
private fun LandTypeBar(name: String, percentage: Int, count: Int, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 12.sp, color = TextCharcoal, fontWeight = FontWeight.Medium)
            Text("$percentage% ($count)", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = BorderSubtle
        )
    }
}

@Composable
private fun AlertItemCard(alert: Alert, onClick: () -> Unit) {
    val severityColor = when (alert.severity) {
        Severity.CRITICAL -> RiskCriticalRed
        Severity.HIGH -> RiskWarningAmber
        Severity.MODERATE -> RiskModerateYellow
        Severity.LOW -> RiskLowGreen
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(severityColor)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCharcoal)
                Text("${alert.affectedLocation} • ${alert.timestamp}", fontSize = 11.sp, color = TextMuted)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObservationDetailDialog(
    obs: SatelliteObservation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Radar, null, tint = ForestPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Satellite Observation Specs", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
                    Text(obs.locationName, fontSize = 12.sp, color = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Provider Array", obs.provider)
                DetailRow("Acquisition Date", obs.observationDate)
                DetailRow("Orbit Direction", obs.orbitDetails)
                DetailRow("Spatial Resolution", obs.spatialResolution)
                DetailRow("Polarization", obs.polarization)
                DetailRow("Cloud Cover", "${obs.cloudCoverPercentage}% (Penetrated)")
                DetailRow("Detected Change", obs.detectedChange, SatelliteSky)
                DetailRow("Confidence Score", "${obs.confidencePercentage}%", ForestPrimary)
                DetailRow("InSAR Movement", "${obs.groundShiftMmPerYr} mm/year", RiskCriticalRed)
                DetailRow("NDVI Index", "${obs.ndviIndex}", ForestPrimary)
                DetailRow("Soil Saturation", "${(obs.soilMoistureIndex * 100).toInt()}%", SatelliteSky)
                DetailRow("Radar Backscatter", "${obs.radarBackscatterDb} dB", RiskWarningAmber)
                DetailRow("Slope Steepness", "${obs.slopeAngleDegrees}°", ForestDark)
                
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftMintContainer)
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Recommended Mitigation Protocol", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestDark)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(obs.recommendedAction, fontSize = 11.sp, color = TextCharcoal)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary)
            ) {
                Text("CLOSE TELEMETRY", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = TextCharcoal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SoftMintContainer.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    com.example.landguard.ui.theme.LandGuardTheme {
        Column(modifier = Modifier.fillMaxSize().background(LightBackground).padding(16.dp)) {
            KpiStatCard("Monitored Parcels", "12,482", "+1,328 from last month", Icons.Filled.Landscape, ForestPrimary, SoftMintContainer)
            Spacer(modifier = Modifier.height(12.dp))
            LandCoverCard(LandCoverBreakdown())
        }
    }
}
