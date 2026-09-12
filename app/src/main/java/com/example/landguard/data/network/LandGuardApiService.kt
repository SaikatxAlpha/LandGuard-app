package com.example.landguard.data.network

import com.example.landguard.domain.model.Alert
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class StatusUpdateRequest(val status: String)
data class DeviceRegisterRequest(
    val token: String,
    val platform: String = "android"
)

interface LandGuardApiService {
    @GET("alerts")
    suspend fun getAlerts(): List<Alert>

    @GET("alerts/{id}")
    suspend fun getAlertById(@Path("id") id: String): Alert

    @PATCH("alerts/{id}/status")
    suspend fun updateAlertStatus(
        @Path("id") id: String,
        @Body body: StatusUpdateRequest
    )

    @POST("api/devices/register")
    suspend fun registerDevice(@Body body: DeviceRegisterRequest)
}
