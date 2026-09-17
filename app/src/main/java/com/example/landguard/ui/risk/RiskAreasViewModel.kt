package com.example.landguard.ui.risk

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.landguard.data.repository.AlertRepository
import com.example.landguard.data.repository.SatelliteRepository
import com.example.landguard.data.repository.ZoneRepository
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.domain.model.GroundDeformationPoint
import com.example.landguard.domain.model.Severity
import com.example.landguard.domain.model.Zone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.abs

/**
 * A monitored area ranked for the "Risk Areas" view.
 * Built purely from existing repository data — no risk logic is changed.
 */
@Immutable
data class RiskArea(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val severity: Severity,
    val score: Int,
    val alertCount: Int,
    val activeAlertCount: Int,
    val displacementMmPerYr: Double,
    val soilMoisturePercent: Int,
    val slopeDegrees: Double,
    val ndvi: Double,
    val lastUpdated: String,
    val latestAlert: Alert?
)

@Immutable
data class RiskAreasUiState(
    val isLoading: Boolean = true,
    val areas: List<RiskArea> = emptyList(),
    val activeAlerts: Int = 0
) {
    fun countFor(severity: Severity) = areas.count { it.severity == severity }
}

@HiltViewModel
class RiskAreasViewModel @Inject constructor(
    zoneRepository: ZoneRepository,
    satelliteRepository: SatelliteRepository,
    alertRepository: AlertRepository
) : ViewModel() {

    private val zonesFlow = flow {
        val zones = zoneRepository.refreshZones().getOrNull()
            ?: zoneRepository.observeZones().first()
        emit(zones)
    }.catch { emit(emptyList()) }

    val uiState: StateFlow<RiskAreasUiState> = combine(
        zonesFlow,
        satelliteRepository.observeDeformationPoints().catch { emit(emptyList()) },
        alertRepository.observeHistory().catch { emit(emptyList()) }
    ) { zones, points, alerts ->
        val areas = buildAreas(zones, points, alerts)
        RiskAreasUiState(
            isLoading = false,
            areas = areas,
            activeAlerts = alerts.count { it.status != AlertStatus.RESOLVED }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RiskAreasUiState()
    )
}

// ─────────────────────────────────────────────────────────────
// Ranking
// ─────────────────────────────────────────────────────────────

private val GENERIC_NAME_WORDS = setOf(
    "valley", "slope", "sector", "river", "ridge", "corridor", "gorge",
    "terrace", "foothills", "observatory", "bypass", "hill", "zone", "area", "cut"
)

private fun buildAreas(
    zones: List<Zone>,
    points: List<GroundDeformationPoint>,
    alerts: List<Alert>
): List<RiskArea> {
    val fromZones = zones.map { zone ->
        areaOf(
            id = zone.id,
            name = zone.name,
            lat = zone.latitude,
            lng = zone.longitude,
            severity = zone.riskLevel,
            displacement = zone.alos4DisplacementMmPerYr,
            moisture = zone.soilMoisturePercent,
            slope = zone.slopeDegrees,
            ndvi = zone.sentinel2Ndvi,
            updated = zone.lastUpdated,
            alerts = alerts
        )
    }

    // Include deformation points that are not already represented by a zone.
    val fromPoints = points
        .filter { point ->
            zones.none { abs(it.latitude - point.latitude) < 0.01 && abs(it.longitude - point.longitude) < 0.01 }
        }
        .map { point ->
            areaOf(
                id = point.id,
                name = point.label,
                lat = point.latitude,
                lng = point.longitude,
                severity = point.riskSeverity,
                displacement = point.displacementRateMmPerYear,
                moisture = point.soilMoisturePercentage,
                slope = point.slopeAngleDegrees,
                ndvi = point.ndviScore,
                updated = point.lastScanDate,
                alerts = alerts
            )
        }

    return (fromZones + fromPoints)
        .sortedWith(compareByDescending<RiskArea> { it.score }.thenByDescending { it.activeAlertCount })
}

private fun areaOf(
    id: String,
    name: String,
    lat: Double,
    lng: Double,
    severity: Severity,
    displacement: Double,
    moisture: Int,
    slope: Double,
    ndvi: Double,
    updated: String,
    alerts: List<Alert>
): RiskArea {
    val keywords = name
        .split(' ', '–', '-', ',', '(', ')')
        .map { it.trim().lowercase() }
        .filter { it.length >= 5 && it !in GENERIC_NAME_WORDS }

    val related = alerts.filter { alert ->
        val haystack = "${alert.affectedLocation} ${alert.title}".lowercase()
        alert.parcelId == id || keywords.any { haystack.contains(it) }
    }
    val active = related.count { it.status != AlertStatus.RESOLVED }

    val base = when (severity) {
        Severity.CRITICAL -> 70
        Severity.HIGH -> 52
        Severity.MODERATE -> 34
        Severity.LOW -> 14
    }
    val activity = minOf(18, active * 6)
    val movement = minOf(12.0, abs(displacement) * 0.3).toInt()

    return RiskArea(
        id = id,
        name = name,
        latitude = lat,
        longitude = lng,
        severity = severity,
        score = (base + activity + movement).coerceIn(0, 100),
        alertCount = related.size,
        activeAlertCount = active,
        displacementMmPerYr = displacement,
        soilMoisturePercent = moisture,
        slopeDegrees = slope,
        ndvi = ndvi,
        lastUpdated = updated,
        latestAlert = related.firstOrNull()
    )
}
