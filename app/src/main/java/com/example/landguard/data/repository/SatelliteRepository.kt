// app/src/main/java/com/example/landguard/data/repository/SatelliteRepository.kt

package com.example.landguard.data.repository

import com.example.landguard.data.regional.DataResult
import com.example.landguard.data.regional.RegionalMonitoringRepository
import com.example.landguard.domain.model.GroundDeformationPoint
import com.example.landguard.domain.model.SatelliteApiConfig
import com.example.landguard.domain.model.SatelliteOverpass
import com.example.landguard.domain.model.SatelliteScene
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface SatelliteRepository {
    fun observeScenes(): Flow<List<SatelliteScene>>
    fun observeDeformationPoints(): Flow<List<GroundDeformationPoint>>
    fun observeOverpasses(): Flow<List<SatelliteOverpass>>
    fun observeApiConfig(): Flow<SatelliteApiConfig>
    suspend fun updateApiConfig(config: SatelliteApiConfig): Result<Unit>
    suspend fun refreshSatelliteFeeds(): Result<Unit>
}

/**
 * Monitoring points for the analysis map, derived from the regional
 * monitoring repository (NASA landslide catalog clusters across Northeast
 * India + live Open-Meteo rainfall / Copernicus DEM slope).
 *
 * Values that have no real source are NaN rather than invented:
 *  • displacementRateMmPerYear — no InSAR/ALOS-4 service is available;
 *  • ndviScore / radarBackscatterDb — filled in on demand from real
 *    Sentinel-2 / Sentinel-1 scenes when a point is selected (MapViewModel).
 */
@Singleton
class SatelliteRepositoryImpl @Inject constructor(
    private val regional: RegionalMonitoringRepository
) : SatelliteRepository {

    // No credentials are bundled; the dialog starts empty until the user configures one.
    private val apiConfigFlow = MutableStateFlow(SatelliteApiConfig())

    override fun observeDeformationPoints(): Flow<List<GroundDeformationPoint>> =
        combine(regional.catalog, regional.hotspotConditions) { catalog, conditions ->
            val snapshot = (catalog as? DataResult.Available)?.value ?: return@combine emptyList()
            snapshot.hotspots
                .map { hotspot -> hotspot to regional.hotspotRisk(hotspot, conditions[hotspot.id]) }
                .sortedByDescending { (_, risk) -> risk.score }
                .map { (hotspot, risk) ->
                    val c = conditions[hotspot.id]
                    GroundDeformationPoint(
                        id = hotspot.id,
                        label = "${hotspot.name}, ${hotspot.state}",
                        latitude = hotspot.latitude,
                        longitude = hotspot.longitude,
                        displacementRateMmPerYear = Double.NaN,
                        radarBackscatterDb = Double.NaN,
                        ndviScore = Double.NaN,
                        // -1 = unavailable (the model field is a non-null Int)
                        soilMoisturePercentage = c?.rainfall?.soilMoistureM3M3?.let { (it * 100).toInt() } ?: -1,
                        riskSeverity = risk.severity,
                        lastScanDate = buildString {
                            append("${hotspot.eventCount} recorded landslides (NASA GLC)")
                            c?.rainfall?.let {
                                append(" · rain 72 h: %.0f mm".format(Locale.US, it.past72hMm))
                            }
                            append(" · tap for Sentinel data")
                        },
                        slopeAngleDegrees = c?.terrain?.slopeDeg?.let { Math.round(it * 10) / 10.0 } ?: Double.NaN
                    )
                }
        }

    // Scene lists / overpass predictions are not fabricated: acquisitions are
    // looked up per location on demand, and pass predictions require orbit
    // propagation that this app does not perform.
    override fun observeScenes(): Flow<List<SatelliteScene>> = flowOf(emptyList())
    override fun observeOverpasses(): Flow<List<SatelliteOverpass>> = flowOf(emptyList())
    override fun observeApiConfig(): Flow<SatelliteApiConfig> = apiConfigFlow.asStateFlow()

    override suspend fun updateApiConfig(config: SatelliteApiConfig): Result<Unit> = try {
        apiConfigFlow.value = config.copy(isConfigured = true)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun refreshSatelliteFeeds(): Result<Unit> = runCatching {
        regional.refreshCatalog()
        regional.refreshConditions()
    }
}
