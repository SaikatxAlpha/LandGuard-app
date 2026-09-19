package com.example.landguard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    /** alertId from the backend — the same on every channel. */
    @PrimaryKey
    val id: String,

    val title: String,
    val description: String,
    val severity: String,
    val affectedLocation: String,
    /** zoneId */
    val parcelId: String,
    val detectedEvent: String,
    /** ISO-8601 UTC issue time. */
    val timestamp: String,
    /** This device's handling: NEW | ACKNOWLEDGED | RESOLVED */
    val status: String,
    val confidencePercentage: Int,
    val sourceProvider: String,
    val isDemoData: Boolean = false,

    // ── Added in schema v2 (shared alert contract) ──
    @ColumnInfo(defaultValue = "NULL")
    val expiresAt: String? = null,
    @ColumnInfo(defaultValue = "active")
    val serverStatus: String = "active",
    @ColumnInfo(defaultValue = "")
    val source: String = "",
    @ColumnInfo(defaultValue = "")
    val origin: String = "",
    @ColumnInfo(defaultValue = "0")
    val hopCount: Int = 0,
    @ColumnInfo(defaultValue = "")
    val receivedVia: String = "",
    @ColumnInfo(defaultValue = "NULL")
    val latitude: Double? = null,
    @ColumnInfo(defaultValue = "NULL")
    val longitude: Double? = null
)
