package com.example.landguard.offline

import com.google.gson.Gson

data class OfflineAlert(
    val alertId: String,
    val zoneId: String,
    val zoneName: String,
    val level: String,
    val message: String,
    val timestamp: String,
    val expiresAt: Long,
    val originDeviceId: String,
    val hopCount: Int
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): OfflineAlert? {
            return try {
                Gson().fromJson(json, OfflineAlert::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
