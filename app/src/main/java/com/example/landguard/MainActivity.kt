package com.example.landguard

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.landguard.data.alerts.AlertSyncManager
import com.example.landguard.data.alerts.ReceiptSender
import com.example.landguard.data.repository.AlertRepository
import com.example.landguard.offline.MeshService
import com.example.landguard.offline.NearbyMeshManager
import com.example.landguard.service.AlertNotifier
import com.example.landguard.ui.app.LandGuardAppUI
import com.example.landguard.ui.brand.SystemBarIcons
import com.example.landguard.ui.startup.LandGuardStartup
import com.example.landguard.ui.theme.LandGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═════════════════════════════════════════════════════════════════════
// ACTIVITY
// ═════════════════════════════════════════════════════════════════════

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var alertRepository: AlertRepository
    @Inject lateinit var meshManager: NearbyMeshManager
    @Inject lateinit var alertSync: AlertSyncManager
    @Inject lateinit var receipts: ReceiptSender

    /**
     * True once the launch-time notification and offline-mesh prompts are
     * resolved (or not needed), so the location prompt is never launched on top of them.
     */
    private val startupPromptsSettled = mutableStateOf(false)

    /** Alert to open from a notification tap (cold start or while running); cleared once shown. */
    private val pendingAlertId = MutableStateFlow<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) android.util.Log.w("LandGuardFCM", "Notification permission denied by user")
        requestMeshPermissionsIfNeeded()
    }

    private val meshPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filterValues { !it }.keys
        if (denied.isEmpty()) {
            MeshService.ensureRunning(this)
        } else {
            // Without these the offline channel cannot run at all; the alert
            // pipeline keeps working over FCM and sync.
            android.util.Log.w(
                "LandGuardMesh",
                "Offline mesh disabled — permissions denied: $denied. " +
                    "Grant them in Settings › Apps › LandGuard › Permissions to relay alerts without internet."
            )
        }
        startupPromptsSettled.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Launch experience is dark (light icons); the main app switches to dark icons itself.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // A recreated activity (rotation, process restore) must not re-open the same alert.
        val openedFromAlert = savedInstanceState == null && handleAlertIntent(intent)

        setContent {
            LandGuardTheme {
                LandGuardStartup(
                    skipIntro = openedFromAlert,
                    onEnteredApp = { requestNotificationPermissionIfNeeded() }
                ) {
                    val notificationAlertId by pendingAlertId.collectAsStateWithLifecycle()
                    SystemBarIcons(darkIcons = true)
                    LandGuardAppUI(
                        notificationAlertId = notificationAlertId,
                        onNotificationHandled = { pendingAlertId.value = null },
                        canPromptForLocation = startupPromptsSettled.value
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAlertIntent(intent)
    }

    /**
     * Opened from an alert notification (ours, or one shown by the system from an FCM
     * payload — both carry the same alertId): show that alert and tell the authority it was seen.
     */
    private fun handleAlertIntent(intent: Intent?): Boolean {
        val alertId = intent?.getStringExtra(AlertNotifier.EXTRA_ALERT_ID)?.takeIf { it.isNotBlank() } ?: return false
        pendingAlertId.value = alertId
        lifecycleScope.launch {
            alertRepository.getAlertById(alertId)?.let {
                receipts.send(alertId, "opened", it.receivedVia.ifBlank { "fcm" }, it.hopCount)
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        // Starts the offline mesh as soon as its permissions are available, and
        // catches up on anything missed while the app was in the background.
        MeshService.ensureRunning(this)
        alertSync.requestSync()
    }

    private var notificationPromptRequested = false

    private fun requestNotificationPermissionIfNeeded() {
        if (notificationPromptRequested) return
        notificationPromptRequested = true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestMeshPermissionsIfNeeded()
        }
    }

    /**
     * Bluetooth, nearby-Wi-Fi and — below API 33 — fine location for the offline
     * alert mesh.
     *
     * Location used to be filtered out here on the assumption that the map would
     * ask for it. On API 24–30 it is the *only* permission Nearby needs, so the
     * filtered list came back empty, the prompt never appeared, and
     * startIfPermitted() then refused to start because the permission was still
     * missing — the mesh could never come up. Every permission Nearby actually
     * needs is requested here.
     */
    private fun requestMeshPermissionsIfNeeded() {
        val missing = meshManager.missingPermissions()
        if (missing.isEmpty()) {
            MeshService.ensureRunning(this)
            startupPromptsSettled.value = true
        } else {
            meshPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    // The mesh deliberately outlives this Activity: it is owned by MeshService so
    // alerts still relay with the app closed and the screen off.
}
