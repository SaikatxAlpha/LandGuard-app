package com.example.landguard.domain.model

data class Zone(
    val id: String,
    val name: String,
    val riskLevel: Severity,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastUpdated: String = "",
    val alos4DisplacementMmPerYr: Double = -14.2, // ALOS-4 InSAR ground movement speed
    val alos4RadarBackscatterDb: Double = -11.5,  // ALOS-4 SAR HH/HV backscatter
    val sentinel2Ndvi: Double = 0.42,            // Sentinel-2 vegetation index (drop indicates landslide scar)
    val soilMoisturePercent: Int = 78,            // Sentinel-2 NDWI / soil moisture calculation
    val slopeDegrees: Double = 34.5,              // ALOS World 3D DEM slope steepness
    val activeSensorsCount: Int = 8
)
