package com.example.landguard.data.repository

import com.example.landguard.data.alerts.AlertSyncManager
import com.example.landguard.data.alerts.ReceiptSender
import com.example.landguard.data.local.AlertDao
import com.example.landguard.data.local.AlertEntity
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.domain.model.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AlertRepository {

    /** Fetches alerts missed while offline from the backend (same data as the authority console). */
    suspend fun refreshActiveAlerts(): Result<List<Alert>>

    fun observeHistory(): Flow<List<Alert>>

    suspend fun getAlertById(id: String): Alert?

    suspend fun saveAlert(alert: Alert)

    /** The user's own handling; acknowledgements are confirmed back to the authority. */
    suspend fun updateAlertStatus(
        alertId: String,
        newStatus: AlertStatus
    ): Result<Unit>
}

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    private val sync: AlertSyncManager,
    private val receipts: ReceiptSender
) : AlertRepository {

    override suspend fun refreshActiveAlerts(): Result<List<Alert>> =
        sync.syncAlerts().map { alertDao.activeWithExpiry().map { it.toDomain() } }

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
        val existing = alertDao.getById(alertId) ?: return Result.failure(NoSuchElementException(alertId))
        alertDao.updateStatus(alertId, newStatus.name)
        if (newStatus == AlertStatus.ACKNOWLEDGED && !existing.isDemoData) {
            receipts.send(alertId, "acknowledged", existing.receivedVia.ifBlank { "fcm" }, existing.hopCount)
        }
        return Result.success(Unit)
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
    isDemoData = isDemoData,
    expiresAt = expiresAt,
    serverStatus = serverStatus,
    source = source,
    origin = origin,
    hopCount = hopCount,
    receivedVia = receivedVia,
    latitude = latitude,
    longitude = longitude
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
    isDemoData = isDemoData,
    expiresAt = expiresAt,
    serverStatus = serverStatus,
    source = source,
    origin = origin,
    hopCount = hopCount,
    receivedVia = receivedVia,
    latitude = latitude,
    longitude = longitude
)
