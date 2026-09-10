// app/src/main/java/com/example/landguard/data/repository/ForecastRepository.kt   ← NEW FILE

package com.example.landguard.data.repository

import com.example.landguard.domain.model.ForecastDay
import com.example.landguard.domain.model.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface ForecastRepository {
    fun observeForecast(): Flow<List<ForecastDay>>
}

@Singleton
class ForecastRepositoryImpl @Inject constructor() : ForecastRepository {

    private val sevenDayForecast = listOf(
        ForecastDay("TODAY", "Sep 10", 88, Severity.CRITICAL, 96, "⛈", "Severe Storm",   18),
        ForecastDay("TUE",   "Sep 11", 74, Severity.HIGH,     88, "🌧", "Heavy Rain",     19),
        ForecastDay("WED",   "Sep 12", 62, Severity.HIGH,     74, "🌧", "Moderate Rain",  20),
        ForecastDay("THU",   "Sep 13", 47, Severity.MODERATE, 55, "⛅", "Partly Cloudy",  22),
        ForecastDay("FRI",   "Sep 14", 33, Severity.MODERATE, 30, "🌤", "Clearing",       23),
        ForecastDay("SAT",   "Sep 15", 18, Severity.LOW,      14, "🌤", "Partly Sunny",   24),
        ForecastDay("SUN",   "Sep 16", 11, Severity.LOW,       6, "☀", "Clear Skies",    25),
    )

    private val _forecastFlow = MutableStateFlow(sevenDayForecast)

    override fun observeForecast(): Flow<List<ForecastDay>> = _forecastFlow.asStateFlow()
}