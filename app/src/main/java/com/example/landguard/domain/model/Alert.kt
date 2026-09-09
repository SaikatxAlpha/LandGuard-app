package com.example.landguard.domain.model

enum class Severity {
    LOW, MODERATE, HIGH, CRITICAL
}

enum class AlertStatus {
    NEW, ACKNOWLEDGED, RESOLVED
}

data class Alert(
    val id: String,
    val title: String,
    val description: String = "",
    val severity: Severity,
    val affectedLocation: String = "",
    val parcelId: String = "",
    val detectedEvent: String = "",
    val timestamp: String = "",
    val status: AlertStatus = AlertStatus.NEW,
    val confidencePercentage: Int = 90,
    val sourceProvider: String = "LandGuard Engine",
    val isDemoData: Boolean = true
)
