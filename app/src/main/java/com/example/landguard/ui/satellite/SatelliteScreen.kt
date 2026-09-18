package com.example.landguard.ui.satellite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.ui.home.PanelButton
import com.example.landguard.ui.location.LocalRequestUserLocation
import com.example.landguard.ui.location.LocalUserLocation
import com.example.landguard.ui.location.LocationStatus
import com.example.landguard.ui.monitor.LiveReadings
import com.example.landguard.ui.monitor.RiskIndexSummary
import com.example.landguard.ui.monitor.formatTime
import com.example.landguard.ui.risk.AnalysisState
import com.example.landguard.ui.risk.RiskAreasViewModel
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.BrandContainer
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary
import java.util.Locale

/**
 * Satellite & live data for the device's GPS position.
 *
 * Every value shown comes from a real source (Sentinel-2, Sentinel-1,
 * Open-Meteo, Copernicus DEM, NASA landslide catalog) with its acquisition
 * date; anything that could not be obtained is shown as "Data unavailable".
 */
@Composable
fun SatelliteScreen(
    onBack: () -> Unit = {},
    viewModel: RiskAreasViewModel = hiltViewModel()
) {
    val location = LocalUserLocation.current
    val requestLocation = LocalRequestUserLocation.current
    val analysis by viewModel.userAnalysis.collectAsStateWithLifecycle()

    LaunchedEffect(location.location) {
        location.location?.let { viewModel.analyzeUserLocation(it.latitude, it.longitude) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(BrandContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.SatelliteAlt, null, tint = BrandPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Satellite Data", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Latest real observations at your location", color = TextSecondary, fontSize = 12.sp)
            }
            val loc = location.location
            if (loc != null) {
                IconButton(onClick = { viewModel.analyzeUserLocation(loc.latitude, loc.longitude, force = true) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                }
            }
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Spacer(Modifier.height(16.dp))

        val loc = location.location
        if (loc == null) {
            LocationNeeded(
                status = location.status,
                onEnable = requestLocation
            )
            return@Column
        }

        // Where the analysis is for
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgSurface)
                .border(1.dp, BgBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.MyLocation, null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    location.placeLabel ?: "Your GPS position",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "%.4f, %.4f · 2.2 km analysis box".format(Locale.US, loc.latitude, loc.longitude),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val ready = analysis as? AnalysisState.Ready
        if (ready != null) {
            RiskIndexSummary(ready.analysis.risk)
            Spacer(Modifier.height(12.dp))
        }

        LiveReadings(state = analysis)

        Spacer(Modifier.height(14.dp))

        Text(
            text = buildString {
                append("Sources: ESA Sentinel-2 L2A and Sentinel-1 RTC (via Microsoft Planetary Computer), ")
                append("Open-Meteo weather models, Copernicus GLO-90 DEM, NASA Global Landslide Catalog. ")
                append("Satellite values reflect the latest usable acquisition — not a live feed. ")
                append("ALOS-4 PALSAR-3 is shown as unavailable because no public data service is configured.")
                ready?.let { append(" Analysed ${formatTime(it.analysis.analysedAtMillis)}.") }
            },
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun LocationNeeded(status: LocationStatus, onEnable: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgSurface)
            .border(1.dp, BgBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BrandContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LocationOff, null, tint = BrandPrimary)
        }
        Text(
            text = when (status) {
                LocationStatus.PENDING, LocationStatus.LOCATING -> "Finding your location…"
                LocationStatus.DENIED -> "Location permission is off"
                LocationStatus.UNAVAILABLE -> "No GPS fix available"
                LocationStatus.READY -> "Location ready"
            },
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Satellite analysis runs for your device's GPS position. No data is shown without it.",
            color = TextSecondary,
            fontSize = 12.sp
        )
        if (status == LocationStatus.DENIED || status == LocationStatus.UNAVAILABLE) {
            PanelButton("Enable location", primary = true, onClick = onEnable, modifier = Modifier.fillMaxWidth())
        }
    }
}
