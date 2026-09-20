package com.example.landguard.data.alerts

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import com.example.landguard.data.network.LandGuardApiService
import com.example.landguard.data.regional.DataResult
import com.example.landguard.data.regional.RegionalMonitoringRepository
import com.example.landguard.domain.service.FcmTokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the device in step with the backend without a manual refresh:
 *  • on app start and whenever the network comes back, alerts changed since
 *    the last sync are fetched (missed alerts, cancellations) and queued
 *    receipts are sent;
 *  • an FCM "sync" hint refreshes alerts or regional monitoring immediately.
 */
@Singleton
class AlertSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: LandGuardApiService,
    private val ingestor: AlertIngestor,
    private val receipts: ReceiptSender,
    private val tokens: FcmTokenManager,
    private val regional: RegionalMonitoringRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val prefs get() = context.getSharedPreferences("LandGuardSync", Context.MODE_PRIVATE)
    private var started = false

    fun start() {
        if (started) return
        started = true
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Fires immediately when already online, and again every time connectivity returns.
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i(TAG, "Network available — synchronising with LandGuard backend")
                requestSync(registerDevice = true)
                refreshMonitoringIfIncomplete()
            }
        })
    }

    /** After an offline start, fetch the monitored areas / live conditions that could not be loaded. */
    private fun refreshMonitoringIfIncomplete() {
        scope.launch {
            val catalogMissing = regional.catalog.value is DataResult.Unavailable
            val conditionsMissing = regional.conditionsSource.value == null
            if (!catalogMissing && !conditionsMissing) return@launch
            runCatching {
                regional.refreshCatalog()
                regional.refreshConditions()
            }.onFailure { Log.w(TAG, "Monitoring refresh after reconnect failed: ${it.message}") }
        }
    }

    fun requestSync(registerDevice: Boolean = false) {
        scope.launch {
            if (registerDevice) tokens.registerCurrentToken()
            syncAlerts()
        }
    }

    /** FCM "sync" hint from the backend. */
    fun onSyncHint(scopeName: String?) {
        scope.launch {
            when (scopeName) {
                "monitoring" -> runCatching { regional.refreshConditions(force = true) }
                    .onFailure { Log.w(TAG, "Monitoring refresh failed: ${it.message}") }
                else -> syncAlerts()
            }
        }
    }

    suspend fun syncAlerts(): Result<Int> = mutex.withLock {
        val startedAt = System.currentTimeMillis()
        // Overlap by 10 minutes so an alert updated during the previous sync is never missed.
        val since = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 }?.let { AlertContract.isoUtc(it - 10 * 60_000) }
        try {
            val alerts = api.getAlerts(updatedSince = since).mapNotNull { AlertContract.fromJson(it) }
            var added = 0
            for (alert in alerts) {
                if (ingestor.ingest(alert, AlertChannel.SYNC)) added++
                else if (alert.status != "active") ingestor.applyStatus(alert.alertId, alert.status)
            }
            prefs.edit().putLong(KEY_LAST_SYNC, startedAt).apply()
            receipts.flush()
            Log.i(TAG, "Alert sync complete: ${alerts.size} changed, $added new")
            Result.success(added)
        } catch (e: Exception) {
            Log.w(TAG, "Alert sync failed (offline?): ${e.message}")
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "LandGuardSync"
        const val KEY_LAST_SYNC = "last_alert_sync"
    }
}
