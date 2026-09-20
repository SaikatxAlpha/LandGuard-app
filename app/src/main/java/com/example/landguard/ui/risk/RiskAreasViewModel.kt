package com.example.landguard.ui.risk

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.landguard.data.network.NetworkMonitor
import com.example.landguard.data.regional.CatalogSnapshot
import com.example.landguard.data.regional.DataResult
import com.example.landguard.data.regional.HotspotConditions
import com.example.landguard.data.regional.LandslideHotspot
import com.example.landguard.data.regional.LocationAnalysis
import com.example.landguard.data.regional.NortheastRegion
import com.example.landguard.data.regional.RegionalMonitoringRepository
import com.example.landguard.data.regional.RiskIndex
import com.example.landguard.data.repository.AlertRepository
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.domain.model.Severity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A monitored area: a cluster of recorded landslides (NASA Global Landslide
 * Catalog) with live rainfall / slope context. All metrics are nullable —
 * null means the real value could not be obtained.
 */
@Immutable
data class RiskArea(
    val id: String,
    val name: String,
    val state: String,
    val latitude: Double,
    val longitude: Double,
    val severity: Severity,
    val score: Int,
    val risk: RiskIndex,
    val eventCount: Int,
    val fatalities: Int,
    val lastEventMillis: Long?,
    val dominantTrigger: String?,
    val rain72hMm: Double?,
    val rainNext24hMm: Double?,
    val soilMoisturePct: Int?,
    val slopeDegrees: Double?,
    val alertCount: Int,
    val activeAlertCount: Int,
    val latestAlert: Alert?
)

sealed interface AnalysisState {
    data object Loading : AnalysisState
    data class Ready(val analysis: LocationAnalysis) : AnalysisState
}

@Immutable
data class RiskAreasUiState(
    val isLoading: Boolean = true,
    val areas: List<RiskArea> = emptyList(),
    val activeAlerts: Int = 0,
    /** Set when the monitored-area catalog could not be loaded at all. */
    val catalogError: String? = null,
    val catalogEventCount: Int = 0,
    val catalogFirstYear: Int? = null,
    val catalogLastYear: Int? = null,
    val catalogFetchedAtMillis: Long? = null,
    val catalogFromCache: Boolean = false,
    val conditionsUpdatedAtMillis: Long? = null,
    val conditionsSource: String? = null,
    val statesCovered: List<Pair<String, Int>> = emptyList(),
    val online: Boolean = true,
    val backendReachable: Boolean? = null
) {
    fun countFor(severity: Severity) = areas.count { it.severity == severity }

    /** LIVE / CACHED / OFFLINE / DATA UNAVAILABLE with source and timestamp. */
    val dataStatus: DataStatus
        get() = monitoringStatus(
            isLoading = isLoading,
            catalogError = catalogError,
            fromCache = catalogFromCache,
            fetchedAtMillis = catalogFetchedAtMillis,
            online = online,
            backendReachable = backendReachable,
            conditionsAtMillis = conditionsUpdatedAtMillis,
            conditionsSource = conditionsSource
        )
}

private data class Connection(val online: Boolean, val backendReachable: Boolean?, val conditionsSource: String?)

