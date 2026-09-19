package com.example.landguard.data.network

import com.example.landguard.data.regional.LandslideEvent
import com.example.landguard.data.regional.RainfallReading
import com.example.landguard.data.regional.TerrainReading
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class DeviceRegisterRequest(
    val token: String,
    val platform: String = "android",
    val installationId: String? = null,
    val appVersion: String? = null
)

/** Device-side delivery confirmation; `via` is fcm | sync | mesh. */
data class ReceiptRequest(
    val installationId: String,
    val event: String,
    val via: String,
    val hopCount: Int = 0
)

// ─────────────────────────────────────────────────────────────
// Regional monitoring — the backend runs the same RegionalAnalytics model
// ─────────────────────────────────────────────────────────────

data class RiskFactorDto(val name: String?, val score: Int?, val weight: Double?, val detail: String?)

data class RiskDto(
    val score: Int?,
    val level: String?,
    val factors: List<RiskFactorDto>?,
    val coverage: Double?
)

data class MonitoringZoneDto(
    val id: String?,
    val risk: RiskDto?,
    val rainfall: RainfallReading?,
    val terrain: TerrainReading?
)

data class ConditionsDto(val state: String?, val observedAt: String?)

data class MonitoringZonesResponse(
    val generatedAt: String?,
    val conditions: ConditionsDto?,
    val zones: List<MonitoringZoneDto>?
)

data class CatalogResponse(
    val sourceUrl: String?,
    val fetchedAtMillis: Long?,
    val fromCache: Boolean?,
    val events: List<LandslideEvent>?
)

interface LandGuardApiService {
    /** Public alerts, optionally only those changed since [updatedSince] (ISO-8601). */
    @GET("alerts")
    suspend fun getAlerts(
        @Query("updatedSince") updatedSince: String? = null,
        @Query("limit") limit: Int = 200
    ): List<JsonObject>

    @GET("alerts/{id}")
    suspend fun getAlertById(@Path("id") id: String): JsonObject

    @POST("alerts/{id}/receipts")
    suspend fun postReceipt(@Path("id") id: String, @Body body: ReceiptRequest): Response<ResponseBody>

    @POST("devices")
    suspend fun registerDevice(@Body body: DeviceRegisterRequest): Response<ResponseBody>

    @GET("monitoring/catalog")
    suspend fun getMonitoringCatalog(): CatalogResponse

    @GET("monitoring/zones")
    suspend fun getMonitoringZones(): MonitoringZonesResponse

    @GET("monitoring/analysis")
    suspend fun getLocationAnalysis(@Query("lat") lat: Double, @Query("lng") lng: Double): JsonObject
}
