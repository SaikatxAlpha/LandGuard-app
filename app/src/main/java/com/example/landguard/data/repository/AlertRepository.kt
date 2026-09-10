package com.example.landguard.data.repository

import com.example.landguard.data.network.LandGuardApiService
import com.example.landguard.data.network.StatusUpdateRequest
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
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
    private val apiService: LandGuardApiService
) : AlertRepository {

    private val alertsState = MutableStateFlow<List<Alert>>(emptyList())

    override suspend fun refreshActiveAlerts(): Result<List<Alert>> {
        return try {
            val alerts = apiService.getAlerts()
            alertsState.value = alerts
            Result.success(alerts)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override fun observeHistory(): Flow<List<Alert>> = alertsState.asStateFlow()

    override suspend fun updateAlertStatus(alertId: String, newStatus: AlertStatus): Result<Unit> {
        return try {
            apiService.updateAlertStatus(alertId, StatusUpdateRequest(newStatus.name))
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
