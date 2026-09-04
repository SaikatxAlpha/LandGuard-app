package com.example.landguard.data.repository

import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.Severity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

interface AlertRepository {
    suspend fun refreshActiveAlerts(): Result<List<Alert>>
    fun observeHistory(): Flow<List<Alert>>
}

@Singleton
class AlertRepositoryImpl @Inject constructor() : AlertRepository {

    // TODO: replace with real backend / local Room cache once API contract is fixed.
    private val sampleAlerts = listOf(
        Alert(
            id = "1",
            title = "Heavy rainfall detected in Zone A",
            description = "Rainfall exceeded 80mm in the last 6 hours.",
            severity = Severity.HIGH,
            zone = "Zone A",
            createdAt = "2026-09-04 10:15"
        ),
        Alert(
            id = "2",
            title = "Soil moisture rising in Zone B",
            description = "Sensors show a steady increase over 24 hours.",
            severity = Severity.MODERATE,
            zone = "Zone B",
            createdAt = "2026-09-03 22:40"
        )
    )

    override suspend fun refreshActiveAlerts(): Result<List<Alert>> {
        return try {
            delay(400) // simulate network call
            Result.success(sampleAlerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeHistory(): Flow<List<Alert>> = flowOf(sampleAlerts)
}