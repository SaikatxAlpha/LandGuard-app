// app/src/main/java/com/example/landguard/ui/home/HomeViewModel.kt

package com.example.landguard.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.landguard.data.repository.AlertRepository
import com.example.landguard.data.repository.ForecastRepository
import com.example.landguard.data.repository.ParcelRepository
import com.example.landguard.data.repository.SatelliteRepository
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.ForecastDay
import com.example.landguard.domain.model.LandCoverBreakdown
import com.example.landguard.domain.model.LandParcel
import com.example.landguard.domain.model.SatelliteObservation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val parcels: List<LandParcel> = emptyList(),
    val alerts: List<Alert> = emptyList(),
    val forecastDays: List<ForecastDay> = emptyList(),
    val landCover: LandCoverBreakdown = LandCoverBreakdown(),
    val latestObservation: SatelliteObservation? = null,
    val nextSatellitePass: String = "",
    val totalMonitoredHa: Double = 0.0,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val parcelRepository: ParcelRepository,
    private val satelliteRepository: SatelliteRepository,
    private val forecastRepository: ForecastRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        loadForecast()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            combine(
                alertRepository.observeHistory(),
                parcelRepository.observeParcels(),
                parcelRepository.observeLandCoverBreakdown(),
                satelliteRepository.observeDeformationPoints()
            ) { alerts, parcels, cover, points ->
                // Real per-location satellite readings live in RegionalMonitoringRepository;
                // no synthetic "latest observation" is constructed here.
                val latestObs: SatelliteObservation? = null
                val totalHa = parcels.sumOf { it.areaHectares }
                HomeUiState(
                    isLoading           = false,
                    searchQuery         = _uiState.value.searchQuery,
                    parcels             = parcels,
                    alerts              = alerts,
                    forecastDays        = _uiState.value.forecastDays,
                    landCover           = cover,
                    latestObservation   = latestObs,
                    nextSatellitePass   = "",
                    totalMonitoredHa    = totalHa,
                    isOffline           = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun loadForecast() {
        viewModelScope.launch {
            forecastRepository.observeForecast().collect { forecast ->
                _uiState.value = _uiState.value.copy(forecastDays = forecast)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun refresh() {
        loadDashboardData()
        loadForecast()
    }
}