// app/src/main/java/com/example/landguard/data/repository/SatelliteRepository.kt

package com.example.landguard.data.repository

import com.example.landguard.domain.model.GroundDeformationPoint
import com.example.landguard.domain.model.SatelliteApiConfig
import com.example.landguard.domain.model.SatelliteLayer
import com.example.landguard.domain.model.SatelliteOverpass
import com.example.landguard.domain.model.SatelliteScene
import com.example.landguard.domain.model.SatelliteSource
import com.example.landguard.domain.model.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@Singleton
class SatelliteRepositoryImpl @Inject constructor() : SatelliteRepository {

    private val apiConfigFlow = MutableStateFlow(
        SatelliteApiConfig(
            copernicusClientId     = "cdse_demo_user_8829",
            copernicusClientSecret = "HIDDEN_IN_BACKEND",
            sentinelHubApiKey      = "HIDDEN_IN_BACKEND",
            jaxaGPortalApiKey      = "HIDDEN_IN_BACKEND",
            isConfigured           = true
        )
    )

    private val scenes = listOf(
        SatelliteScene(
            id                     = "alos4_palsar3_20260910",
            source                 = SatelliteSource.ALOS4_PALSAR3,
            layer                  = SatelliteLayer.ALOS4_INSAR_DISPLACEMENT,
            acquisitionDate        = "2026-09-10 14:22 UTC",
            orbitDirection         = "Descending (Path 114 / Frame 0680)",
            polarization           = "HH + HV Dual Polarimetric",
            cloudCoverPercentage   = 0.0,
            spatialResolutionMeters = 3,
            coverageRegion         = "Darjeeling–Kalimpong Himalayan Corridor"
        ),
        SatelliteScene(
            id                     = "sentinel2_msi_20260910",
            source                 = SatelliteSource.SENTINEL2_MSI,
            layer                  = SatelliteLayer.SENTINEL2_NDVI,
            acquisitionDate        = "2026-09-10 05:18 UTC",
            orbitDirection         = "Ascending (Tile T45RVP)",
            polarization           = "Optical VNIR + SWIR",
            cloudCoverPercentage   = 3.8,
            spatialResolutionMeters = 10,
            coverageRegion         = "North Bengal & Sikkim Foothills"
        )
    )

    private val deformationPoints = listOf(
        GroundDeformationPoint(
            id                       = "def_01",
            label                    = "Kalimpong Ridge – Sector 04",
            latitude                 = 27.0600,
            longitude                = 88.4700,
            displacementRateMmPerYear = -28.4,
            radarBackscatterDb       = -14.2,
            ndviScore                = 0.31,
            soilMoisturePercentage   = 84,
            riskSeverity             = Severity.HIGH,
            lastScanDate             = "2026-09-10 (ALOS-4 PALSAR-3)",
            slopeAngleDegrees        = 38.2
        ),
        GroundDeformationPoint(
            id                       = "def_02",
            label                    = "Observatory Hill – Darjeeling",
            latitude                 = 27.0410,
            longitude                = 88.2630,
            displacementRateMmPerYear = -16.8,
            radarBackscatterDb       = -11.8,
            ndviScore                = 0.48,
            soilMoisturePercentage   = 76,
            riskSeverity             = Severity.MODERATE,
            lastScanDate             = "2026-09-10 (ALOS-4 InSAR)",
            slopeAngleDegrees        = 33.0
        ),
        GroundDeformationPoint(
            id                       = "def_03",
            label                    = "Teesta River Valley Cut",
            latitude                 = 26.9820,
            longitude                = 88.4210,
            displacementRateMmPerYear = -34.5,
            radarBackscatterDb       = -16.8,
            ndviScore                = 0.22,
            soilMoisturePercentage   = 91,
            riskSeverity             = Severity.CRITICAL,
            lastScanDate             = "2026-09-10 (ALOS-4 + Sentinel-2)",
            slopeAngleDegrees        = 42.1
        ),
        GroundDeformationPoint(
            id                       = "def_04",
            label                    = "Siliguri Bypass Terrace",
            latitude                 = 26.7270,
            longitude                = 88.3950,
            displacementRateMmPerYear = -2.1,
            radarBackscatterDb       = -8.1,
            ndviScore                = 0.68,
            soilMoisturePercentage   = 52,
            riskSeverity             = Severity.LOW,
            lastScanDate             = "2026-09-10 (Sentinel-2 MSI)",
            slopeAngleDegrees        = 12.4
        ),
        GroundDeformationPoint(
            id                       = "def_05",
            label                    = "Gorubathan Valley Terrace",
            latitude                 = 26.8600,
            longitude                = 88.5100,
            displacementRateMmPerYear = -21.3,
            radarBackscatterDb       = -13.4,
            ndviScore                = 0.39,
            soilMoisturePercentage   = 79,
            riskSeverity             = Severity.HIGH,
            lastScanDate             = "2026-09-10 (ALOS-4 PALSAR-3)",
            slopeAngleDegrees        = 36.7
        )
    )

    private val overpasses = listOf(
        SatelliteOverpass(
            satelliteName = "ALOS-4 (JAXA PALSAR-3)",
            expectedTime  = "Today, 22:15 UTC  •  in 3h 42m",
            orbitType     = "Descending Pass (Path 114)",
            sensorType    = "L-band High-Res SAR (InSAR Active)",
            status        = "Targeting Teesta Valley"
        ),
        SatelliteOverpass(
            satelliteName = "Sentinel-2C (ESA MSI)",
            expectedTime  = "Tomorrow, 05:22 UTC  •  in 11h 09m",
            orbitType     = "Ascending Orbit (Tile T45RVP)",
            sensorType    = "10m Optical VNIR/SWIR",
            status        = "Scheduled"
        ),
        SatelliteOverpass(
            satelliteName = "ALOS-2 (PALSAR-2 Backup)",
            expectedTime  = "Sep 13, 11:40 UTC",
            orbitType     = "Ascending Pass",
            sensorType    = "L-band Wide Swath SAR",
            status        = "Queued"
        )
    )

    override fun observeScenes(): Flow<List<SatelliteScene>>               = MutableStateFlow(scenes).asStateFlow()
    override fun observeDeformationPoints(): Flow<List<GroundDeformationPoint>> = MutableStateFlow(deformationPoints).asStateFlow()
    override fun observeOverpasses(): Flow<List<SatelliteOverpass>>        = MutableStateFlow(overpasses).asStateFlow()
    override fun observeApiConfig(): Flow<SatelliteApiConfig>              = apiConfigFlow.asStateFlow()

    override suspend fun updateApiConfig(config: SatelliteApiConfig): Result<Unit> = try {
        apiConfigFlow.value = config.copy(isConfigured = true)
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun refreshSatelliteFeeds(): Result<Unit> = Result.success(Unit)
}