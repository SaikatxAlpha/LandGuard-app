package com.example.landguard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey
    val id: String,

    val title: String,
    val description: String,
    val severity: String,
    val affectedLocation: String,
    val parcelId: String,
    val detectedEvent: String,
    val timestamp: String,
    val status: String,
    val confidencePercentage: Int,
    val sourceProvider: String,
    val isDemoData: Boolean = false
)
