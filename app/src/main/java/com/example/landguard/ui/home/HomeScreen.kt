package com.example.landguard.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.theme.*

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
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search land parcels or risk alerts...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = ForestPrimary) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = ForestPrimary,
                    unfocusedBorderColor = BorderSubtle
                )
            )
        }

        // Overview / Quick Stats Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SoftMintContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftMint),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Land Risk Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
                            Text("Active satellite & InSAR monitoring", fontSize = 12.sp, color = TextMuted)
                        }
                        Row {
                            IconButton(onClick = onOpenProfile) {
                                Icon(Icons.Filled.Person, contentDescription = "Profile", tint = ForestPrimary)
                            }
                            IconButton(onClick = onOpenMap) {
                                Icon(Icons.Filled.Radar, contentDescription = "Risk Map", tint = ForestPrimary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ColoredStatCard(
                            label = "Monitored",
                            value = uiState.parcels.size.toString(),
                            icon = Icons.Filled.Landscape,
                            containerColor = RiskLowContainer,
                            contentColor = ForestPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ColoredStatCard(
                            label = "Active Alerts",
                            value = uiState.alerts.size.toString(),
                            icon = Icons.Filled.Warning,
                            containerColor = RiskWarningContainer,
                            contentColor = RiskWarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                        ColoredStatCard(
                            label = "Max Risk",
                            value = "${uiState.parcels.maxOfOrNull { it.riskScore } ?: 0}%",
                            icon = Icons.Filled.Speed,
                            containerColor = RiskCriticalContainer,
                            contentColor = RiskCriticalRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Latest Satellite Observation Card (Sky Blue Theme)
        uiState.latestObservation?.let { obs ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMap() },
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SatelliteSky.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Surface(
                                    color = SatelliteSkyContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Satellite,
                                            contentDescription = null,
                                            tint = SatelliteSky,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            obs.provider,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SatelliteSky,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                obs.observationDate,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(obs.locationName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(obs.detectedChange, fontSize = 13.sp, color = TextMuted)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Colored Telemetry Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricChip(
                                label = "Shift",
                                value = "${obs.groundShiftMmPerYr} mm/y",
                                containerColor = RiskCriticalContainer,
                                textColor = RiskCriticalRed,
                                modifier = Modifier.weight(1f)
                            )
                            MetricChip(
                                label = "NDVI",
                                value = "${obs.ndviIndex}",
                                containerColor = RiskLowContainer,
                                textColor = ForestPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricChip(
                                label = "Moisture",
                                value = "${(obs.soilMoistureIndex * 100).toInt()}%",
                                containerColor = SatelliteSkyContainer,
                                textColor = SatelliteSky,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Recent Risk Alerts Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = RiskCriticalRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recent Risk Alerts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
                }
                TextButton(onClick = onOpenAlertHistory) {
                    Text("View All", color = ForestPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Recent Risk Alerts List
        items(uiState.alerts.take(3), key = { it.id }) { alert ->
            val (bgColor, borderColor, badgeColor) = when (alert.severity) {
                Severity.CRITICAL, Severity.HIGH -> Triple(RiskCriticalContainer, RiskCriticalRed.copy(alpha = 0.5f), RiskCriticalRed)
                Severity.MODERATE -> Triple(RiskWarningContainer, RiskWarningAmber.copy(alpha = 0.5f), RiskWarningAmber)
                Severity.LOW -> Triple(RiskLowContainer, RiskLowGreen.copy(alpha = 0.5f), RiskLowGreen)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAlert(alert.id) },
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (alert.severity == Severity.LOW) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            alert.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextCharcoal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${alert.affectedLocation} • ${alert.timestamp}",
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextMuted)
                }
            }
        }

        // Monitored Land Parcels Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Landscape, contentDescription = null, tint = ForestPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Monitored Land Parcels", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
            }
        }

        // Land Parcels List
        items(uiState.parcels, key = { it.id }) { parcel ->
            val (riskColor, riskBg) = when (parcel.riskCategory) {
                Severity.CRITICAL, Severity.HIGH -> Pair(RiskCriticalRed, RiskCriticalContainer)
                Severity.MODERATE -> Pair(RiskWarningAmber, RiskWarningContainer)
                Severity.LOW -> Pair(RiskLowGreen, RiskLowContainer)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenMap() },
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Top row: Parcel name & Risk tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = parcel.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextCharcoal,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = riskBg,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${parcel.riskScore}% Risk",
                                color = riskColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Location & Area
                    Text(
                        text = "${parcel.villageOrDistrict}, ${parcel.stateName} • ${parcel.areaHectares} ha",
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom row: Land Type badge & Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = SoftMintContainer,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, ForestPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = parcel.landType,
                                fontSize = 11.sp,
                                color = ForestPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View Radar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = ForestPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColoredStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = contentColor)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextCharcoal.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("$label: ", fontSize = 10.sp, fontWeight = FontWeight.Normal, color = TextMuted)
            Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
        }
    }
}
