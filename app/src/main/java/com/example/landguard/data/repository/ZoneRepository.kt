package com.example.landguard.data.repository

import com.example.landguard.domain.model.Severity
import com.example.landguard.domain.model.Zone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

interface ZoneRepository {
    suspend fun refreshZones(): Result<List<Zone>>
    fun observeZones(): Flow<List<Zone>>
}

@Singleton
class ZoneRepositoryImpl @Inject constructor() : ZoneRepository {

    private val sampleZones = listOf(
        Zone(
            id = "zone_a",
            name = "Kalimpong Slope - Sector 04",
            riskLevel = Severity.HIGH,
            latitude = 27.0600,
            longitude = 88.4700,
            lastUpdated = "Updated 8m ago (ALOS-4 PALSAR-3)",
            alos4DisplacementMmPerYr = -28.4,
            alos4RadarBackscatterDb = -14.2,
            sentinel2Ndvi = 0.31,
            soilMoisturePercent = 84,
            slopeDegrees = 38.2,
            activeSensorsCount = 12
        ),
        Zone(
            id = "zone_b",
            name = "Darjeeling Observatory Ridge",
            riskLevel = Severity.MODERATE,
            latitude = 27.0410,
            longitude = 88.2630,
            lastUpdated = "Updated 21m ago (ALOS-4 InSAR)",
            alos4DisplacementMmPerYr = -16.8,
            alos4RadarBackscatterDb = -11.8,
            sentinel2Ndvi = 0.48,
            soilMoisturePercent = 76,
            slopeDegrees = 33.0,
            activeSensorsCount = 9
        ),
        Zone(
            id = "zone_c",
            name = "Teesta River Gorge Corridor",
            riskLevel = Severity.CRITICAL,
            latitude = 26.9820,
            longitude = 88.4210,
            lastUpdated = "Updated 5m ago (ALOS-4 + Sentinel-2)",
            alos4DisplacementMmPerYr = -34.5,
            alos4RadarBackscatterDb = -16.8,
            sentinel2Ndvi = 0.22,
            soilMoisturePercent = 91,
            slopeDegrees = 42.1,
            activeSensorsCount = 15
        ),
        Zone(
            id = "zone_d",
            name = "Siliguri Foothills Valley",
            riskLevel = Severity.LOW,
            latitude = 26.7270,
            longitude = 88.3950,
            lastUpdated = "Updated 1h ago (Sentinel-2 Optical)",
            alos4DisplacementMmPerYr = -2.1,
            alos4RadarBackscatterDb = -8.1,
            sentinel2Ndvi = 0.68,
            soilMoisturePercent = 52,
            slopeDegrees = 12.4,
            activeSensorsCount = 6
        )
    )

    override suspend fun refreshZones(): Result<List<Zone>> {
        return try {
            delay(200)
            Result.success(sampleZones)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeZones(): Flow<List<Zone>> = flowOf(sampleZones)
}
