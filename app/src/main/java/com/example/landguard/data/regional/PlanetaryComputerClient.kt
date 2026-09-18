package com.example.landguard.data.regional

import com.example.landguard.di.PublicDataClient
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Microsoft Planetary Computer — public STAC catalogue and data API.
 *
 *  • sentinel-2-l2a : ESA Sentinel-2 MSI surface reflectance (optical, 10 m)
 *  • sentinel-1-rtc : ESA Sentinel-1 C-band SAR, radiometrically terrain corrected
 *
 * Statistics are computed server-side over a small polygon, so the phone
 * never downloads imagery. No API key is required for these endpoints.
 */
@Singleton
class PlanetaryComputerClient @Inject constructor(
    @PublicDataClient private val http: OkHttpClient
) {
    private val stacSearch = "https://planetarycomputer.microsoft.com/api/stac/v1/search"
    private val itemStatistics = "https://planetarycomputer.microsoft.com/api/data/v1/item/statistics"

    data class StacItem(
        val id: String,
        val collection: String,
        val acquiredMillis: Long,
        val cloudCover: Double?,
        val orbitState: String?,
        val relativeOrbit: Int?
    )

    suspend fun search(
        collection: String,
        lat: Double,
        lng: Double,
        fromMillis: Long,
        toMillis: Long,
        limit: Int,
        maxCloudCover: Double? = null,
        orbitState: String? = null,
        relativeOrbit: Int? = null
    ): List<StacItem> {
        val query = JsonObject().apply {
            maxCloudCover?.let { add("eo:cloud_cover", JsonObject().apply { addProperty("lt", it) }) }
            orbitState?.let { add("sat:orbit_state", JsonObject().apply { addProperty("eq", it) }) }
            relativeOrbit?.let { add("sat:relative_orbit", JsonObject().apply { addProperty("eq", it) }) }
        }
        val body = JsonObject().apply {
            add("collections", JsonArray().apply { add(collection) })
            add("intersects", JsonObject().apply {
                addProperty("type", "Point")
                add("coordinates", JsonArray().apply { add(lng); add(lat) })
            })
            addProperty("datetime", "${isoUtc(fromMillis)}/${isoUtc(toMillis)}")
            addProperty("limit", limit)
            add("sortby", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("field", "datetime")
                    addProperty("direction", "desc")
                })
            })
            if (query.size() > 0) add("query", query)
        }

        val root = http.postJson(stacSearch, body).asJsonObject
        return root.getAsJsonArray("features")?.mapNotNull { element ->
            val feature = element.asJsonObject
            val props = feature.getAsJsonObject("properties") ?: return@mapNotNull null
            val datetime = props.str("datetime") ?: return@mapNotNull null
            StacItem(
                id = feature.str("id") ?: return@mapNotNull null,
                collection = collection,
                acquiredMillis = parseIsoUtc(datetime) ?: return@mapNotNull null,
                cloudCover = props.dbl("eo:cloud_cover"),
                orbitState = props.str("sat:orbit_state"),
                relativeOrbit = props.int("sat:relative_orbit")
            )
        }.orEmpty()
    }

    /**
     * Server-side zonal statistics of one or more band expressions
     * (separated by ';') over a square box centred on the point.
     * Returns expression → mean, in the same order as requested.
     */
    suspend fun meanOverBox(
        item: StacItem,
        lat: Double,
        lng: Double,
        halfSizeDeg: Double,
        expression: String? = null,
        asset: String? = null
    ): List<Double> {
        val url = itemStatistics.toHttpUrl().newBuilder()
            .addQueryParameter("collection", item.collection)
            .addQueryParameter("item", item.id)
            .addQueryParameter("max_size", "256")
            .apply {
                if (expression != null) {
                    addQueryParameter("expression", expression)
                    addQueryParameter("asset_as_band", "true")
                }
                if (asset != null) addQueryParameter("assets", asset)
            }
            .build()
            .toString()

        val feature = boxFeature(lat, lng, halfSizeDeg)

        val stats = http.postJson(url, feature).asJsonObject
            .getAsJsonObject("properties")
            ?.getAsJsonObject("statistics")
            ?: throw IllegalStateException("No statistics returned for ${item.id}")

        return stats.entrySet().map { (_, value) ->
            val band = value.asJsonObject
            val validPixels = band.dbl("valid_pixels") ?: band.dbl("count") ?: 0.0
            if (validPixels <= 0.0) Double.NaN else band.dbl("mean") ?: Double.NaN
        }
    }

    /** Median of a single asset (robust for SAR speckle). */
    suspend fun medianOverBox(item: StacItem, lat: Double, lng: Double, halfSizeDeg: Double, asset: String): Double {
        val url = itemStatistics.toHttpUrl().newBuilder()
            .addQueryParameter("collection", item.collection)
            .addQueryParameter("item", item.id)
            .addQueryParameter("assets", asset)
            .addQueryParameter("max_size", "256")
            .build()
            .toString()
        val feature = boxFeature(lat, lng, halfSizeDeg)
        val band = http.postJson(url, feature).asJsonObject
            .getAsJsonObject("properties")
            ?.getAsJsonObject("statistics")
            ?.entrySet()?.firstOrNull()?.value?.asJsonObject
            ?: throw IllegalStateException("No statistics returned for ${item.id}")
        return band.dbl("median") ?: Double.NaN
    }

    /** GeoJSON Feature of a square box centred on the point. */
    private fun boxFeature(lat: Double, lng: Double, halfSizeDeg: Double): JsonObject {
        val ring = JsonArray().apply {
            listOf(
                lng - halfSizeDeg to lat - halfSizeDeg,
                lng + halfSizeDeg to lat - halfSizeDeg,
                lng + halfSizeDeg to lat + halfSizeDeg,
                lng - halfSizeDeg to lat + halfSizeDeg,
                lng - halfSizeDeg to lat - halfSizeDeg
            ).forEach { (x, y) -> add(JsonArray().apply { add(x); add(y) }) }
        }
        return JsonObject().apply {
            addProperty("type", "Feature")
            add("properties", JsonObject())
            add("geometry", JsonObject().apply {
                addProperty("type", "Polygon")
                add("coordinates", JsonArray().apply { add(ring) })
            })
        }
    }

    // java.time needs API 26; minSdk is 24, so format/parse ISO-8601 manually.
    private fun isoUtc(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(millis))

    /** Parses e.g. 2026-09-14T04:32:31.024000Z (fractional seconds of any length). */
    private fun parseIsoUtc(value: String): Long? = runCatching {
        val base = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(value.take(19))!!.time
        val fraction = value.drop(19).removePrefix(".").takeWhile { it.isDigit() }
        val millis = if (fraction.isEmpty()) 0L else fraction.padEnd(3, '0').take(3).toLong()
        base + millis
    }.getOrNull()
}
