package com.example.landguard.data.regional

import com.example.landguard.di.PublicDataClient
import com.google.gson.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NASA Global Landslide Catalog (GLC), queried through ArcGIS REST feature
 * services that publish the catalog with its original schema.
 *
 * NASA's own Socrata endpoint was retired, so the catalog is read from the
 * public feature-service copies below (tried in order). The records are the
 * NASA catalog as published (events 2007 onwards); they are historical and
 * are never presented as live observations.
 */
@Singleton
class LandslideCatalogClient @Inject constructor(
    @PublicDataClient private val http: OkHttpClient
) {

    private val featureServices = listOf(
        "https://services5.arcgis.com/XpkmJXTTH9PDa1gO/arcgis/rest/services/nasa_global_landslide_catalog_point/FeatureServer/0",
        "https://services9.arcgis.com/wb84GxCwiPzK3Eow/arcgis/rest/services/nasa_global_landslide_catalog_point/FeatureServer/0"
    )

    private val outFields = listOf(
        "event_id", "event_date", "event_title", "location_description", "location_accuracy",
        "landslide_category", "landslide_trigger", "landslide_size", "fatality_count", "injury_count",
        "latitude", "longitude", "admin_division_name", "gazetteer_closest_point",
        "source_name", "source_link"
    ).joinToString(",")

    data class Result(val events: List<LandslideEvent>, val sourceUrl: String)

    /** All catalogued landslides in the eight Northeast states. */
    suspend fun fetchNortheastEvents(): Result {
        var lastError: Exception? = null
        for (service in featureServices) {
            try {
                return Result(queryAll(service), service)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Landslide catalog unreachable")
    }

    private suspend fun queryAll(service: String): List<LandslideEvent> {
        val states = NortheastRegion.STATES.joinToString(",") { "'${it.replace("'", "''")}'" }
        val where = "country_name='India' AND admin_division_name IN ($states)"
        val pageSize = 1000
        val events = mutableListOf<LandslideEvent>()
        var offset = 0
        while (true) {
            val url = "$service/query".toHttpUrl().newBuilder()
                .addQueryParameter("where", where)
                .addQueryParameter(
                    "geometry",
                    "${NortheastRegion.MIN_LNG},${NortheastRegion.MIN_LAT},${NortheastRegion.MAX_LNG},${NortheastRegion.MAX_LAT}"
                )
                .addQueryParameter("geometryType", "esriGeometryEnvelope")
                .addQueryParameter("inSR", "4326")
                .addQueryParameter("spatialRel", "esriSpatialRelIntersects")
                .addQueryParameter("outFields", outFields)
                .addQueryParameter("returnGeometry", "false")
                .addQueryParameter("orderByFields", "event_date DESC")
                .addQueryParameter("resultOffset", offset.toString())
                .addQueryParameter("resultRecordCount", pageSize.toString())
                .addQueryParameter("f", "json")
                .build()
                .toString()

            val root = http.getJson(url).asJsonObject
            root.getAsJsonObject("error")?.let { error ->
                throw IllegalStateException("Catalog query failed: ${error.str("message") ?: error}")
            }
            val features = root.getAsJsonArray("features") ?: break
            features.forEach { f -> parse(f.asJsonObject.getAsJsonObject("attributes"))?.let(events::add) }

            val exceeded = root.get("exceededTransferLimit")?.asBoolean == true
            if (!exceeded || features.size() == 0) break
            offset += features.size()
        }
        if (events.isEmpty()) throw IllegalStateException("Catalog returned no records")
        return events
    }

    private fun parse(a: JsonObject?): LandslideEvent? {
        a ?: return null
        val lat = a.dbl("latitude") ?: return null
        val lng = a.dbl("longitude") ?: return null
        val state = a.str("admin_division_name") ?: return null
        return LandslideEvent(
            id = a.str("event_id") ?: "${lat}_${lng}_${a.lng("event_date")}",
            title = a.str("event_title") ?: "Landslide",
            dateMillis = a.lng("event_date"),
            latitude = lat,
            longitude = lng,
            state = state,
            nearestPlace = a.str("gazetteer_closest_point"),
            locationDescription = a.str("location_description"),
            locationAccuracy = a.str("location_accuracy"),
            category = a.str("landslide_category"),
            trigger = a.str("landslide_trigger"),
            size = a.str("landslide_size"),
            fatalities = a.int("fatality_count") ?: 0,
            injuries = a.int("injury_count") ?: 0,
            sourceName = a.str("source_name"),
            sourceLink = a.str("source_link")
        )
    }
}
