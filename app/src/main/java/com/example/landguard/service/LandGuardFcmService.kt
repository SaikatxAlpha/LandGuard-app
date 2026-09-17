package com.example.landguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.landguard.MainActivity
import com.example.landguard.data.network.DeviceRegisterRequest
import com.example.landguard.data.network.LandGuardApiService
import com.example.landguard.data.repository.AlertRepository
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.Severity
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LandGuardFcmService : FirebaseMessagingService() {

    @Inject
    lateinit var apiService: LandGuardApiService

    @Inject
    lateinit var alertRepository: AlertRepository

    @Inject
    lateinit var meshManager: com.example.landguard.offline.NearbyMeshManager

    override fun onCreate() {
        super.onCreate()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let { token ->
                    if (token.isNotBlank()) {
                        registerToken(token)
                    }
                }
            } else {
                task.exception?.printStackTrace()
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        registerToken(token)
    }

    private fun registerToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sharedPrefs = getSharedPreferences("LandGuardNetworkPrefs", Context.MODE_PRIVATE)
                val baseUrl = sharedPrefs.getString("base_url", "UNKNOWN")
                
                Log.d("LandGuardFCM", "FCM token obtained")
                Log.d("LandGuardBackend", "POST /devices")
                Log.d("LandGuardBackend", "Configured URL: $baseUrl")
                
                val response = apiService.registerDevice(DeviceRegisterRequest(token, platform = "android"))
                
                Log.d("LandGuardBackend", "HTTP status = ${response.code()}")
                val bodyString = if (response.isSuccessful) {
                    response.body()?.string() ?: "empty"
                } else {
                    response.errorBody()?.string() ?: "empty error"
                }
                Log.d("LandGuardBackend", "response = $bodyString")
                
                if (response.isSuccessful) {
                    Log.d("LandGuardBackend", "FCM device registration SUCCESS")
                } else {
                    Log.d("LandGuardBackend", "FCM device registration FAILED")
                }
            } catch (e: Exception) {
                Log.e("LandGuardBackend", "FCM device registration FAILED: ${e.message}", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.i("LandGuardFCM", "MESSAGE RECEIVED")

        val data = remoteMessage.data

        // 5. Read these FCM data fields: alertId, zoneId, zoneName, level, deepLink
        val alertId = data["alertId"] ?: "alert_${System.currentTimeMillis()}"
        val zoneId = data["zoneId"] ?: "unknown_zone"
        val zoneName = data["zoneName"] ?: "Unknown Zone"
        val level = data["level"] ?: "NORMAL"
        val deepLink = data["deepLink"]

        Log.i("LandGuardFCM", "alertId=$alertId")
        Log.i("LandGuardFCM", "zoneId=$zoneId")
        Log.i("LandGuardFCM", "level=$level")

        val title = remoteMessage.notification?.title 
            ?: data["title"] 
            ?: "$level Alert: $zoneName"
            
        val body = remoteMessage.notification?.body 
            ?: data["body"] 
            ?: "A $level alert was reported."

        val severity = runCatching { Severity.valueOf(level.uppercase()) }
            .getOrDefault(if (level.equals("CRITICAL", true)) Severity.CRITICAL else Severity.MODERATE)

        val alert = Alert(
            id = alertId,
            title = title,
            description = body,
            severity = severity,
            affectedLocation = zoneName,
            parcelId = zoneId,
            isDemoData = false
        )

        CoroutineScope(Dispatchers.IO).launch {
            alertRepository.saveAlert(alert)
        }

        try {
            Log.i("LandGuardFCM", "Offline relay attempted")
            val currentEndpoints = meshManager.connectedEndpoints.value
            if (currentEndpoints.isNotEmpty()) {
                val offlineAlert = com.example.landguard.offline.OfflineAlert(
                    alertId = alertId,
                    zoneId = zoneId,
                    zoneName = zoneName,
                    level = level,
                    message = body,
                    timestamp = System.currentTimeMillis().toString(),
                    expiresAt = System.currentTimeMillis() + 86400000,
                    originDeviceId = Build.MODEL,
                    hopCount = 0
                )
                meshManager.sendOfflineAlert(offlineAlert)
                Log.i("LandGuardMesh", "Offline alert forwarded")
            } else {
                Log.i("LandGuardFCM", "Offline relay skipped \u2014 no nearby peers")
            }
        } catch (e: Exception) {
            Log.e("LandGuardFCM", "Error during offline relay", e)
        }

        showNotification(
            title = title,
            body = body,
            alertId = alertId,
            level = level,
            zoneId = zoneId,
            zoneName = zoneName,
            deepLink = deepLink
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        alertId: String,
        level: String,
        zoneId: String,
        zoneName: String,
        deepLink: String?
    ) {
        // 10. Handle notification permission correctly for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("LandGuardFCM", "POST_NOTIFICATIONS permission not granted. Cannot show notification.")
                return
            }
        }

        // 7. Create/use notification channel: landguard_alerts
        val channelId = "landguard_alerts"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 8. Critical alerts should use high notification priority.
        // 9. Normal alerts should use default/normal priority.
        val isCritical = level.equals("CRITICAL", ignoreCase = true) || level.equals("HIGH", ignoreCase = true)
        val priority = if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "LandGuard Alerts", 
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alertId", alertId)
            putExtra("zoneId", zoneId)
            putExtra("zoneName", zoneName)
            putExtra("level", level)
            if (deepLink != null) {
                putExtra("deepLink", deepLink)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            alertId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 6. Display a native Android notification when an FCM alert arrives.
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.example.landguard.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(alertId.hashCode(), notification)
        Log.i("LandGuardFCM", "notification created")
        Log.i("LandGuardFCM", "Notification displayed successfully")
    }
}
