package com.example.landguard.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.landguard.data.regional.DataResult
import com.example.landguard.data.regional.LocationAnalysis
import com.example.landguard.data.regional.RegionalMonitoringRepository
import com.example.landguard.data.regional.valueOrNull
import com.example.landguard.data.repository.SatelliteRepository
import com.example.landguard.data.repository.ZoneRepository
import com.example.landguard.domain.model.GroundDeformationPoint
import com.example.landguard.domain.model.SatelliteApiConfig
import com.example.landguard.domain.model.SatelliteLayer
import com.example.landguard.domain.model.SatelliteOverpass
import com.example.landguard.domain.model.SatelliteScene
import com.example.landguard.domain.model.SatelliteSource
import com.example.landguard.domain.model.Zone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MapUiState(
    val isLoading: Boolean = true,
    val zones: List<Zone> = emptyList(),
    val deformationPoints: List<GroundDeformationPoint> = emptyList(),
    val scenes: List<SatelliteScene> = emptyList(),
    val overpasses: List<SatelliteOverpass> = emptyList(),
    val selectedSource: SatelliteSource = SatelliteSource.ALOS4_PALSAR3,
    val selectedLayer: SatelliteLayer = SatelliteLayer.ALOS4_INSAR_DISPLACEMENT,
    val opacity: Float = 0.85f,
    val selectedPoint: GroundDeformationPoint? = null,
    val selectedZone: Zone? = null,
    val isSplitViewEnabled: Boolean = false,
    val filterSeverity: String = "All",
    val apiConfig: SatelliteApiConfig = SatelliteApiConfig(),
    val showApiDialog: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
    private val satelliteRepository: SatelliteRepository,
    private val regional: RegionalMonitoringRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** Real satellite readings fetched for points the user selected. */
    private val enriched = mutableMapOf<String, LocationAnalysis>()
    private var enrichJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Load the monitored areas across Northeast India (real catalog + live conditions).
            launch {
                val zones = zoneRepository.refreshZones()
                _uiState.value = _uiState.value.copy(
                    zones = zones.getOrDefault(_uiState.value.zones),
                    errorMessage = zones.exceptionOrNull()?.message,
                    isLoading = regional.catalog.value == null
                )
            }

            combine(
                satelliteRepository.observeDeformationPoints(),
                satelliteRepository.observeScenes(),
                satelliteRepository.observeOverpasses(),
                satelliteRepository.observeApiConfig()
            ) { points, scenes, overpasses, config ->
                Quadruple(points, scenes, overpasses, config)
            }.collect { (points, scenes, overpasses, config) ->
                val merged = points.map { applyEnrichment(it) }
                val previousId = _uiState.value.selectedPoint?.id
                val selected = merged.firstOrNull { it.id == previousId } ?: merged.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = regional.catalog.value == null,
                    deformationPoints = merged,
                    scenes = scenes,
                    overpasses = overpasses,
                    apiConfig = config,
                    selectedPoint = selected,
                    errorMessage = (regional.catalog.value as? DataResult.Unavailable)?.reason
                )
                if (selected != null && selected.id != previousId) enrich(selected)
            }
        }
    }

    fun selectSource(source: SatelliteSource) {
        val defaultLayer = when (source) {
            SatelliteSource.ALOS4_PALSAR3 -> SatelliteLayer.ALOS4_INSAR_DISPLACEMENT
            SatelliteSource.SENTINEL2_MSI -> SatelliteLayer.SENTINEL2_NDVI
            SatelliteSource.HYBRID_FUSION -> SatelliteLayer.ALOS4_INSAR_DISPLACEMENT
        }
        _uiState.value = _uiState.value.copy(
            selectedSource = source,
            selectedLayer = defaultLayer
        )
    }

    fun selectLayer(layer: SatelliteLayer) {
        _uiState.value = _uiState.value.copy(selectedLayer = layer)
    }

    fun setOpacity(value: Float) {
        _uiState.value = _uiState.value.copy(opacity = value)
    }

    fun selectDeformationPoint(point: GroundDeformationPoint?) {
        _uiState.value = _uiState.value.copy(selectedPoint = point?.let { applyEnrichment(it) })
        point?.let { enrich(it) }
    }

    /** Fetch real Sentinel-2 / Sentinel-1 readings for the selected point. */
    private fun enrich(point: GroundDeformationPoint) {
        if (enriched.containsKey(point.id)) return
        enrichJob?.cancel()
        enrichJob = viewModelScope.launch {
            val analysis = regional.analyzeLocation(point.latitude, point.longitude)
            enriched[point.id] = analysis
            _uiState.value = _uiState.value.copy(
                deformationPoints = _uiState.value.deformationPoints.map { applyEnrichment(it) },
                selectedPoint = _uiState.value.selectedPoint?.let { applyEnrichment(it) }
            )
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
                optical?.let { "Sentinel-2 ${format.format(Date(it.acquiredMillis))}" } ?: "Sentinel-2: data unavailable",
                sar?.let { "Sentinel-1 ${format.format(Date(it.acquiredMillis))}" } ?: "Sentinel-1: data unavailable",
                "ALOS-4: data unavailable"
            ).joinToString(" · ")
        )
    }

    fun selectZone(zone: Zone?) {
        _uiState.value = _uiState.value.copy(selectedZone = zone)
    }

    fun toggleSplitView(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isSplitViewEnabled = enabled)
    }

    fun setFilter(severity: String) {
        _uiState.value = _uiState.value.copy(filterSeverity = severity)
    }

    fun setShowApiDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showApiDialog = show)
    }

    fun updateApiConfig(config: SatelliteApiConfig) {
        viewModelScope.launch {
            satelliteRepository.updateApiConfig(config)
            _uiState.value = _uiState.value.copy(apiConfig = config, showApiDialog = false)
        }
    }

    fun refresh() {
        enriched.clear()
        viewModelScope.launch {
            regional.refreshCatalog(force = true)
            regional.refreshConditions(force = true)
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
