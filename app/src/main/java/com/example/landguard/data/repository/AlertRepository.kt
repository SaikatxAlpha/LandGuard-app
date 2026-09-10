// app/src/main/java/com/example/landguard/data/repository/AlertRepository.kt

package com.example.landguard.data.repository

import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.domain.model.Severity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface AlertRepository {
    suspend fun refreshActiveAlerts(): Result<List<Alert>>
    fun observeHistory(): Flow<List<Alert>>
    suspend fun updateAlertStatus(alertId: String, newStatus: AlertStatus): Result<Unit>
}

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val apiService: com.example.landguard.data.network.LandGuardApiService
) : AlertRepository {

    private val alertsState = MutableStateFlow<List<Alert>>(emptyList())

    override suspend fun refreshActiveAlerts(): Result<List<Alert>> {
        return try {
            val alerts = apiService.getAlerts()
            alertsState.value = alerts
            Result.success(alerts)
        } catch (e: Exception) {
            e.printStackTrace()
            // optionally use cached items
            Result.failure(e)
        }
    }

    override fun observeHistory(): Flow<List<Alert>> = alertsState.asStateFlow()

    override suspend fun updateAlertStatus(alertId: String, newStatus: AlertStatus): Result<Unit> {
        return try {
            apiService.updateAlertStatus(alertId, com.example.landguard.data.network.StatusUpdateRequest(newStatus.name))
            val updated = alertsState.value.map {
                if (it.id == alertId) it.copy(status = newStatus) else it
            }
            alertsState.value = updated
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}