package com.example.landguard.domain.model

data class UserProfile(
    val phoneNumber: String = "",
    val selectedZone: String = "",
    val notificationsEnabled: Boolean = true,
    val language: String = "English"
)
