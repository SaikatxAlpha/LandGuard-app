package com.example.landguard.domain.model

data class Alert(
    val id: String,
    val title: String,
    val description: String = "",
    val severity: Severity,
    val zone: String = "",
    val createdAt: String = ""
)

enum class Severity {
    LOW, MODERATE, HIGH, CRITICAL
}