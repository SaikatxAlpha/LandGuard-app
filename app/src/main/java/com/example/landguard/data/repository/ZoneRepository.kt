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

    // TODO: replace with real backend / geospatial risk model once API contract is fixed.
    private val sampleZones = listOf(
        Zone(
            id = "zone_a",
            name = "Zone A - Hillside Colony",
            riskLevel = Severity.HIGH,
            latitude = 22.3460,
            longitude = 87.2320,
            lastUpdated = "2026-09-05 08:00"
        ),
        Zone(
            id = "zone_b",
            name = "Zone B - River Valley",
            riskLevel = Severity.MODERATE,
            latitude = 22.3510,
            longitude = 87.2390,
            lastUpdated = "2026-09-05 07:30"
        ),
        Zone(
            id = "zone_c",
            name = "Zone C - Plateau Village",
            riskLevel = Severity.LOW,
            latitude = 22.3400,
            longitude = 87.2280,
            lastUpdated = "2026-09-05 07:00"
        )
    )

    override suspend fun refreshZones(): Result<List<Zone>> {
        return try {
            delay(300) // simulate network call
            Result.success(sampleZones)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeZones(): Flow<List<Zone>> = flowOf(sampleZones)
}
