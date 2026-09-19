package com.example.landguard.domain.service

import android.content.Context
import android.util.Log
import com.example.landguard.BuildConfig
import com.example.landguard.data.network.DeviceRegisterRequest
import com.example.landguard.data.network.LandGuardApiService
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single path for registering this device's FCM token with the LandGuard
 * backend. The random installation id lets the backend replace a rotated token
 * and count delivery receipts per device without identifying the user.
 */
@Singleton
class FcmTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: LandGuardApiService
) {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val installationId: String
        get() = prefs.getString(KEY_INSTALLATION_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_INSTALLATION_ID, it).apply()
        }

    suspend fun registerCurrentToken(): Boolean = try {
        registerToken(FirebaseMessaging.getInstance().token.await())
    } catch (e: Exception) {
        Log.e(TAG, "Could not obtain FCM token", e)
        false
    }

    suspend fun registerToken(token: String): Boolean {
        if (token.isBlank()) return false
        return try {
            val response = apiService.registerDevice(
                DeviceRegisterRequest(token = token, platform = "android", installationId = installationId, appVersion = BuildConfig.VERSION_NAME)
            )
            response.body()?.close()
            if (response.isSuccessful) Log.i(TAG, "Device registered with LandGuard backend")
            else Log.e(TAG, "Device registration rejected: HTTP ${response.code()}")
            response.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Device registration failed (will retry on next sync): ${e.message}")
            false
        }
    }

    private companion object {
        const val TAG = "LandGuardFCM"
        const val PREFS = "LandGuardDevicePrefs"
        const val KEY_INSTALLATION_ID = "installation_id"
    }
}
