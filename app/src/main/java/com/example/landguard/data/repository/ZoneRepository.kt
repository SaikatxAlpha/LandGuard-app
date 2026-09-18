// app/src/main/java/com/example/landguard/data/repository/ZoneRepository.kt

package com.example.landguard.data.repository

import com.example.landguard.data.regional.CatalogSnapshot
import com.example.landguard.data.regional.DataResult
import com.example.landguard.data.regional.HotspotConditions
import com.example.landguard.data.regional.RegionalMonitoringRepository
import com.example.landguard.domain.model.Zone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface ZoneRepository {
    suspend fun refreshZones(): Result<List<Zone>>
    fun observeZones(): Flow<List<Zone>>
}

/**
 * Monitoring zones across Northeast India, derived from clusters of recorded
 * landslides (NASA Global Landslide Catalog) — no hand-picked locations.
 * Satellite-only metrics without a real source are NaN.
 */
@Singleton
class ZoneRepositoryImpl @Inject constructor(
    private val regional: RegionalMonitoringRepository
) : ZoneRepository {

    override suspend fun refreshZones(): Result<List<Zone>> = try {
        regional.refreshCatalog()
        regional.refreshConditions()
        when (val catalog = regional.catalog.value) {
            is DataResult.Available -> Result.success(toZones(catalog.value, regional.hotspotConditions.value))
            is DataResult.Unavailable -> Result.failure(IllegalStateException(catalog.reason))
            null -> Result.failure(IllegalStateException("Landslide catalog not loaded"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeZones(): Flow<List<Zone>> =
        combine(regional.catalog, regional.hotspotConditions) { catalog, conditions ->
            (catalog as? DataResult.Available)?.value?.let { toZones(it, conditions) } ?: emptyList()
        }

    private fun toZones(snapshot: CatalogSnapshot, conditions: Map<String, HotspotConditions>): List<Zone> {
        val format = SimpleDateFormat("d MMM yyyy", Locale.US)
        return snapshot.hotspots.map { h ->
            val c = conditions[h.id]
            Zone(
                id = h.id,
                name = "${h.name}, ${h.state}",
                riskLevel = regional.hotspotRisk(h, c).severity,
                latitude = h.latitude,
                longitude = h.longitude,
                lastUpdated = h.lastEventMillis?.let { "Last recorded landslide ${format.format(Date(it))}" }
                    ?: "No dated landslide record",
                alos4DisplacementMmPerYr = Double.NaN,
                alos4RadarBackscatterDb = Double.NaN,
                sentinel2Ndvi = Double.NaN,
                soilMoisturePercent = c?.rainfall?.soilMoistureM3M3?.let { (it * 100).toInt() } ?: -1,
                slopeDegrees = c?.terrain?.slopeDeg ?: Double.NaN,
                activeSensorsCount = 0
            )
        }
    }
}
