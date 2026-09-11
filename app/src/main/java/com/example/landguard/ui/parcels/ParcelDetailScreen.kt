package com.example.landguard.ui.parcels

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.landguard.domain.model.LandParcel
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.components.LandGuardButton
import com.example.landguard.ui.components.LandGuardOutlinedButton
import com.example.landguard.ui.components.RiskBadge
import com.example.landguard.ui.components.RiskGauge
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.BrandContainer
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.RiskCriticalContainer
import com.example.landguard.ui.theme.RiskHigh
import com.example.landguard.ui.theme.RiskHighContainer
import com.example.landguard.ui.theme.RiskLow
import com.example.landguard.ui.theme.RiskLowContainer
import com.example.landguard.ui.theme.RiskModerate
import com.example.landguard.ui.theme.RiskModerateContainer
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary

@Composable
fun ParcelDetailScreen(
    parcel: LandParcel,
    onBack: () -> Unit,
    onOpenMap: () -> Unit
) {
    var selectedTab by remember {
        mutableStateOf("Overview")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {

        // ═══════════════════════════════════════════════════════════
        // TOP BAR
        // ═══════════════════════════════════════════════════════════

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 18.dp,
                    top = 14.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clickable(onClick = onBack),

                color = BgSurface,
                shape = CircleShape,

                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    BgBorder
                )
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Parcel Details",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = parcel.id,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            RiskBadge(
                severity = parcel.riskCategory
            )
        }

        // ═══════════════════════════════════════════════════════════
        // CONTENT
        // ═══════════════════════════════════════════════════════════

        LazyColumn(
            modifier = Modifier.fillMaxSize(),

            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 30.dp
            ),

            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ─────────────────────────────────────────────────────
            // MAP PREVIEW
            // ─────────────────────────────────────────────────────

            item {

                ParcelMapPreview(
                    parcel = parcel,
                    onClick = onOpenMap
                )
            }

            // ─────────────────────────────────────────────────────
            // NAME
            // ─────────────────────────────────────────────────────

            item {

                Column {

                    Text(
                        text = parcel.name,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(5.dp))

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
                            text = "${parcel.villageOrDistrict}, ${parcel.stateName}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────
            // TABS
            // ─────────────────────────────────────────────────────

            item {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(BgSurface)
                        .border(
                            1.dp,
                            BgBorder,
                            RoundedCornerShape(13.dp)
                        )
                        .padding(4.dp)
                ) {

                    listOf(
                        "Overview",
                        "Satellite",
                        "History",
                        "Reports"
                    ).forEach { tab ->

                        ParcelDetailTab(
                            text = tab,
                            selected = selectedTab == tab,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTab = tab
                            }
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────
            // TAB CONTENT
            // ─────────────────────────────────────────────────────

            item {

                when (selectedTab) {

                    "Overview" -> {

                        OverviewContent(
                            parcel = parcel
                        )
                    }

                    "Satellite" -> {

                        SatelliteContent(
                            parcel = parcel
                        )
                    }

                    "History" -> {

                        HistoryContent(
                            parcel = parcel
                        )
                    }

                    "Reports" -> {

                        ReportsContent(
                            parcel = parcel
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────
            // ACTIONS
            // ─────────────────────────────────────────────────────

            item {

                Column {

                    LandGuardButton(
                        text = "View on Map",
                        onClick = onOpenMap,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(9.dp))

                    LandGuardOutlinedButton(
                        text = "Generate Report",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// MAP PREVIEW
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ParcelMapPreview(
    parcel: LandParcel,
    onClick: () -> Unit
) {
    val riskColor = when (parcel.riskCategory) {
        Severity.CRITICAL -> RiskCritical
        Severity.HIGH -> RiskHigh
        Severity.MODERATE -> RiskModerate
        Severity.LOW -> RiskLow
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFDDE7DF))
            .clickable(onClick = onClick)
    ) {

        // Geographic-looking map surface.
        // The actual interactive MapLibre map is opened using
        // "View on Map".

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.Center)
                .background(Color(0xFFBFCFC3))
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .align(Alignment.Center)
                .background(Color(0xFFBFCFC3))
        )

        // Parcel boundary
        Box(
            modifier = Modifier
                .size(125.dp)
                .align(Alignment.Center)
                .border(
                    width = 3.dp,
                    color = riskColor,
                    shape = RoundedCornerShape(18.dp)
                )
                .background(
                    riskColor.copy(alpha = 0.18f),
                    RoundedCornerShape(18.dp)
                )
        )

        // Center marker
        Box(
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(riskColor)
                .border(
                    3.dp,
                    Color.White,
                    CircleShape
                )
        )

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

                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(15.dp)
                )

                Spacer(Modifier.width(5.dp))

                Text(
                    text = "PARCEL MAP",
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
            shape = RoundedCornerShape(11.dp)
        ) {

            Text(
                text = "Open interactive map  ›",
                color = BrandPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    horizontal = 13.dp,
                    vertical = 8.dp
                )
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// DETAIL TAB
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ParcelDetailTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),

        color = if (selected) {
            BrandPrimary
        } else {
            Color.Transparent
        },

        shape = RoundedCornerShape(10.dp)
    ) {

        Box(
            modifier = Modifier.padding(
                vertical = 9.dp
            ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,
                color = if (selected) {
                    Color.White
                } else {
                    TextSecondary
                },
                fontSize = 9.sp,
                fontWeight = if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// OVERVIEW
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun OverviewContent(
    parcel: LandParcel
) {
    Column {

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BgSurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                BgBorder
            )
        ) {

            Column(
                modifier = Modifier.padding(17.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Risk Assessment",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Current land risk score",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    RiskGauge(
                        score = parcel.riskScore,
                        size = 105.dp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BgSurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                BgBorder
            )
        ) {

            Column(
                modifier = Modifier.padding(17.dp)
            ) {

                Text(
                    text = "Parcel Information",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(10.dp))

                DetailRow(
                    label = "Parcel ID",
                    value = parcel.id
                )

                DetailRow(
                    label = "Land Type",
                    value = parcel.landType
                )

                DetailRow(
                    label = "Area",
                    value = "${String.format("%.2f", parcel.areaHectares)} hectares"
                )

                DetailRow(
                    label = "Risk Score",
                    value = "${parcel.riskScore}/100"
                )

                DetailRow(
                    label = "Risk Category",
                    value = parcel.riskCategory.name
                )

                DetailRow(
                    label = "Last Updated",
                    value = parcel.lastUpdated
                )

                DetailRow(
                    label = "Latitude",
                    value = String.format("%.6f", parcel.latitude)
                )

                DetailRow(
                    label = "Longitude",
                    value = String.format("%.6f", parcel.longitude)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// SATELLITE
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun SatelliteContent(
    parcel: LandParcel
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            BgBorder
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(40.dp)
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

                Spacer(Modifier.width(10.dp))

                Column {

                    Text(
                        text = "Satellite Observation",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Latest available observation",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(15.dp))

            DetailRow(
                label = "Observation Area",
                value = parcel.name
            )

            DetailRow(
                label = "Location",
                value = parcel.villageOrDistrict
            )

            DetailRow(
                label = "Last Updated",
                value = parcel.lastUpdated
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Satellite imagery can be opened from the Map and Satellite Data sections.",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// HISTORY
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryContent(
    parcel: LandParcel
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            BgBorder
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(21.dp)
                )

                Spacer(Modifier.width(9.dp))

                Text(
                    text = "Observation History",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(15.dp))

            HistoryItem(
                date = parcel.lastUpdated,
                title = "Latest land assessment",
                description = "Current risk score: ${parcel.riskScore}/100"
            )

            HistoryItem(
                date = "Previous observation",
                title = "Satellite monitoring",
                description = "Parcel monitoring remains active."
            )

            HistoryItem(
                date = "Monitoring started",
                title = "Parcel added",
                description = "LandGuard began monitoring this parcel."
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// REPORTS
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ReportsContent(
    parcel: LandParcel
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            BgBorder
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(21.dp)
                )

                Spacer(Modifier.width(9.dp))

                Column {

                    Text(
                        text = "Parcel Reports",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Risk and observation reports",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(15.dp))

            Text(
                text = "Current Risk Summary",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text = "${parcel.riskCategory.name} risk • ${parcel.riskScore}/100",
                color = when (parcel.riskCategory) {
                    Severity.CRITICAL -> RiskCritical
                    Severity.HIGH -> RiskHigh
                    Severity.MODERATE -> RiskModerate
                    Severity.LOW -> RiskLow
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Generate a complete report from the Reports section.",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// DETAIL ROW
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
    ) {

        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.width(105.dp)
        )

        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════
// HISTORY ITEM
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryItem(
    date: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
    ) {

        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(BrandPrimary)
        )

        Spacer(Modifier.width(10.dp))

        Column {

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 10.sp
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = date,
                color = TextMuted,
                fontSize = 9.sp
            )
        }
    }
}