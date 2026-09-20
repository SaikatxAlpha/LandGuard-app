// app/src/main/java/com/example/landguard/data/repository/ForecastRepository.kt

package com.example.landguard.data.repository

import com.example.landguard.domain.model.ForecastDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface ForecastRepository {
    fun observeForecast(): Flow<List<ForecastDay>>
}

/**
 * Daily risk forecast. No forecast model output is published by the backend,
 * so this is empty instead of a hard-coded week. Rainfall forecasts for real
 * locations come from Open-Meteo via RegionalMonitoringRepository.
 */
@Singleton
class ForecastRepositoryImpl @Inject constructor() : ForecastRepository {

    private val _forecastFlow = MutableStateFlow<List<ForecastDay>>(emptyList())

    override fun observeForecast(): Flow<List<ForecastDay>> = _forecastFlow.asStateFlow()
}
