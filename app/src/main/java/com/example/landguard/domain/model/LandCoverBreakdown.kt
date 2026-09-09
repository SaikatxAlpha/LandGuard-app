package com.example.landguard.domain.model

data class LandCoverBreakdown(
    val agriculturalPercentage: Int = 42,
    val forestPercentage: Int = 35,
    val urbanPercentage: Int = 12,
    val waterPercentage: Int = 8,
    val otherPercentage: Int = 3
)
