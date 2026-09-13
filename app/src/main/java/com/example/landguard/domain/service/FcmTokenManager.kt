package com.example.landguard.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.example.landguard.data.network.DeviceRegisterRequest
import com.example.landguard.data.network.LandGuardApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    private val apiService: LandGuardApiService
) {

    suspend fun registerCurrentToken() {
        try {
            val token = withContext(Dispatchers.IO) {
                FirebaseMessaging.getInstance().token.await()
            }

            apiService.registerDevice(
                DeviceRegisterRequest(token)
            )

            Log.d("LandGuardFCM", "FCM token registered")
        } catch (e: Exception) {
            Log.e("LandGuardFCM", "Token registration failed", e)
        }
    }
}