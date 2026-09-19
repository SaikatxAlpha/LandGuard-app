package com.example.landguard.service

import android.Manifest
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
import com.example.landguard.R
import com.example.landguard.data.alerts.AlertDto
import com.example.landguard.domain.model.Severity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The one place LandGuard alert notifications are built — FCM, sync and offline mesh alike. */
@Singleton
class AlertNotifier @Inject constructor(@ApplicationContext private val context: Context) {

    fun show(alert: AlertDto, via: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted; alert ${alert.alertId} is saved in the alert screen only")
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alertId", alert.alertId)
            putExtra("zoneId", alert.zoneId)
            putExtra("zoneName", alert.zoneName)
            putExtra("level", alert.level)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.alertId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val urgent = alert.severity >= Severity.HIGH
        val body = if (via == "mesh") "${alert.message}\n\nRelayed offline by a nearby LandGuard device." else alert.message

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .apply { alert.expiresAtMillis?.let { setTimeoutAfter((it - System.currentTimeMillis()).coerceAtLeast(60_000)) } }
            .build()

        manager.notify(alert.alertId.hashCode(), notification)
        Log.i(TAG, "Notification shown for ${alert.alertId} via $via")
    }

    fun cancel(alertId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(alertId.hashCode())
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "LandGuard Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Landslide alerts from LandGuard authorities, online and relayed offline"
                }
            )
        }
    }

    companion object {
        /** Same channel id the backend targets. */
        const val CHANNEL_ID = "landguard_alerts"
        private const val TAG = "LandGuardAlerts"
    }
}
