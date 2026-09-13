package com.example.landguard.data.repository

import com.example.landguard.data.local.AlertDao
import com.example.landguard.data.local.AlertEntity
import com.example.landguard.data.network.LandGuardApiService
import com.example.landguard.data.network.StatusUpdateRequest
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.domain.model.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AlertRepository {

    suspend fun refreshActiveAlerts(): Result<List<Alert>>

    fun observeHistory(): Flow<List<Alert>>

    suspend fun getAlertById(id: String): Alert?

    suspend fun saveAlert(alert: Alert)

    suspend fun updateAlertStatus(
        alertId: String,
        newStatus: AlertStatus
    ): Result<Unit>
}

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val apiService: LandGuardApiService,
    private val alertDao: AlertDao
) : AlertRepository {

    override suspend fun refreshActiveAlerts(): Result<List<Alert>> {
        return try {
            val alerts = apiService.getAlerts()

            alertDao.upsertAll(
                alerts.map { it.toEntity() }
            )

            Result.success(alerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeHistory(): Flow<List<Alert>> {
        return alertDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAlertById(id: String): Alert? {
        return alertDao.getById(id)?.toDomain()
    }

    override suspend fun saveAlert(alert: Alert) {
        alertDao.upsert(alert.toEntity())
    }

    override suspend fun updateAlertStatus(
        alertId: String,
        newStatus: AlertStatus
    ): Result<Unit> {
        return try {

            apiService.updateAlertStatus(
                alertId,
                StatusUpdateRequest(newStatus.name)
            )

            val existing = alertDao.getById(alertId)

            if (existing != null) {
                alertDao.upsert(
                    existing.copy(
                        status = newStatus.name
                    )
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun Alert.toEntity() = AlertEntity(
    id = id,
    title = title,
    description = description,
    severity = severity.name,
    affectedLocation = affectedLocation,
    parcelId = parcelId,
    detectedEvent = detectedEvent,
    timestamp = timestamp,
    status = status.name,
    confidencePercentage = confidencePercentage,
    sourceProvider = sourceProvider,
    isDemoData = isDemoData
)

private fun AlertEntity.toDomain() = Alert(
    id = id,
    title = title,
    description = description,
    severity = runCatching {
        Severity.valueOf(severity)
    }.getOrDefault(Severity.LOW),
    affectedLocation = affectedLocation,
    parcelId = parcelId,
    detectedEvent = detectedEvent,
    timestamp = timestamp,
    status = runCatching {
        AlertStatus.valueOf(status)
    }.getOrDefault(AlertStatus.NEW),
    confidencePercentage = confidencePercentage,
    sourceProvider = sourceProvider,
    isDemoData = isDemoData
)