package com.example.landguard.domain.model

data class SatelliteObservation(
    val id: String,
    val provider: String, // e.g. "JAXA ALOS-4 & ESA Copernicus Sentinel-2"
    val locationName: String,
    val observationDate: String,
    val detectedChange: String,
    val confidencePercentage: Int,
    val ndviIndex: Double = 0.31,
    val soilMoistureIndex: Double = 0.84,
    val groundShiftMmPerYr: Double = -28.4,
    val radarBackscatterDb: Double = -14.2,
    val slopeAngleDegrees: Double = 38.2,
    val orbitDetails: String = "Descending Path 114 / Frame 0680",
    val spatialResolution: String = "3m L-Band SAR + 10m Optical",
    val polarization: String = "HH + HV Dual Polarimetric",
    val cloudCoverPercentage: Double = 0.0,
    val recommendedAction: String = "Trigger drone topography scan & issue local slope advisories",
    val isDemoData: Boolean = true
)
