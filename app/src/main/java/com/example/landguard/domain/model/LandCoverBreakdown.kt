package com.example.landguard.domain.model

/** Land-cover shares in percent. All zero = no land-cover data available. */
data class LandCoverBreakdown(
    val agriculturalPercentage: Int = 0,
    val forestPercentage: Int = 0,
    val urbanPercentage: Int = 0,
    val waterPercentage: Int = 0,
    val otherPercentage: Int = 0
)
