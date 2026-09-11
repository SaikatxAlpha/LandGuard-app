package com.example.landguard.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.LandParcel
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.components.LandGuardAlertCard
import com.example.landguard.ui.components.LandGuardCard
import com.example.landguard.ui.components.LandGuardSectionHeader
import com.example.landguard.ui.components.LandGuardStatCard
import com.example.landguard.ui.components.RiskBadge
import com.example.landguard.ui.components.RiskGauge
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.BrandContainer
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.EmeraldContainer
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.RiskHigh
import com.example.landguard.ui.theme.RiskLow
import com.example.landguard.ui.theme.RiskModerate
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onOpenAlert: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenAlertHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val highestRiskParcel = uiState.parcels.maxByOrNull { it.riskScore }

    val currentRiskScore = highestRiskParcel?.riskScore ?: 0

    val activeAlerts = uiState.alerts.count {
        it.severity != Severity.LOW &&
                it.status != com.example.landguard.domain.model.AlertStatus.RESOLVED
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 18.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        // ─────────────────────────────────────────────────────────────
        // HEADER
        // ─────────────────────────────────────────────────────────────

        item {
            HomeHeader(
                activeAlerts = activeAlerts,
                onNotificationsClick = onOpenAlertHistory,
                onProfileClick = onOpenProfile
            )
        }

        // ─────────────────────────────────────────────────────────────
        // SEARCH
        // ─────────────────────────────────────────────────────────────

        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                placeholder = {
                    Text(
                        text = "Search parcels, locations or alerts...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BgSurface,
                    unfocusedContainerColor = BgSurface,
                    focusedBorderColor = BrandPrimary,
                    unfocusedBorderColor = BgBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = BrandPrimary
                )
            )
        }

        // ─────────────────────────────────────────────────────────────
        // CURRENT LAND RISK
        // ─────────────────────────────────────────────────────────────

        item {
            CurrentRiskCard(
                score = currentRiskScore,
                parcel = highestRiskParcel,
                onOpenMap = onOpenMap
            )
        }

        // ─────────────────────────────────────────────────────────────
        // STATISTICS
        // ─────────────────────────────────────────────────────────────

        item {
            LandGuardSectionHeader(
                title = "Overview",
                action = null,
                onAction = null
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LandGuardStatCard(
                    value = uiState.parcels.size.toString(),
                    label = "Monitored Parcels",
                    modifier = Modifier.weight(1f),
                    accent = BrandPrimary
                )

                LandGuardStatCard(
                    value = activeAlerts.toString(),
                    label = "Active Alerts",
                    modifier = Modifier.weight(1f),
                    accent = RiskCritical
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LandGuardStatCard(
                    value = uiState.parcels.count {
                        it.riskCategory == Severity.HIGH ||
                                it.riskCategory == Severity.CRITICAL
                    }.toString(),
                    label = "High-Risk Zones",
                    modifier = Modifier.weight(1f),
                    accent = RiskHigh
                )

                LandGuardStatCard(
                    value = uiState.totalMonitoredHa
                        .let { String.format("%.1f", it) },
                    label = "Hectares",
                    modifier = Modifier.weight(1f),
                    accent = RiskLow
                )
            }
        }

        // ─────────────────────────────────────────────────────────────
        // LIVE MAP
        // ─────────────────────────────────────────────────────────────

        item {
            LandGuardSectionHeader(
                title = "Live Map",
                action = "View Full Map",
                onAction = onOpenMap
            )

            Spacer(Modifier.height(10.dp))

            LiveMapCard(
                parcels = uiState.parcels,
                onClick = onOpenMap
            )
        }

        // ─────────────────────────────────────────────────────────────
        // ACTIVE ALERTS
        // ─────────────────────────────────────────────────────────────

        if (uiState.alerts.isNotEmpty()) {
            item {
                LandGuardSectionHeader(
                    title = "Recent Alerts",
                    action = "View All",
                    onAction = onOpenAlertHistory
                )
            }

            items(
                count = minOf(uiState.alerts.size, 3),
                key = { index -> uiState.alerts[index].id }
            ) { index ->

                val alert = uiState.alerts[index]

                LandGuardAlertCard(
                    title = alert.title,
                    location = alert.affectedLocation.ifBlank {
                        "Location unavailable"
                    },
                    timestamp = alert.timestamp.ifBlank {
                        "Recently detected"
                    },
                    severity = alert.severity,
                    description = alert.description,
                    onClick = {
                        onOpenAlert(alert.id)
                    }
                )
            }
        }

        // ─────────────────────────────────────────────────────────────
        // SATELLITE DATA
        // ─────────────────────────────────────────────────────────────

        item {
            uiState.latestObservation?.let { observation ->

                SatelliteSummaryCard(
                    provider = observation.provider,
                    location = observation.locationName,
                    observationDate = observation.observationDate,
                    ndvi = observation.ndviIndex,
                    confidence = observation.confidencePercentage,
                    onClick = onOpenMap
                )
            }
        }

        // ─────────────────────────────────────────────────────────────
        // MONITORED PARCELS
        // ─────────────────────────────────────────────────────────────

        if (uiState.parcels.isNotEmpty()) {
            item {
                LandGuardSectionHeader(
                    title = "Monitored Parcels",
                    action = "View Map",
                    onAction = onOpenMap
                )
            }

            items(
                count = minOf(uiState.parcels.size, 3),
                key = { index -> uiState.parcels[index].id }
            ) { index ->

                val parcel = uiState.parcels[index]

                ParcelSummaryCard(
                    parcel = parcel,
                    onClick = onOpenMap
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// HEADER
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun HomeHeader(
    activeAlerts: Int,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Good morning,",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "Explorer 🌿",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Monitor your land and detect changes early.",
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier.size(46.dp)
        ) {

            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(BgSurface)
                    .border(
                        width = 1.dp,
                        color = BgBorder,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = BrandPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }

            if (activeAlerts > 0) {
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(RiskCritical)
                        .border(
                            width = 2.dp,
                            color = BgDeep,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (activeAlerts > 9) "9+" else activeAlerts.toString(),
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = onProfileClick,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(BrandContainer)
                .border(
                    width = 1.dp,
                    color = BrandPrimary.copy(alpha = 0.18f),
                    shape = CircleShape
                )
        ) {
            Text(
                text = "E",
                color = BrandPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// CURRENT LAND RISK
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun CurrentRiskCard(
    score: Int,
    parcel: LandParcel?,
    onOpenMap: () -> Unit
) {
    val risk = when {
        score >= 80 -> Severity.CRITICAL
        score >= 60 -> Severity.HIGH
        score >= 35 -> Severity.MODERATE
        else -> Severity.LOW
    }

    LandGuardCard {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "CURRENT LAND RISK",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Overall Risk",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    RiskBadge(severity = risk)

                    Spacer(Modifier.height(10.dp))

                    if (parcel != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(15.dp)
                            )

                            Spacer(Modifier.width(4.dp))

                            Text(
                                text = parcel.villageOrDistrict,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                RiskGauge(
                    score = score,
                    size = 142.dp
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgDeep)
                    .clickable(onClick = onOpenMap)
                    .padding(
                        horizontal = 14.dp,
                        vertical = 11.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(17.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "View risk on map",
                    color = BrandPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// LIVE MAP CARD
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun LiveMapCard(
    parcels: List<LandParcel>,
    onClick: () -> Unit
) {
    LandGuardCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(
                    Color(0xFFE2EAE4)
                )
        ) {

            // Map-like geographic background.
            // The actual interactive MapLibre map opens when this card
            // is selected. This avoids replacing the real map with a
            // fake implementation.

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFFC8D7CC))
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFFC8D7CC))
            )

            // Risk area indicators
            parcels.take(5).forEachIndexed { index, parcel ->

                val riskColor = when (parcel.riskCategory) {
                    Severity.CRITICAL -> RiskCritical
                    Severity.HIGH -> RiskHigh
                    Severity.MODERATE -> RiskModerate
                    Severity.LOW -> RiskLow
                }

                Box(
                    modifier = Modifier
                        .size(
                            when (index) {
                                0 -> 54.dp
                                1 -> 44.dp
                                2 -> 36.dp
                                else -> 30.dp
                            }
                        )
                        .align(
                            when (index) {
                                0 -> Alignment.TopStart
                                1 -> Alignment.TopEnd
                                2 -> Alignment.Center
                                3 -> Alignment.BottomStart
                                else -> Alignment.BottomEnd
                            }
                        )
                        .padding(18.dp)
                        .clip(CircleShape)
                        .background(riskColor.copy(alpha = 0.65f))
                        .border(
                            2.dp,
                            riskColor.copy(alpha = 0.9f),
                            CircleShape
                        )
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                color = Color.White.copy(alpha = 0.94f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 7.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(RiskLow)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = "LIVE RISK MAP",
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                color = Color.White.copy(alpha = 0.96f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 9.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = "Open interactive map",
                        color = BrandPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// SATELLITE SUMMARY
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun SatelliteSummaryCard(
    provider: String,
    location: String,
    observationDate: String,
    ndvi: Double,
    confidence: Int,
    onClick: () -> Unit
) {
    LandGuardCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(BrandContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Satellite,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(11.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Latest Satellite Observation",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = location,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                SatelliteMetric(
                    value = String.format("%.2f", ndvi),
                    label = "NDVI",
                    modifier = Modifier.weight(1f)
                )

                SatelliteMetric(
                    value = "$confidence%",
                    label = "Confidence",
                    modifier = Modifier.weight(1f)
                )

                SatelliteMetric(
                    value = observationDate,
                    label = "Observed",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = provider,
                color = TextMuted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SatelliteMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BgDeep)
            .padding(10.dp)
    ) {
        Text(
            text = value,
            color = BrandPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp
        )
    }
}

// ═════════════════════════════════════════════════════════════════════
// PARCEL SUMMARY
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ParcelSummaryCard(
    parcel: LandParcel,
    onClick: () -> Unit
) {
    LandGuardCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {

        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (parcel.riskCategory) {
                            Severity.CRITICAL -> com.example.landguard.ui.theme.RiskCriticalContainer
                            Severity.HIGH -> com.example.landguard.ui.theme.RiskHighContainer
                            Severity.MODERATE -> com.example.landguard.ui.theme.RiskModerateContainer
                            Severity.LOW -> com.example.landguard.ui.theme.RiskLowContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = when (parcel.riskCategory) {
                        Severity.CRITICAL -> RiskCritical
                        Severity.HIGH -> RiskHigh
                        Severity.MODERATE -> RiskModerate
                        Severity.LOW -> RiskLow
                    },
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = parcel.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "${parcel.villageOrDistrict}, ${parcel.stateName}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${String.format("%.1f", parcel.areaHectares)} ha • ${parcel.landType}",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                RiskBadge(
                    severity = parcel.riskCategory
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "${parcel.riskScore}/100",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}