package com.example.landguard.domain.model

data class RiskFactor(
    val factorName: String,
    val impactPercentage: Int
)

data class RiskAssessment(
    val score: Int, // 0..100
    val category: Severity,
    val contributingFactors: List<RiskFactor>,
    val timestamp: String,
    val confidencePercentage: Int = 92
)
