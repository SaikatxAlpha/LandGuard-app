package com.example.landguard.ui.monitor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.landguard.data.regional.DataResult
import com.example.landguard.data.regional.LocationAnalysis
import com.example.landguard.data.regional.RiskIndex
import com.example.landguard.data.regional.valueOrNull
import com.example.landguard.ui.components.SeverityPill
import com.example.landguard.ui.components.accent
import com.example.landguard.ui.risk.AnalysisState
import com.example.landguard.ui.risk.RiskArea
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.RiskModerate
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────
// Formatting
// ─────────────────────────────────────────────────────────────

fun formatDay(millis: Long): String = SimpleDateFormat("d MMM yyyy", Locale.US).format(Date(millis))

fun formatTime(millis: Long): String = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(millis))

/** Honest freshness label for an acquisition time — never "real-time". */
fun ageLabel(millis: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - millis).coerceAtLeast(0)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        hours < 1 -> "less than an hour ago"
        hours < 24 -> "$hours h ago"
        days < 60 -> "$days day${if (days == 1L) "" else "s"} ago"
        days < 730 -> "${days / 30} months ago"
        else -> "${days / 365} years ago"
    }
}

/** One-line summary of a monitored area using only values that exist. */
fun RiskArea.summaryLine(distance: String? = null): String = buildList {
    add("$eventCount recorded")
    rain72hMm?.let { add("%.0f mm rain/72 h".format(Locale.US, it)) }
    slopeDegrees?.let { add("%.0f° slope".format(Locale.US, it)) }
    if (activeAlertCount > 0) add("$activeAlertCount alert${if (activeAlertCount > 1) "s" else ""}")
    distance?.let { add(it) }
}.joinToString(" · ")

fun RiskArea.historyLine(): String = buildString {
    append(state)
    lastEventMillis?.let { append(" · last recorded ${formatDay(it)}") }
    if (fatalities > 0) append(" · $fatalities fatalities recorded")
}

// ─────────────────────────────────────────────────────────────
// Live readings card
// ─────────────────────────────────────────────────────────────

/**
 * Real readings for one location. [compact] shows the key lines only.
 */
@Composable
fun LiveReadings(
    state: AnalysisState?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
        contentKey = { it?.javaClass },
        label = "liveReadings",
        modifier = modifier
    ) { current ->
        when (current) {
            null, AnalysisState.Loading -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgElevated)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = BrandPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Fetching latest Sentinel-2, Sentinel-1, rainfall and terrain data…",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            is AnalysisState.Ready -> ReadingsList(current.analysis, compact)
        }
    }
}

@Composable
private fun ReadingsList(a: LocationAnalysis, compact: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgElevated)
            .border(1.dp, BgBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Reading(
            icon = Icons.Filled.SatelliteAlt,
            title = "Sentinel-2 vegetation (NDVI)",
            result = a.optical,
            value = { o ->
                buildString {
                    append("%.2f".format(Locale.US, o.ndvi))
                    o.ndviChange?.let { append("  (%+.2f vs. a year earlier)".format(Locale.US, it)) }
                        ?: append("  (no clear baseline scene)")
                }
            },
            meta = { o -> "Scene ${formatDay(o.acquiredMillis)} · ${ageLabel(o.acquiredMillis)} · ${(o.clearFraction * 100).toInt()}% cloud-free" }
        )
        Reading(
            icon = Icons.Filled.WaterDrop,
            title = "Rainfall",
            result = a.rainfall,
            value = { r -> "%.0f mm past 72 h · %.0f mm next 24 h".format(Locale.US, r.past72hMm, r.next24hMm) },
            meta = { r ->
                buildString {
                    r.soilMoistureM3M3?.let { append("Topsoil moisture %.0f%% · ".format(Locale.US, it * 100)) }
                    append("Open-Meteo models · fetched ${formatTime(r.fetchedAtMillis)}")
                }
            }
        )
        if (!compact) {
            Reading(
                icon = Icons.Filled.Radar,
                title = "Sentinel-1 SAR backscatter (VV)",
                result = a.sar,
                value = { s ->
                    buildString {
                        append("%.1f dB".format(Locale.US, s.vvDb))
                        s.vvChangeDb?.let { append("  (%+.1f dB vs. a year earlier)".format(Locale.US, it)) }
                    }
                },
                meta = { s -> "Pass ${formatDay(s.acquiredMillis)} · ${ageLabel(s.acquiredMillis)}" }
            )
            Reading(
                icon = Icons.Filled.Grain,
                title = "ALOS-4 PALSAR-3 (ground deformation)",
                result = a.alos4,
                value = { s -> "%.1f dB".format(Locale.US, s.vvDb) },
                meta = { s -> formatDay(s.acquiredMillis) }
            )
        }
        Reading(
            icon = Icons.Filled.Terrain,
            title = "Terrain",
            result = a.terrain,
            value = { t -> "%.0f° slope · %.0f m elevation".format(Locale.US, t.slopeDeg, t.elevationM) },
            meta = { _ -> "Copernicus GLO-90 DEM" }
        )
        Reading(
            icon = Icons.Filled.History,
            title = "Landslide record",
            result = a.history,
            value = { h -> "${h.eventsWithin10Km} recorded within 10 km" },
            meta = { h ->
                buildString {
                    h.lastEventMillis?.let { append("Last ${formatDay(it)} · ") }
                    h.nearestEventKm?.let { append("nearest %.1f km · ".format(Locale.US, it)) }
                    append("NASA Global Landslide Catalog (historical)")
                }
            }
        )
    }
}

