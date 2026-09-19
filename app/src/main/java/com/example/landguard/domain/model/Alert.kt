package com.example.landguard.domain.model

enum class Severity {
    LOW, MODERATE, HIGH, CRITICAL
}

/** The user's own handling of an alert on this device. */
enum class AlertStatus {
    NEW, ACKNOWLEDGED, RESOLVED
}

data class Alert(
    /** alertId — identical on the backend, FCM, this device and the offline mesh. */
    val id: String,
    val title: String,
    val description: String = "",
    val severity: Severity,
    val affectedLocation: String = "",
    /** zoneId of the monitored area. */
    val parcelId: String = "",
    val detectedEvent: String = "",
    /** ISO-8601 UTC issue time. */
    val timestamp: String = "",
    val status: AlertStatus = AlertStatus.NEW,
    val confidencePercentage: Int = 90,
    val sourceProvider: String = "LandGuard Engine",
    val isDemoData: Boolean = true,
    /** ISO-8601 UTC expiry. */
    val expiresAt: String? = null,
    /** Backend lifecycle: active | cancelled | expired. */
    val serverStatus: String = "active",
    /** authority | risk_engine */
    val source: String = "",
    /** authority_web | api */
    val origin: String = "",
    val hopCount: Int = 0,
    /** How this device received it: fcm | sync | mesh. */
    val receivedVia: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)
