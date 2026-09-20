package com.example.landguard.ui.risk

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.landguard.data.regional.DataResult
import com.example.landguard.ui.components.MapControlButton
import com.example.landguard.ui.components.MetricTile
import com.example.landguard.ui.components.SeverityPill
import com.example.landguard.ui.components.accent
import com.example.landguard.ui.components.container
import com.example.landguard.ui.components.pressClickable
import com.example.landguard.ui.home.PanelButton
import com.example.landguard.ui.location.UserLocation
import com.example.landguard.ui.location.distanceKm
import com.example.landguard.ui.location.formatDistance
import com.example.landguard.ui.monitor.LiveReadings
import com.example.landguard.ui.monitor.RiskIndexSummary
import com.example.landguard.ui.monitor.formatTime
import com.example.landguard.ui.monitor.historyLine
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BrandContainer
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary
import java.util.Locale

private const val UNAVAILABLE = "—"

/** Nearest monitored area to a point, with its distance in km. */
fun nearestArea(areas: List<RiskArea>, lat: Double, lng: Double): Pair<RiskArea, Double>? =
    areas.map { it to RegionalDistance.km(lat, lng, it.latitude, it.longitude) }.minByOrNull { it.second }

/**
 * Everything LandGuard knows about one monitored area: the risk index and its
 * factors, the recorded landslide history, live rainfall / terrain and the
 * latest satellite readings (fetched on open). Missing values read "—" and
 * are explained as data unavailable — nothing is filled in.
 */
@Composable
fun AreaDetailContent(
    area: RiskArea,
    analysis: AnalysisState?,
    userLocation: UserLocation?,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onOpenAlert: (String) -> Unit,
    onOpenAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeverityPill(area.severity)
                    userLocation?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatDistance(distanceKm(it, area.latitude, area.longitude)) + " away",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = area.name,
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(area.historyLine(), color = TextMuted, fontSize = 11.sp, maxLines = 2)
                Text(
                    "%.4f, %.4f".format(Locale.US, area.latitude, area.longitude),
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            MapControlButton(
                icon = Icons.Filled.Close,
                contentDescription = "Close",
                onClick = onClose,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("${area.score}", "Risk score", Modifier.weight(1f), accent = area.severity.accent)
            MetricTile("${area.eventCount}", "Recorded slides", Modifier.weight(1f))
            MetricTile(area.rain72hMm?.let { "%.0f".format(Locale.US, it) } ?: UNAVAILABLE, "mm rain / 72 h", Modifier.weight(1f))
            MetricTile(area.slopeDegrees?.let { "%.0f°".format(Locale.US, it) } ?: UNAVAILABLE, "Slope", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile(area.rainNext24hMm?.let { "%.0f".format(Locale.US, it) } ?: UNAVAILABLE, "mm next 24 h", Modifier.weight(1f))
            MetricTile(area.soilMoisturePct?.let { "$it%" } ?: UNAVAILABLE, "Soil moisture", Modifier.weight(1f))
            MetricTile("${area.fatalities}", "Fatalities", Modifier.weight(1f))
            MetricTile(area.dominantTrigger?.replaceFirstChar { it.uppercase() } ?: UNAVAILABLE, "Main trigger", Modifier.weight(1f))
        }
        if (area.rain72hMm == null || area.slopeDegrees == null || area.soilMoisturePct == null || area.rainNext24hMm == null) {
            Text(
                "— = DATA UNAVAILABLE from the live sources right now",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        RiskIndexSummary(DataResult.Available(area.risk))

        Spacer(Modifier.height(12.dp))
        SectionHeader(
            title = "Latest satellite & live readings",
            analysis = analysis,
            onRefresh = onRefresh
        )
        Spacer(Modifier.height(6.dp))
        LiveReadings(state = analysis)

        area.latestAlert?.let { alert ->
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(alert.severity.container)
                    .pressClickable(pressedScale = 0.98f) { onOpenAlert(alert.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Warning, null, tint = alert.severity.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = alert.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelButton(
                text = primaryActionLabel,
                primary = true,
                onClick = onPrimaryAction,
                modifier = Modifier.weight(1f)
            )
            PanelButton(
                text = if (area.activeAlertCount > 0) "Alerts (${area.activeAlertCount})" else "Alerts",
                primary = false,
                onClick = onOpenAlerts,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Details for any point — the user's own GPS position or a searched place:
 * the full real-data analysis plus the nearest monitored area.
 */
@Composable
fun PlaceDetailContent(
    title: String,
    subtitle: String?,
    latitude: Double,
    longitude: Double,
    isUserLocation: Boolean,
    analysis: AnalysisState?,
    nearest: Pair<RiskArea, Double>?,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onOpenArea: (RiskArea) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isUserLocation) Icons.Filled.MyLocation else Icons.Filled.Place,
                    null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isUserLocation) "YOUR AREA" else "SEARCHED PLACE",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text("%.5f, %.5f".format(Locale.US, latitude, longitude), color = TextMuted, fontSize = 10.sp)
            }
            MapControlButton(
                icon = Icons.Filled.Close,
                contentDescription = "Close",
                onClick = onClose,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        when (analysis) {
            is AnalysisState.Ready -> RiskIndexSummary(analysis.analysis.risk)
            else -> Unit
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader(title = "Satellite, rainfall, terrain & history", analysis = analysis, onRefresh = onRefresh)
        Spacer(Modifier.height(6.dp))
        LiveReadings(state = analysis)

        Spacer(Modifier.height(12.dp))
        if (nearest == null) {
            Text(
                "Nearest monitored area: DATA UNAVAILABLE (monitored areas not loaded)",
                color = TextMuted,
                fontSize = 11.sp
            )
        } else {
            val (area, km) = nearest
            Text("Nearest monitored area", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgElevated)
                    .border(1.dp, BgBorder, RoundedCornerShape(14.dp))
                    .pressClickable(pressedScale = 0.98f) { onOpenArea(area) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(area.severity.accent)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(area.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${formatDistance(km)} away · ${area.state} · score ${area.score}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                SeverityPill(area.severity)
                Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, analysis: AnalysisState?, onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            (analysis as? AnalysisState.Ready)?.let {
                Text("Analysed ${formatTime(it.analysis.analysedAtMillis)}", color = TextMuted, fontSize = 10.sp)
            }
        }
        if (analysis is AnalysisState.Ready) {
            SmallIconAction(Icons.Filled.Refresh, "Refresh readings", onRefresh)
        }
    }
}

@Composable
private fun SmallIconAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = description,
        tint = BrandPrimary,
        modifier = Modifier
            .clip(CircleShape)
            .pressClickable(onClick = onClick)
            .padding(6.dp)
            .size(18.dp)
    )
}
