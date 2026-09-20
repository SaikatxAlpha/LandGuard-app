package com.example.landguard.data.alerts

import android.util.Log
import com.example.landguard.data.local.AlertDao
import com.example.landguard.data.local.AlertEntity
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.offline.NearbyMeshManager
import com.example.landguard.service.AlertNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** How an alert reached this device — the same words the backend records. */
enum class AlertChannel(val wire: String) { FCM("fcm"), SYNC("sync"), MESH("mesh") }

/**
 * The single entry point for alerts on the device, whatever the channel:
 *
 *  FCM ─┐
 *  sync ─┼─▶ validate ─▶ de-duplicate by alertId ─▶ Room ─▶ notify once
 *  mesh ─┘                                           └─▶ relay over Nearby (hopCount + 1, until expiry)
 *                                                    └─▶ delivery receipt to the backend
 *
 * Because every channel carries the same alertId, an alert that arrives by
 * FCM and again over the mesh is stored, shown and relayed exactly once.
 */
@Singleton
class AlertIngestor @Inject constructor(
    private val dao: AlertDao,
    private val notifier: AlertNotifier,
    private val mesh: NearbyMeshManager,
    private val receipts: ReceiptSender
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var started = false

    /** Wires the offline mesh into the pipeline. Called once from the Application. */
    fun start() {
        if (started) return
        started = true
        scope.launch { mesh.incomingAlerts.collect { ingest(it.alert, AlertChannel.MESH, it.fromEndpointId) } }
        scope.launch { mesh.peerConnected.collect { forwardActiveAlertsTo(it) } }
    }

    /** @return true when the alert was new on this device. */
    suspend fun ingest(alert: AlertDto, channel: AlertChannel, fromEndpointId: String? = null): Boolean {
        val now = System.currentTimeMillis()
        val isNew = mutex.withLock {
            val existing = dao.getById(alert.alertId)
            if (existing != null) {
                // Duplicate copy (e.g. FCM + mesh). Only a lifecycle change is applied.
                if (alert.status != "active" && existing.serverStatus != alert.status) {
                    dao.updateServerStatus(alert.alertId, alert.status)
                    if (alert.status == "cancelled") notifier.cancel(alert.alertId)
                }
                false
            } else {
                dao.upsert(alert.toEntity(channel))
                true
            }
        }
        if (!isNew) {
            Log.d(TAG, "Duplicate ${alert.alertId} via ${channel.wire} ignored")
            return false
        }
        Log.i(TAG, "Alert ${alert.alertId} (${alert.level}) stored via ${channel.wire}, hop ${alert.hopCount}")

        if (alert.status == "active" && !alert.isExpired(now)) {
            notifier.show(alert, channel.wire)
            relay(alert, fromEndpointId)
        }
        receipts.send(alert.alertId, "received", channel.wire, alert.hopCount)
        return true
    }

    /** Backend lifecycle change for an alert this device already holds (e.g. cancellation). */
    suspend fun applyStatus(alertId: String, status: String) {
        val existing = dao.getById(alertId) ?: return
        if (existing.serverStatus == status) return
        dao.updateServerStatus(alertId, status)
        if (status == "cancelled") notifier.cancel(alertId)
        Log.i(TAG, "Alert $alertId is now $status")
    }

    private fun relay(alert: AlertDto, excludeEndpointId: String?) {
        val next = alert.copy(hopCount = alert.hopCount + 1, originDeviceId = android.os.Build.MODEL)
        if (!next.isRelayable()) {
            Log.i(
                TAG,
                "Not relaying ${alert.alertId}: status=${next.status} expired=${next.isExpired()} " +
                    "hop=${next.hopCount}/${AlertContract.MAX_MESH_HOPS}"
            )
            return
        }
        // Peers connected right now get it immediately; a peer that connects
        // later picks it up through store-and-forward (forwardActiveAlertsTo).
        val peers = mesh.relay(next, excludeEndpointId)
        Log.i(TAG, "Relayed ${alert.alertId} to $peers peer(s) at hop ${next.hopCount}")
    }

    /** Store-and-forward: a newly connected peer receives every alert that is still valid. */
    private suspend fun forwardActiveAlertsTo(endpointId: String) {
        val now = System.currentTimeMillis()
        dao.activeWithExpiry()
            .map { it.toDto() }
            .map { it.copy(hopCount = it.hopCount + 1, originDeviceId = android.os.Build.MODEL) }
            .filter { it.isRelayable(now) }
            .forEach { mesh.sendTo(endpointId, it) }
    }

    private companion object {
        const val TAG = "LandGuardAlerts"
    }
}

fun AlertDto.toEntity(channel: AlertChannel) = AlertEntity(
    id = alertId,
    title = title,
    description = message,
    severity = severity.name,
    affectedLocation = zoneName,
    parcelId = zoneId,
    detectedEvent = "Authority alert",
    timestamp = timestamp,
    status = AlertStatus.NEW.name,
    confidencePercentage = 100,
    sourceProvider = if (source == "risk_engine") "LandGuard risk engine" else "LandGuard Authority",
    isDemoData = false,
    expiresAt = expiresAt,
    serverStatus = status,
    source = source,
    origin = origin,
    hopCount = hopCount,
    receivedVia = channel.wire,
    latitude = lat,
    longitude = lng
)

fun AlertEntity.toDto() = AlertDto(
    alertId = id,
    zoneId = parcelId,
    zoneName = affectedLocation,
    level = severity.lowercase(),
    message = description,
    timestamp = timestamp,
    expiresAt = expiresAt ?: timestamp,
    source = source.ifBlank { "authority" },
    status = serverStatus,
    origin = origin.ifBlank { "api" },
    hopCount = hopCount,
    lat = latitude,
    lng = longitude
)