@Composable
private fun <T> Reading(
    icon: ImageVector,
    title: String,
    result: DataResult<T>,
    value: (T) -> String,
    meta: (T) -> String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        val available = result is DataResult.Available
        Icon(
            imageVector = if (available) icon else Icons.Filled.CloudOff,
            contentDescription = null,
            tint = if (available) BrandPrimary else TextMuted,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            when (result) {
                is DataResult.Available -> {
                    Text(value(result.value), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(meta(result.value), color = TextMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                is DataResult.Unavailable -> {
                    Text("Data unavailable", color = RiskModerate, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(result.reason, color = TextMuted, fontSize = 10.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Risk index summary
// ─────────────────────────────────────────────────────────────

@Composable
fun RiskIndexSummary(result: DataResult<RiskIndex>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgElevated)
            .border(1.dp, BgBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        when (result) {
            is DataResult.Unavailable -> {
                Text("Risk index", color = TextSecondary, fontSize = 11.sp)
                Text("Data unavailable", color = RiskModerate, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(result.reason, color = TextMuted, fontSize = 11.sp)
            }

            is DataResult.Available -> {
                val risk = result.value
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LandGuard risk index", color = TextSecondary, fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${risk.score}", color = risk.severity.accent, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text(" / 100", color = TextMuted, fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            SeverityPill(risk.severity)
                        }
                    }
                    Text(
                        "${(risk.coverage * 100).toInt()}% of model\nbacked by data",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                risk.factors.forEach { f ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(f.name, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(f.detail, color = TextMuted, fontSize = 11.sp)
                        Spacer(Modifier.width(8.dp))
                        FactorBar(f.score, risk.severity.accent)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Heuristic index from the inputs above (weights: slope, rainfall, landslide record, vegetation and SAR change). " +
                            "Not an official warning.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun FactorBar(score: Int, color: Color) {
    Box(
        Modifier
            .width(46.dp)
            .height(5.dp)
            .clip(CircleShape)
            .background(BgBorder)
    ) {
        Box(
            Modifier
                .width(46.dp * (score / 100f))
                .height(5.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

fun LocationAnalysis.headline(): String = buildList {
    optical.valueOrNull()?.let { add("Sentinel-2 ${formatDay(it.acquiredMillis)}") } ?: add("Sentinel-2 unavailable")
    rainfall.valueOrNull()?.let { add("%.0f mm rain/72 h".format(Locale.US, it.past72hMm)) }
    terrain.valueOrNull()?.let { add("%.0f° slope".format(Locale.US, it.slopeDeg)) }
}.joinToString(" · ")
