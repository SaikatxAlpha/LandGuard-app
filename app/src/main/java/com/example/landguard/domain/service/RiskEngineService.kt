package com.example.landguard.domain.service

import com.example.landguard.domain.model.RiskAssessment
import com.example.landguard.domain.model.RiskFactor
import com.example.landguard.domain.model.Severity
import javax.inject.Inject
import javax.inject.Singleton

interface RiskEngineService {
    fun categorizeScore(score: Int): Severity
    fun computeRiskAssessment(
        displacementRateMmYr: Double,
        ndviScore: Double,
        soilMoisturePct: Int,
        slopeDegrees: Double
    ): RiskAssessment
}

@Singleton
class RiskEngineServiceImpl @Inject constructor() : RiskEngineService {

    override fun categorizeScore(score: Int): Severity {
        return when {
            score >= 75 -> Severity.CRITICAL
            score >= 50 -> Severity.HIGH
            score >= 25 -> Severity.MODERATE
            else -> Severity.LOW
        }
    }

    override fun computeRiskAssessment(
        displacementRateMmYr: Double,
        ndviScore: Double,
        soilMoisturePct: Int,
        slopeDegrees: Double
    ): RiskAssessment {
        // Calculate factor weights
        val displacementScore = (Math.abs(displacementRateMmYr) * 2.5).coerceIn(0.0, 100.0).toInt()
        val ndviStressScore = ((1.0 - ndviScore.coerceIn(0.0, 1.0)) * 100).toInt()
        val moistureScore = soilMoisturePct.coerceIn(0, 100)
        val slopeScore = ((slopeDegrees / 45.0) * 100).coerceIn(0.0, 100.0).toInt()

        // Weighted composite score
        val compositeScore = (
            displacementScore * 0.40 +
            moistureScore * 0.25 +
            slopeScore * 0.20 +
            ndviStressScore * 0.15
        ).toInt().coerceIn(0, 100)

        val factors = listOf(
            RiskFactor("InSAR Ground Displacement", displacementScore),
            RiskFactor("Soil Saturation (NDWI)", moistureScore),
            RiskFactor("Terrain Slope Geometry", slopeScore),
            RiskFactor("Vegetation Stress (NDVI)", ndviStressScore)
        )

        return RiskAssessment(
            score = compositeScore,
            category = categorizeScore(compositeScore),
            contributingFactors = factors,
            timestamp = "Just now",
            confidencePercentage = 92
        )
    }
}
