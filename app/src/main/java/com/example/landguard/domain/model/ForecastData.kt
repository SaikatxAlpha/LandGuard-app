// app/src/main/java/com/example/landguard/domain/model/ForecastData.kt   ← NEW FILE

package com.example.landguard.domain.model

data class ForecastDay(
    val dayLabel: String,        // "TODAY", "TUE", "WED" …
    val date: String,            // "Sep 10"
    val riskScore: Int,          // 0–100
    val riskCategory: Severity,
    val rainChancePct: Int,      // 0–100
    val conditionEmoji: String,  // "⛈", "🌧", "⛅", "☀"
    val conditionLabel: String,  // "Severe Storm", "Partly Cloudy" …
    val tempCelsius: Int
)