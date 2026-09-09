package com.example.landguard.domain.model

enum class SatelliteSource(
    val displayName: String,
    val description: String,
    val wavelengthBand: String,
    val sensorType: String
) {
    ALOS4_PALSAR3(
        displayName = "ALOS-4 (PALSAR-3)",
        description = "JAXA L-band Synthetic Aperture Radar. Penetrates clouds & canopy to measure ground surface displacement.",
        wavelengthBand = "L-Band (23.6 cm wavelength)",
        sensorType = "SAR (Synthetic Aperture Radar)"
    ),
    SENTINEL2_MSI(
        displayName = "Sentinel-2 (MSI)",
        description = "ESA 10m Multispectral Optical imagery. Tracks vegetation indices (NDVI) and moisture (NDWI).",
        wavelengthBand = "13 Spectral Bands (VNIR - SWIR)",
        sensorType = "Multispectral Optical"
    ),
    HYBRID_FUSION(
        displayName = "ALOS-4 + Sentinel-2 Hybrid",
        description = "Fused view combining ALOS-4 InSAR surface displacement vectors with Sentinel-2 spectral vegetation loss.",
        wavelengthBand = "Combined Radar + Optical",
        sensorType = "Multi-Sensor Fusion"
    )
}

enum class SatelliteLayer(
    val title: String,
    val code: String,
    val unit: String
) {
    ALOS4_INSAR_DISPLACEMENT("ALOS-4 InSAR Ground Displacement", "ALOS4_DISP", "mm/year"),
    ALOS4_SAR_BACKSCATTER("ALOS-4 SAR Radar Backscatter (HH/HV)", "ALOS4_SAR", "dB"),
    SENTINEL2_TRUE_COLOR("Sentinel-2 Optical (True Color RGB)", "S2_RGB", "RGB"),
    SENTINEL2_NDVI("Sentinel-2 NDVI (Vegetation Health)", "S2_NDVI", "Index (-1 to 1)"),
    SENTINEL2_MOISTURE("Sentinel-2 NDWI (Soil Moisture Saturation)", "S2_NDWI", "Index (-1 to 1)"),
    TERRAIN_3D_DEM("ALOS World 3D (AW3D30) DEM Slope", "AW3D_DEM", "Meters / Degrees")
}

data class SatelliteScene(
    val id: String,
    val source: SatelliteSource,
    val layer: SatelliteLayer,
    val acquisitionDate: String,
    val orbitDirection: String, // Ascending / Descending
    val polarization: String, // e.g., "HH + HV" for ALOS-4, "N/A" for Optical
    val cloudCoverPercentage: Double, // 0.0 for ALOS-4 SAR
    val spatialResolutionMeters: Int, // 3m for ALOS-4, 10m for Sentinel-2
    val coverageRegion: String,
    val downloadUrl: String = ""
)

data class GroundDeformationPoint(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val displacementRateMmPerYear: Double, // Positive = uplift, Negative = subsidence / slope slip
    val radarBackscatterDb: Double,
    val ndviScore: Double,
    val soilMoisturePercentage: Int,
    val riskSeverity: Severity,
    val lastScanDate: String,
    val slopeAngleDegrees: Double
)

data class SatelliteOverpass(
    val satelliteName: String,
    val expectedTime: String,
    val orbitType: String,
    val sensorType: String,
    val status: String
)

data class SatelliteApiConfig(
    val copernicusClientId: String = "",
    val copernicusClientSecret: String = "",
    val sentinelHubApiKey: String = "",
    val jaxaGPortalApiKey: String = "",
    val isConfigured: Boolean = false,
    val selectedEndpoint: String = "Copernicus Data Space Ecosystem (CDSE)"
)
