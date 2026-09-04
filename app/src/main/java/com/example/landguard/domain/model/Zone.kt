package com.example.landguard.domain.model

data class Zone(
    val id: String,
    val name: String,
    val riskLevel: Severity,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastUpdated: String = ""
)
