package com.example.landguard.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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
    private val satelliteRepository: SatelliteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            combine(
                satelliteRepository.observeDeformationPoints(),
                satelliteRepository.observeScenes(),
                satelliteRepository.observeOverpasses(),
                satelliteRepository.observeApiConfig()
            ) { points, scenes, overpasses, config ->
                Quadruple(points, scenes, overpasses, config)
            }.collect { (points, scenes, overpasses, config) ->
                zoneRepository.refreshZones()
                    .onSuccess { zones ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            zones = zones,
                            deformationPoints = points,
                            scenes = scenes,
                            overpasses = overpasses,
                            apiConfig = config,
                            selectedPoint = points.firstOrNull()
                        )
                    }
                    .onFailure { err ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = err.message
                        )
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
        _uiState.value = _uiState.value.copy(selectedPoint = point)
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
        loadData()
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
