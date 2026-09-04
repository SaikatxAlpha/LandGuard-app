package com.example.landguard.domain.model

data class Observation(
    val id: String = "",
    val description: String,
    val zone: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUri: String? = null,
    val createdAt: String = ""
)