@HiltViewModel
class RiskAreasViewModel @Inject constructor(
    private val regional: RegionalMonitoringRepository,
    alertRepository: AlertRepository,
    network: NetworkMonitor
) : ViewModel() {

    val uiState: StateFlow<RiskAreasUiState> = combine(
        combine(
            regional.catalog,
            regional.hotspotConditions,
            regional.conditionsUpdatedAtMillis,
            alertRepository.observeHistory().catch { emit(emptyList()) }
        ) { catalog, conditions, conditionsAt, alerts ->
            buildState(catalog, conditions, conditionsAt, alerts)
        },
        combine(network.online, regional.backendReachable, regional.conditionsSource, ::Connection)
    ) { state, connection ->
        state.copy(
            online = connection.online,
            backendReachable = connection.backendReachable,
            conditionsSource = connection.conditionsSource
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RiskAreasUiState()
    )

    private val _userAnalysis = MutableStateFlow<AnalysisState?>(null)
    val userAnalysis: StateFlow<AnalysisState?> = _userAnalysis.asStateFlow()

    private val _areaAnalysis = MutableStateFlow<Map<String, AnalysisState>>(emptyMap())
    val areaAnalysis: StateFlow<Map<String, AnalysisState>> = _areaAnalysis.asStateFlow()

    private var userJob: Job? = null

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            regional.refreshCatalog(force)
            regional.refreshConditions(force)
        }
    }

    /** Analyse the device location from real satellite / weather / terrain sources. */
    fun analyzeUserLocation(lat: Double, lng: Double, force: Boolean = false) {
        val current = (_userAnalysis.value as? AnalysisState.Ready)?.analysis
        if (!force && current != null &&
            RegionalDistance.km(current.latitude, current.longitude, lat, lng) < 1.0 &&
            System.currentTimeMillis() - current.analysedAtMillis < 30 * 60 * 1000L
        ) return
        userJob?.cancel()
        userJob = viewModelScope.launch {
            if (_userAnalysis.value == null || force) _userAnalysis.value = AnalysisState.Loading
            regional.refreshCatalog()
            _userAnalysis.value = AnalysisState.Ready(regional.analyzeLocation(lat, lng, force))
        }
    }

    fun analyzeArea(area: RiskArea, force: Boolean = false) = analyzePoint(area.id, area.latitude, area.longitude, force)

    /**
     * Real-data analysis of any point (monitored area, searched place, alert location),
     * cached per [key]. Results are shown via [areaAnalysis].
     */
    fun analyzePoint(key: String, lat: Double, lng: Double, force: Boolean = false) {
        val existing = _areaAnalysis.value[key]
        if (!force && existing != null) return
        if (existing == AnalysisState.Loading) return
        _areaAnalysis.value = _areaAnalysis.value + (key to AnalysisState.Loading)
        viewModelScope.launch {
            regional.refreshCatalog()
            val result = regional.analyzeLocation(lat, lng, force)
            _areaAnalysis.value = _areaAnalysis.value + (key to AnalysisState.Ready(result))
        }
    }

    private fun buildState(
        catalog: DataResult<CatalogSnapshot>?,
        conditions: Map<String, HotspotConditions>,
        conditionsAt: Long?,
        alerts: List<Alert>
    ): RiskAreasUiState {
        val activeAlerts = alerts.count { it.status != AlertStatus.RESOLVED }
        return when (catalog) {
            null -> RiskAreasUiState(isLoading = true, activeAlerts = activeAlerts)
            is DataResult.Unavailable -> RiskAreasUiState(
                isLoading = false,
                activeAlerts = activeAlerts,
                catalogError = catalog.reason
            )
            is DataResult.Available -> {
                val snapshot = catalog.value
                val areas = snapshot.hotspots
                    .map { toArea(it, conditions[it.id], alerts) }
                    .sortedWith(compareByDescending<RiskArea> { it.score }.thenByDescending { it.eventCount })
                val years = snapshot.events.mapNotNull { it.dateMillis }.map { yearOf(it) }
                RiskAreasUiState(
                    isLoading = false,
                    areas = areas,
                    activeAlerts = activeAlerts,
                    catalogEventCount = snapshot.events.size,
                    catalogFirstYear = years.minOrNull(),
                    catalogLastYear = years.maxOrNull(),
                    catalogFetchedAtMillis = snapshot.fetchedAtMillis,
                    catalogFromCache = snapshot.fromCache,
                    conditionsUpdatedAtMillis = conditionsAt,
                    statesCovered = NortheastRegion.STATES.map { state ->
                        state to snapshot.events.count { it.state == state }
                    }
                )
            }
        }
    }

    private fun toArea(h: LandslideHotspot, c: HotspotConditions?, alerts: List<Alert>): RiskArea {
        val risk = regional.hotspotRisk(h, c)
        val keywords = (listOf(h.name) + h.events.mapNotNull { it.nearestPlace })
            .flatMap { it.split(' ', ',', '-', '(', ')') }
            .map { it.trim().lowercase() }
            .filter { it.length >= 5 }
            .toSet()
        val related = alerts.filter { alert ->
            val haystack = "${alert.affectedLocation} ${alert.title}".lowercase()
            alert.parcelId == h.id || keywords.any { haystack.contains(it) }
        }
        return RiskArea(
            id = h.id,
            name = h.name,
            state = h.state,
            latitude = h.latitude,
            longitude = h.longitude,
            severity = risk.severity,
            score = risk.score,
            risk = risk,
            eventCount = h.eventCount,
            fatalities = h.fatalities,
            lastEventMillis = h.lastEventMillis,
            dominantTrigger = h.dominantTrigger,
            rain72hMm = c?.rainfall?.past72hMm,
            rainNext24hMm = c?.rainfall?.next24hMm,
            soilMoisturePct = c?.rainfall?.soilMoistureM3M3?.let { (it * 100).toInt() },
            slopeDegrees = c?.terrain?.slopeDeg,
            alertCount = related.size,
            activeAlertCount = related.count { it.status != AlertStatus.RESOLVED },
            latestAlert = related.firstOrNull()
        )
    }

    private fun yearOf(millis: Long): Int =
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }.get(java.util.Calendar.YEAR)
}

internal object RegionalDistance {
    fun km(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double =
        com.example.landguard.data.regional.RegionalAnalytics.distanceKm(lat1, lng1, lat2, lng2)
}
