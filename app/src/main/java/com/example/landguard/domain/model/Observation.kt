package com.example.landguard.domain.model

data class Observation(
    val id: String = "",
    val description: String,
    /** zoneId of the nearest monitored area, if any. */
    val zone: String = "",
    val zoneName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUri: String? = null,
    val createdAt: String = ""
)
