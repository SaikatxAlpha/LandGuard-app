package com.example.landguard

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.landguard.data.network.DeviceRegisterRequest
import com.example.landguard.data.network.LandGuardApiService
import com.example.landguard.ui.app.BackendSetupScreen
import com.example.landguard.ui.app.LandGuardAppUI
import com.example.landguard.ui.theme.LandGuardTheme
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ═════════════════════════════════════════════════════════════════════
// ACTIVITY
// ═════════════════════════════════════════════════════════════════════

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var apiService: LandGuardApiService

    @javax.inject.Inject
    lateinit var alertRepository: com.example.landguard.data.repository.AlertRepository

    @javax.inject.Inject
    lateinit var meshManager: com.example.landguard.offline.NearbyMeshManager

    /**
     * True once the launch-time notification prompt is resolved (or not needed),
     * so the location prompt is never launched on top of it.
     */
    private val startupPromptsSettled = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.util.Log.w("LandGuardFCM", "Notification permission denied by user")
        }
        startupPromptsSettled.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        android.util.Log.e(
            "LandGuardFCM",
            "MAIN ACTIVITY UPDATED BUILD IS RUNNING"
        )
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startupPromptsSettled.value = true
            }
        } else {
            startupPromptsSettled.value = true
        }

        // ---------------------------------------------------------
        // NEARBY CONNECTIONS OFFLINE MESH (PHASE B)
        // ---------------------------------------------------------
        meshManager.startAdvertising()
        meshManager.startDiscovery()

        lifecycleScope.launch(Dispatchers.IO) {
            var isFirstEmission = true
            var lastBroadcastedAlertId: String? = null
            alertRepository.observeHistory().collect { alerts ->
                if (isFirstEmission) {
                    isFirstEmission = false
                    lastBroadcastedAlertId = alerts.firstOrNull()?.id
                    return@collect // skip initial history
                }
                
                alerts.firstOrNull()?.let { newestAlert ->
                    if (newestAlert.id != lastBroadcastedAlertId) {
                        lastBroadcastedAlertId = newestAlert.id
                        val offlineAlert = com.example.landguard.offline.OfflineAlert(
                            alertId = newestAlert.id,
                            zoneId = newestAlert.parcelId,
                            zoneName = newestAlert.affectedLocation,
                            level = newestAlert.severity.name,
                            message = newestAlert.description,
                            timestamp = newestAlert.timestamp,
                            expiresAt = System.currentTimeMillis() + 86400000,
                            originDeviceId = android.os.Build.MODEL,
                            hopCount = 0
                        )
                        meshManager.sendOfflineAlert(offlineAlert)
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // FCM DEVICE REGISTRATION
        // ---------------------------------------------------------
        // Registration is now handled after the backend URL is confirmed.

        // ---------------------------------------------------------
        // FCM NOTIFICATION DEEP LINK
        // ---------------------------------------------------------
        val alertId = intent.getStringExtra("alertId")

        val sharedPrefs = getSharedPreferences("LandGuardNetworkPrefs", android.content.Context.MODE_PRIVATE)

        setContent {
            LandGuardTheme {
                var isBackendConfigured by remember {
                    mutableStateOf(sharedPrefs.getString("base_url", "")?.isNotBlank() == true)
                }

                if (!isBackendConfigured) {
                    BackendSetupScreen(
                        onUrlSaved = { url ->
                            sharedPrefs.edit().putString("base_url", url).apply()
                            isBackendConfigured = true
                            registerFcmDeviceToken()
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        registerFcmDeviceToken()
                    }
                    LandGuardAppUI(
                        notificationAlertId = alertId,
                        canPromptForLocation = startupPromptsSettled.value,
                        onChangeBackendRequest = {
                            isBackendConfigured = false
                        }
                    )
                }
            }
        }
    }

    private fun registerFcmDeviceToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    android.util.Log.e("LandGuardFCM", "Failed to get FCM token", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                if (token.isNullOrBlank()) {
                    android.util.Log.e("LandGuardFCM", "FCM token is empty")
                    return@addOnCompleteListener
                }

                val sharedPrefs = getSharedPreferences("LandGuardNetworkPrefs", android.content.Context.MODE_PRIVATE)
                val baseUrl = sharedPrefs.getString("base_url", "UNKNOWN")

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        android.util.Log.d("LandGuardFCM", "FCM token obtained")
                        android.util.Log.d("LandGuardBackend", "POST /devices")
                        android.util.Log.d("LandGuardBackend", "Configured URL: $baseUrl")
                        
                        val response = apiService.registerDevice(DeviceRegisterRequest(token, platform = "android"))
                        
                        android.util.Log.d("LandGuardBackend", "HTTP status = ${response.code()}")
                        val bodyString = if (response.isSuccessful) {
                            response.body()?.string() ?: "empty"
                        } else {
                            response.errorBody()?.string() ?: "empty error"
                        }
                        android.util.Log.d("LandGuardBackend", "response = $bodyString")
                        
                        if (response.isSuccessful) {
                            android.util.Log.d("LandGuardBackend", "FCM device registration SUCCESS")
                        } else {
                            android.util.Log.d("LandGuardBackend", "FCM device registration FAILED")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("LandGuardBackend", "FCM device registration FAILED: ${e.message}", e)
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::meshManager.isInitialized) {
            meshManager.stopAll()
        }
    }
}
