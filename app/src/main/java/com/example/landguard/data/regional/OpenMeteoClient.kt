package com.example.landguard.data.regional

import com.example.landguard.di.PublicDataClient
import com.google.gson.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Open-Meteo (no API key):
 *  • hourly precipitation (past 72 h + next 24 h) and topsoil moisture from
 *    Open-Meteo's forecast API (numerical weather models, updated hourly);
 *  • terrain elevation from the Copernicus GLO-90 DEM, used to derive slope.
 *
 * Multiple coordinates are batched into one request where the API allows it.
 */
@Singleton
class OpenMeteoClient @Inject constructor(
    @PublicDataClient private val http: OkHttpClient
) {
    private val forecastUrl = "https://api.open-meteo.com/v1/forecast"
    private val elevationUrl = "https://api.open-meteo.com/v1/elevation"

    private val rainfallSource = "Open-Meteo weather models (hourly)"

    private companion object {
        const val BATCH_PAUSE_MS = 1_500L
        const val RATE_LIMIT_WAIT_MS = 61_000L
    }
    private val demSource = "Copernicus GLO-90 DEM via Open-Meteo"

    /** Rainfall + soil moisture for up to ~50 points per call. */
    suspend fun rainfall(points: List<Pair<Double, Double>>): List<RainfallReading?> {
        if (points.isEmpty()) return emptyList()
        val out = mutableListOf<RainfallReading?>()
        points.chunked(50).forEachIndexed { i, chunk ->
            if (i > 0) delay(BATCH_PAUSE_MS)
            out += withRateLimitRetry { rainfallChunk(chunk) }
        }
        return out
    }

    private suspend fun rainfallChunk(points: List<Pair<Double, Double>>): List<RainfallReading?> {
        val url = forecastUrl.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", points.joinToString(",") { "%.4f".format(java.util.Locale.US, it.first) })
            .addQueryParameter("longitude", points.joinToString(",") { "%.4f".format(java.util.Locale.US, it.second) })
            .addQueryParameter("hourly", "precipitation,soil_moisture_0_to_1cm")
            .addQueryParameter("past_days", "3")
            .addQueryParameter("forecast_days", "2")
            .addQueryParameter("timeformat", "unixtime")
            .addQueryParameter("timezone", "GMT")
            .build()
            .toString()

        val root = http.getJson(url)
        val objects: List<JsonObject> =
            if (root.isJsonArray) root.asJsonArray.map { it.asJsonObject } else listOf(root.asJsonObject)
        val now = System.currentTimeMillis()
        return objects.map { parseRainfall(it, now) }
    }

    private fun parseRainfall(obj: JsonObject, nowMillis: Long): RainfallReading? {
        val hourly = obj.getAsJsonObject("hourly") ?: return null
        val times = hourly.getAsJsonArray("time") ?: return null
        val precipitation = hourly.getAsJsonArray("precipitation") ?: return null
        val soil = hourly.getAsJsonArray("soil_moisture_0_to_1cm")

        var past72 = 0.0
        var next24 = 0.0
        var pastHours = 0
        var soilNow: Double? = null
        val nowSec = nowMillis / 1000
        for (i in 0 until times.size()) {
            val t = times[i].asLong
            val p = precipitation[i].takeUnless { it.isJsonNull }?.asDouble ?: continue
            when {
                t <= nowSec && t > nowSec - 72 * 3600 -> {
                    past72 += p
                    pastHours++
                }
                t > nowSec && t <= nowSec + 24 * 3600 -> next24 += p
            }
            if (t <= nowSec && soil != null) {
                soil[i].takeUnless { it.isJsonNull }?.asDouble?.let { soilNow = it }
            }
        }
        if (pastHours < 48) return null // not enough observed hours to report a 72 h total honestly
        return RainfallReading(
            past72hMm = past72,
            next24hMm = next24,
            soilMoistureM3M3 = soilNow,
            source = rainfallSource,
            fetchedAtMillis = nowMillis
        )
    }

    /**
     * Elevation and slope for each point from a 5-point DEM stencil
     * (centre + N/S/E/W at [spacingM] metres, central differences).
     */
    suspend fun terrain(points: List<Pair<Double, Double>>, spacingM: Double = 150.0): List<TerrainReading?> {
        if (points.isEmpty()) return emptyList()
        val stencils = points.map { (lat, lng) ->
            val dLat = spacingM / 111_320.0
            val dLng = spacingM / (111_320.0 * cos(Math.toRadians(lat)))
            listOf(
                lat to lng,          // centre
                lat + dLat to lng,   // north
                lat - dLat to lng,   // south
                lat to lng + dLng,   // east
                lat to lng - dLng    // west
            )
        }
        val flat = stencils.flatten()
        val elevations = mutableListOf<Double?>()
        flat.chunked(100).forEachIndexed { i, chunk ->
            if (i > 0) delay(BATCH_PAUSE_MS)
            elevations += withRateLimitRetry { elevationChunk(chunk) }
        }
        return stencils.indices.map { i ->
            val z = elevations.subList(i * 5, i * 5 + 5)
            if (z.any { it == null }) return@map null
            val (c, n, so, e, w) = z.map { it!! }
            val dzdx = (e - w) / (2 * spacingM)
            val dzdy = (n - so) / (2 * spacingM)
            val slope = Math.toDegrees(atan(sqrt(dzdx * dzdx + dzdy * dzdy)))
            TerrainReading(elevationM = c, slopeDeg = slope, source = demSource)
        }
    }

    /** Open-Meteo counts each location as a call; on HTTP 429 wait out the minute once. */
    private suspend fun <T> withRateLimitRetry(block: suspend () -> T): T =
        try {
            block()
        } catch (e: HttpStatusException) {
            if (e.code != 429) throw e
            delay(RATE_LIMIT_WAIT_MS)
            block()
        }

    private suspend fun elevationChunk(points: List<Pair<Double, Double>>): List<Double?> {
        val url = elevationUrl.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", points.joinToString(",") { "%.5f".format(java.util.Locale.US, it.first) })
            .addQueryParameter("longitude", points.joinToString(",") { "%.5f".format(java.util.Locale.US, it.second) })
            .build()
            .toString()
        val arr = http.getJson(url).asJsonObject.getAsJsonArray("elevation")
            ?: return List(points.size) { null }
        return (0 until points.size).map { i ->
            arr.getOrNull(i)?.takeUnless { it.isJsonNull }?.asDouble?.takeIf { !it.isNaN() }
        }
    }

    private fun com.google.gson.JsonArray.getOrNull(i: Int) = if (i < size()) get(i) else null
}
