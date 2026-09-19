package com.example.landguard.service

import android.util.Log
import com.example.landguard.data.alerts.AlertChannel
import com.example.landguard.data.alerts.AlertContract
import com.example.landguard.data.alerts.AlertIngestor
import com.example.landguard.data.alerts.AlertSyncManager
import com.example.landguard.domain.service.FcmTokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * The online alert channel. The backend sends data-only, high-priority FCM
 * messages, so this handler runs in the foreground AND background and every
 * alert goes through the same AlertIngestor as sync and the offline mesh.
 */
@AndroidEntryPoint
class LandGuardFcmService : FirebaseMessagingService() {

    @Inject lateinit var ingestor: AlertIngestor
    @Inject lateinit var sync: AlertSyncManager
    @Inject lateinit var tokens: FcmTokenManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { tokens.registerToken(token) }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        Log.i(TAG, "FCM message received: type=${data["type"] ?: "alert"} alertId=${data["alertId"]}")

        when (data["type"] ?: "alert") {
            "alert" -> {
                val alert = AlertContract.fromFcmData(data)
                if (alert == null) {
                    Log.w(TAG, "Ignoring FCM message that is not a valid LandGuard alert")
                    return
                }
                // Persist before returning: the process may be stopped right after this callback.
                runBlocking {
                    withTimeoutOrNull(8_000) { ingestor.ingest(alert, AlertChannel.FCM) }
                        ?: Log.w(TAG, "Timed out storing alert ${alert.alertId}; it will arrive again on the next sync")
                }
            }
            "alert_status" -> {
                val alertId = data["alertId"] ?: return
                val status = data["status"] ?: return
                runBlocking { withTimeoutOrNull(5_000) { ingestor.applyStatus(alertId, status) } }
            }
            "sync" -> sync.onSyncHint(data["scope"])
            else -> Log.w(TAG, "Unknown FCM message type ${data["type"]}")
        }
    }

    private companion object {
        const val TAG = "LandGuardFCM"
    }
}
