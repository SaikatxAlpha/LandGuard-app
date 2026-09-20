package com.example.landguard.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.landguard.data.network.NetworkMonitor
import com.example.landguard.data.regional.DataResult
import com.example.landguard.data.regional.LocationAnalysis
import com.example.landguard.data.regional.RegionalMonitoringRepository
import com.example.landguard.data.regional.valueOrNull
import com.example.landguard.data.repository.AlertRepository
import com.example.landguard.data.repository.SatelliteRepository
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.domain.model.GroundDeformationPoint
import com.example.landguard.domain.model.SatelliteLayer
import com.example.landguard.domain.model.SatelliteSource
import com.example.landguard.ui.risk.AnalysisState
import com.example.landguard.ui.risk.DataStatus
import com.example.landguard.ui.risk.monitoringStatus
import com.example.landguard.ui.search.PlaceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MapUiState(
    val isLoading: Boolean = true,
    val deformationPoints: List<GroundDeformationPoint> = emptyList(),
    val selectedSource: SatelliteSource = SatelliteSource.ALOS4_PALSAR3,
    val selectedLayer: SatelliteLayer = SatelliteLayer.ALOS4_INSAR_DISPLACEMENT,
    val opacity: Float = 0.85f,
    /** The tapped monitored area; null until the user selects one. */
    val selectedPoint: GroundDeformationPoint? = null,
    val selectedAnalysis: AnalysisState? = null,
    /** A searched place (or alert location) and its real-data analysis. */
    val place: PlaceResult? = null,
    val placeAnalysis: AnalysisState? = null,
    /** Alerts that carry coordinates, for the alert layer. */
    val alerts: List<Alert> = emptyList(),
    val dataStatus: DataStatus = monitoringStatus(true, null, false, null, true, null, null, null),
    val errorMessage: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val satelliteRepository: SatelliteRepository,
    private val regional: RegionalMonitoringRepository,
    private val network: NetworkMonitor,
    alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** Real satellite readings fetched for points the user selected. */
    private val enriched = mutableMapOf<String, LocationAnalysis>()
    private var enrichJob: Job? = null
    private var placeJob: Job? = null

    init {
        viewModelScope.launch {
            regional.refreshCatalog()
            regional.refreshConditions()
        }
        viewModelScope.launch {
            satelliteRepository.observeDeformationPoints().collect { points ->
                val merged = points.map { applyEnrichment(it) }
                _uiState.update { s ->
                    s.copy(
                        isLoading = regional.catalog.value == null,
                        deformationPoints = merged,
                        // Keep the user's selection in step with fresh data — never auto-select.
                        selectedPoint = s.selectedPoint?.let { sel -> merged.firstOrNull { it.id == sel.id } ?: sel },
                        errorMessage = (regional.catalog.value as? DataResult.Unavailable)?.reason
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(
                combine(regional.catalog, regional.conditionsUpdatedAtMillis, regional.conditionsSource, ::Triple),
                combine(network.online, regional.backendReachable, ::Pair)
            ) { (catalog, conditionsAt, conditionsSource), (online, backend) ->
                val snapshot = (catalog as? DataResult.Available)?.value
                monitoringStatus(
                    isLoading = catalog == null,
                    catalogError = (catalog as? DataResult.Unavailable)?.reason,
                    fromCache = snapshot?.fromCache == true,
                    fetchedAtMillis = snapshot?.fetchedAtMillis,
                    online = online,
                    backendReachable = backend,
                    conditionsAtMillis = conditionsAt,
                    conditionsSource = conditionsSource
                )
            }.collect { status ->
                _uiState.update { it.copy(dataStatus = status, isLoading = regional.catalog.value == null) }
            }
        }
        viewModelScope.launch {
            alertRepository.observeHistory()
                .catch { emit(emptyList()) }
                .collect { alerts ->
                    _uiState.update { s ->
                        s.copy(alerts = alerts.filter {
                            it.latitude != null && it.longitude != null && it.status != AlertStatus.RESOLVED &&
                                    it.serverStatus == "active"
                        })
                    }
                }
        }
    }

    fun selectSource(source: SatelliteSource) {
        val defaultLayer = when (source) {
            SatelliteSource.ALOS4_PALSAR3 -> SatelliteLayer.ALOS4_INSAR_DISPLACEMENT
            SatelliteSource.SENTINEL2_MSI -> SatelliteLayer.SENTINEL2_NDVI
            SatelliteSource.HYBRID_FUSION -> SatelliteLayer.ALOS4_INSAR_DISPLACEMENT
        }
        _uiState.update { it.copy(selectedSource = source, selectedLayer = defaultLayer) }
    }

    fun selectLayer(layer: SatelliteLayer) {
        _uiState.update { it.copy(selectedLayer = layer) }
    }

    fun setOpacity(value: Float) {
        _uiState.update { it.copy(opacity = value) }
    }

    fun selectDeformationPoint(point: GroundDeformationPoint?) {
        if (point == null) {
            enrichJob?.cancel()
            _uiState.update { it.copy(selectedPoint = null, selectedAnalysis = null) }
            return
        }
        _uiState.update {
            it.copy(
                selectedPoint = applyEnrichment(point),
                selectedAnalysis = enriched[point.id]?.let { a -> AnalysisState.Ready(a) } ?: AnalysisState.Loading,
                place = null,
                placeAnalysis = null
            )
        }
        enrich(point, force = false)
    }

    fun selectPointById(id: String): GroundDeformationPoint? {
        val point = _uiState.value.deformationPoints.firstOrNull { it.id == id } ?: return null
        selectDeformationPoint(point)
        return point
    }

    fun refreshSelected() {
        _uiState.value.selectedPoint?.let { enrich(it, force = true) }
        _uiState.value.place?.let { analyzePlace(it, force = true) }
    }

    /** Fetch real Sentinel-2 / Sentinel-1 / rainfall / terrain readings for the selected point. */
    private fun enrich(point: GroundDeformationPoint, force: Boolean) {
        if (!force && enriched.containsKey(point.id)) return
        enrichJob?.cancel()
        if (force) _uiState.update { it.copy(selectedAnalysis = AnalysisState.Loading) }
        enrichJob = viewModelScope.launch {
            val analysis = regional.analyzeLocation(point.latitude, point.longitude, force)
            enriched[point.id] = analysis
            _uiState.update { s ->
                s.copy(
                    deformationPoints = s.deformationPoints.map { applyEnrichment(it) },
                    selectedPoint = s.selectedPoint?.let { applyEnrichment(it) },
                    selectedAnalysis = if (s.selectedPoint?.id == point.id) AnalysisState.Ready(analysis) else s.selectedAnalysis
                )
            }
        }
    }

    /** A searched place: analyse it with the same real sources as a monitored area. */
    fun showPlace(place: PlaceResult?) {
        placeJob?.cancel()
        if (place == null) {
            _uiState.update { it.copy(place = null, placeAnalysis = null) }
            return
        }
        enrichJob?.cancel()
        _uiState.update { it.copy(place = place, placeAnalysis = AnalysisState.Loading, selectedPoint = null, selectedAnalysis = null) }
        analyzePlace(place, force = false)
    }

    private fun analyzePlace(place: PlaceResult, force: Boolean) {
        placeJob?.cancel()
        if (force) _uiState.update { it.copy(placeAnalysis = AnalysisState.Loading) }
        placeJob = viewModelScope.launch {
            regional.refreshCatalog()
            val analysis = regional.analyzeLocation(place.latitude, place.longitude, force)
            _uiState.update { s -> if (s.place?.key == place.key) s.copy(placeAnalysis = AnalysisState.Ready(analysis)) else s }
        }
    }

    private fun applyEnrichment(point: GroundDeformationPoint): GroundDeformationPoint {
        val analysis = enriched[point.id] ?: return point
        val optical = analysis.optical.valueOrNull()
        val sar = analysis.sar.valueOrNull()
        val format = SimpleDateFormat("d MMM yyyy", Locale.US)
        return point.copy(
            ndviScore = optical?.ndvi?.let { Math.round(it * 100) / 100.0 } ?: Double.NaN,
            radarBackscatterDb = sar?.vvDb?.let { Math.round(it * 10) / 10.0 } ?: Double.NaN,
            slopeAngleDegrees = analysis.terrain.valueOrNull()?.slopeDeg?.let { Math.round(it * 10) / 10.0 }
                ?: point.slopeAngleDegrees,
            lastScanDate = listOf(
                optical?.let { "Sentinel-2 ${format.format(Date(it.acquiredMillis))}" } ?: "Sentinel-2: DATA UNAVAILABLE",
                sar?.let { "Sentinel-1 ${format.format(Date(it.acquiredMillis))}" } ?: "Sentinel-1: DATA UNAVAILABLE",
                "ALOS-4: DATA UNAVAILABLE"
            ).joinToString(" · ")
        )
    }

    fun refresh() {
        enriched.clear()
        viewModelScope.launch {
            regional.refreshCatalog(force = true)
            regional.refreshConditions(force = true)
        }
    }
}
