package com.example.landguard.domain.model

data class LandParcel(
    val id: String,
    val name: String,
    val villageOrDistrict: String,
    val stateName: String,
    val latitude: Double,
    val longitude: Double,
    val areaHectares: Double,
    val landType: String, // "Agricultural", "Forest", "Urban/Built-up", "Water", "Other"
    val riskScore: Int, // 0..100
    val riskCategory: Severity,
    val lastUpdated: String,
    val isDemoData: Boolean = true
)
