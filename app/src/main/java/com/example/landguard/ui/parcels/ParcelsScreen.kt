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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.LandParcel
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.components.RiskBadge
import com.example.landguard.ui.home.HomeViewModel
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
fun ParcelsScreen(
    onOpenMap: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedParcel by remember {
        mutableStateOf<LandParcel?>(null)
    }

    /*
     * When a parcel is selected, show the complete parcel
     * detail screen instead of the list.
     */
    selectedParcel?.let { parcel ->

        ParcelDetailScreen(
            parcel = parcel,
            onBack = {
                selectedParcel = null
            },
            onOpenMap = {
                selectedParcel = null
                onOpenMap()
            }
        )

        return
    }

    val filteredParcels = remember(
        uiState.parcels,
        searchQuery
    ) {
        if (searchQuery.isBlank()) {
            uiState.parcels
        } else {
            val query = searchQuery.trim().lowercase()

            uiState.parcels.filter { parcel ->
                parcel.name.lowercase().contains(query) ||
                        parcel.id.lowercase().contains(query) ||
                        parcel.villageOrDistrict.lowercase().contains(query) ||
                        parcel.stateName.lowercase().contains(query) ||
                        parcel.landType.lowercase().contains(query)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {

        // ─────────────────────────────────────────────────────────
        // HEADER
        // ─────────────────────────────────────────────────────────

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 20.dp
                )
        ) {

            Text(
                text = "Parcels",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Your monitored land areas",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,

                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BrandPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                },

                placeholder = {
                    Text(
                        text = "Search parcels, locations...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },

                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
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

            Spacer(Modifier.height(16.dp))

            // Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                ParcelSummaryChip(
                    value = filteredParcels.size.toString(),
                    label = "Parcels",
                    modifier = Modifier.weight(1f)
                )

                ParcelSummaryChip(
                    value = filteredParcels.count {
                        it.riskCategory == Severity.HIGH ||
                                it.riskCategory == Severity.CRITICAL
                    }.toString(),
                    label = "High Risk",
                    modifier = Modifier.weight(1f)
                )

                ParcelSummaryChip(
                    value = String.format(
                        "%.1f",
                        filteredParcels.sumOf { it.areaHectares }
                    ),
                    label = "Hectares",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        // ─────────────────────────────────────────────────────────
        // LIST
        // ─────────────────────────────────────────────────────────

        if (filteredParcels.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(BrandContainer),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(27.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "No parcels found",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = "Try another search.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),

                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 95.dp
                ),

                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = filteredParcels,
                    key = { it.id }
                ) { parcel ->

                    ParcelCard(
                        parcel = parcel,
                        onClick = {
                            selectedParcel = parcel
                        }
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// SUMMARY CHIP
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ParcelSummaryChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = BgSurface,
        shape = RoundedCornerShape(13.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            BgBorder
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 11.dp,
                vertical = 10.dp
            )
        ) {

            Text(
                text = value,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = label,
                color = TextMuted,
                fontSize = 9.sp
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// PARCEL CARD
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ParcelCard(
    parcel: LandParcel,
    onClick: () -> Unit
) {
    val riskColor = when (parcel.riskCategory) {
        Severity.CRITICAL -> RiskCritical
        Severity.HIGH -> RiskHigh
        Severity.MODERATE -> RiskModerate
        Severity.LOW -> RiskLow
    }

    val riskBackground = when (parcel.riskCategory) {
        Severity.CRITICAL -> RiskCriticalContainer
        Severity.HIGH -> RiskHighContainer
        Severity.MODERATE -> RiskModerateContainer
        Severity.LOW -> RiskLowContainer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),

        color = BgSurface,

        shape = RoundedCornerShape(18.dp),

        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            BgBorder
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Parcel icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(riskBackground),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = parcel.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(3.dp))

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

            Spacer(Modifier.height(14.dp))

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(15.dp)
                )

                Spacer(Modifier.width(5.dp))

                Text(
                    text = "${parcel.villageOrDistrict}, ${parcel.stateName}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            // Information row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                ParcelInfo(
                    label = "AREA",
                    value = "${String.format("%.1f", parcel.areaHectares)} ha",
                    modifier = Modifier.weight(1f)
                )

                ParcelInfo(
                    label = "LAND TYPE",
                    value = parcel.landType,
                    modifier = Modifier.weight(1f)
                )

                ParcelInfo(
                    label = "RISK",
                    value = "${parcel.riskScore}/100",
                    valueColor = riskColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Bottom row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Updated ${parcel.lastUpdated}",
                    color = TextMuted,
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "View Details  ›",
                    color = BrandPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// PARCEL INFO
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ParcelInfo(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BgDeep)
            .padding(9.dp)
    ) {

        Text(
            text = label,
            color = TextMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}