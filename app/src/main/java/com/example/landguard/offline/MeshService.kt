package com.example.landguard.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.landguard.MainActivity
import com.example.landguard.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps the offline alert mesh alive.
 *
 * Nearby Connections advertising and discovery only run while a process is
 * alive and not suspended. Bound to an Activity, the mesh stopped the moment
 * the screen went off or the user left the app — which is exactly the state a
 * phone is in during a disaster, so no peer was ever discovered and no alert
 * was ever relayed. A `connectedDevice` foreground service is the sanctioned
 * way to hold that radio work open, and START_STICKY brings it back after the
 * process is killed or the device restarts.
 *
 * It does not poll, wake-lock or schedule alarms: Nearby itself is the only
 * thing running, and it is idle until a peer appears.
 */
@AndroidEntryPoint
class MeshService : Service() {

    @Inject lateinit var mesh: NearbyMeshManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground must happen first and unconditionally: once
        // startForegroundService has been called, Android kills the process if
        // the notification does not appear within a few seconds — including on
        // a START_STICKY restart, where permissions may since have been revoked.
        startForegroundCompat()

        // Without the Nearby permissions every call would fail, so the service
        // stands down rather than idling in the notification shade.
        if (!mesh.hasAllPermissions()) {
            Log.w(TAG, "Mesh service stopping — permissions missing: ${mesh.missingPermissions()}")
            stopSelf()
            return START_NOT_STICKY
        }

        val started = mesh.startIfPermitted()
        Log.i(TAG, "Mesh service running (advertising+discovery requested=$started)")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mesh.stopAll()
        Log.i(TAG, "Mesh service destroyed")
    }

    private fun startForegroundCompat() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Offline alert mesh", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Keeps LandGuard able to receive and pass on alerts without internet"
                    setShowBadge(false)
                }
            )
        }

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Offline alert mesh active")
            .setContentText("LandGuard can receive and pass on alerts without internet.")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(open)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                0
            }
        )
    }

    companion object {
        private const val TAG = "LandGuardMesh"
        private const val CHANNEL_ID = "landguard_mesh"
        private const val NOTIFICATION_ID = 4711

        /**
         * Starts the mesh service if the permissions are in place. Safe to call
         * from anywhere and as often as you like — a repeat call just re-asserts
         * advertising and discovery.
         *
         * Android 12+ forbids starting a foreground service from the background
         * outside an exemption. The realistic callers are all exempt (a visible
         * Activity, a high-priority FCM message, BOOT_COMPLETED), but if a start
         * is ever refused the mesh is still driven in-process rather than lost.
         */
        fun ensureRunning(context: Context) {
            val mesh = MeshEntryPoint.resolve(context)
            if (!mesh.hasAllPermissions()) {
                Log.i(TAG, "Not starting mesh service — permissions missing: ${mesh.missingPermissions()}")
                return
            }
            val intent = Intent(context, MeshService::class.java)
            try {
                ContextCompat_startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Foreground service start refused (${e.javaClass.simpleName}); running mesh in-process", e)
                mesh.startIfPermitted()
            }
        }

        private fun ContextCompat_startForegroundService(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
