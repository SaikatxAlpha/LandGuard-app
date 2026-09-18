package com.example.landguard.data.regional

import android.content.Context
import android.util.Log
import com.example.landguard.domain.service.RiskEngineService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Regional landslide monitoring for Northeast India, built only from real data:
 *
 *  • Monitored areas  — clusters of recorded landslides from the NASA Global
 *    Landslide Catalog across the eight Northeast states (not hand-picked).
 *  • Live conditions  — Open-Meteo rainfall / soil moisture (hourly models)
 *    and Copernicus DEM slope, fetched in regional batches.
 *  • Location analysis — on demand for any GPS position: latest cloud-free
 *    Sentinel-2 NDVI vs. the same season a year earlier, latest Sentinel-1
 *    VV backscatter vs. a year earlier (same orbit), rainfall, slope and the
 *    local landslide record.
 *
 * Missing inputs are reported as [DataResult.Unavailable]; nothing is
 * interpolated, simulated or substituted.
 */
interface RegionalMonitoringRepository {
    /** null while the first load is in progress. */
    val catalog: StateFlow<DataResult<CatalogSnapshot>?>
    val hotspotConditions: StateFlow<Map<String, HotspotConditions>>
    val conditionsUpdatedAtMillis: StateFlow<Long?>

    suspend fun refreshCatalog(force: Boolean = false)
    suspend fun refreshConditions(force: Boolean = false)
    suspend fun analyzeLocation(lat: Double, lng: Double, force: Boolean = false): LocationAnalysis

    fun hotspotRisk(hotspot: LandslideHotspot, conditions: HotspotConditions?): RiskIndex
}

@Singleton
class RegionalMonitoringRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalogClient: LandslideCatalogClient,
    private val satellite: SatelliteAnalyzer,
    private val meteo: OpenMeteoClient,
    private val riskEngine: RiskEngineService
) : RegionalMonitoringRepository {

    private val tag = "LandGuardRegional"
    private val gson = Gson()

    private val _catalog = MutableStateFlow<DataResult<CatalogSnapshot>?>(null)
    override val catalog: StateFlow<DataResult<CatalogSnapshot>?> = _catalog.asStateFlow()

    private val _conditions = MutableStateFlow<Map<String, HotspotConditions>>(emptyMap())
    override val hotspotConditions: StateFlow<Map<String, HotspotConditions>> = _conditions.asStateFlow()

    private val _conditionsUpdatedAt = MutableStateFlow<Long?>(null)
    override val conditionsUpdatedAtMillis: StateFlow<Long?> = _conditionsUpdatedAt.asStateFlow()

    private val catalogMutex = Mutex()
    private val conditionsMutex = Mutex()
    private val analysisMutex = Mutex()
    private val terrainCache = mutableMapOf<String, TerrainReading>()
    private val analysisCache = mutableMapOf<String, LocationAnalysis>()

    private val cacheFile get() = File(context.filesDir, "ne_landslide_catalog.json")
    private val terrainFile get() = File(context.filesDir, "ne_terrain_cache.json")
    private var terrainLoaded = false

    // ─────────────────────────────────────────────────────────
    // Catalog → monitored areas
    // ─────────────────────────────────────────────────────────

    override suspend fun refreshCatalog(force: Boolean) = catalogMutex.withLock {
        val current = _catalog.value
        if (!force && current is DataResult.Available && !current.value.fromCache &&
            System.currentTimeMillis() - current.value.fetchedAtMillis < CATALOG_TTL_MS
        ) return@withLock

        val cached = withContext(Dispatchers.IO) { readCache() }
        if (!force && cached != null && System.currentTimeMillis() - cached.fetchedAtMillis < CATALOG_TTL_MS) {
            _catalog.value = DataResult.Available(snapshot(cached, fromCache = true))
            return@withLock
        }

        try {
            val result = catalogClient.fetchNortheastEvents()
            val record = CacheRecord(result.events, result.sourceUrl, System.currentTimeMillis())
            withContext(Dispatchers.IO) { writeCache(record) }
            _catalog.value = DataResult.Available(snapshot(record, fromCache = false))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(tag, "Catalog fetch failed", e)
            _catalog.value = if (cached != null) {
                // A previously downloaded copy of the same real catalog — labelled as cached.
                DataResult.Available(snapshot(cached, fromCache = true))
            } else {
                DataResult.Unavailable("Landslide catalog unavailable: ${e.message ?: "network error"}")
            }
        }
    }

    private suspend fun snapshot(record: CacheRecord, fromCache: Boolean) = withContext(Dispatchers.Default) {
        CatalogSnapshot(
            events = record.events,
            hotspots = RegionalAnalytics.cluster(record.events),
            sourceUrl = record.sourceUrl,
            fetchedAtMillis = record.fetchedAtMillis,
            fromCache = fromCache
        )
    }

    // ─────────────────────────────────────────────────────────
    // Live conditions for every monitored area (batched)
    // ─────────────────────────────────────────────────────────

    override suspend fun refreshConditions(force: Boolean) = conditionsMutex.withLock {
        val snapshot = (_catalog.value as? DataResult.Available)?.value ?: return@withLock
        val last = _conditionsUpdatedAt.value
        if (!force && last != null && System.currentTimeMillis() - last < CONDITIONS_TTL_MS) return@withLock

        val hotspots = snapshot.hotspots
        val rainfall = try {
            meteo.rainfall(hotspots.map { it.latitude to it.longitude })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(tag, "Regional rainfall fetch failed", e)
            null
        }

        // Terrain is static: load the on-disk cache once, then only fetch new areas.
        if (!terrainLoaded) {
            withContext(Dispatchers.IO) { readTerrainCache() }?.let { terrainCache.putAll(it) }
            terrainLoaded = true
        }
        val missingTerrain = hotspots.filter { it.id !in terrainCache }
        if (missingTerrain.isNotEmpty()) {
            try {
                meteo.terrain(missingTerrain.map { it.latitude to it.longitude })
                    .forEachIndexed { i, reading -> reading?.let { terrainCache[missingTerrain[i].id] = it } }
                val toSave = terrainCache.toMap()
                withContext(Dispatchers.IO) { writeTerrainCache(toSave) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(tag, "Regional terrain fetch failed", e)
            }
        }

        _conditions.value = hotspots.mapIndexed { i, h ->
            h.id to HotspotConditions(rainfall = rainfall?.getOrNull(i), terrain = terrainCache[h.id])
        }.toMap()
        if (rainfall != null) _conditionsUpdatedAt.value = System.currentTimeMillis()
    }

    override fun hotspotRisk(hotspot: LandslideHotspot, conditions: HotspotConditions?): RiskIndex =
        RegionalAnalytics.hotspotRisk(hotspot, conditions, riskEngine::categorizeScore)

    // ─────────────────────────────────────────────────────────
    // On-demand analysis of a location
    // ─────────────────────────────────────────────────────────

    override suspend fun analyzeLocation(lat: Double, lng: Double, force: Boolean): LocationAnalysis {
        val key = "%.2f,%.2f".format(Locale.US, lat, lng)
        analysisMutex.withLock {
            val cached = analysisCache[key]
            if (!force && cached != null && System.currentTimeMillis() - cached.analysedAtMillis < ANALYSIS_TTL_MS) {
                return cached
            }
        }

        val result = coroutineScope {
            val optical = async { safely("Sentinel-2") { satellite.optical(lat, lng) } }
            val sar = async { safely("Sentinel-1") { satellite.sar(lat, lng) } }
            val rain = async {
                safely("Rainfall") {
                    meteo.rainfall(listOf(lat to lng)).firstOrNull()
                        ?.let { DataResult.Available(it) }
                        ?: DataResult.Unavailable("Rainfall data unavailable for this location")
                }
            }
            val terrain = async {
                safely("Terrain") {
                    meteo.terrain(listOf(lat to lng)).firstOrNull()
                        ?.let { DataResult.Available(it) }
                        ?: DataResult.Unavailable("Elevation data unavailable for this location")
                }
            }
            val history: DataResult<HistoryReading> =
                (_catalog.value as? DataResult.Available)?.value
                    ?.let { DataResult.Available(RegionalAnalytics.historyReading(it.events, lat, lng)) }
                    ?: DataResult.Unavailable("Landslide catalog not loaded")

            val o = optical.await()
            val s = sar.await()
            val r = rain.await()
            val t = terrain.await()
            LocationAnalysis(
                latitude = lat,
                longitude = lng,
                analysedAtMillis = System.currentTimeMillis(),
                optical = o,
                sar = s,
                alos4 = DataResult.Unavailable(ALOS4_UNAVAILABLE),
                rainfall = r,
                terrain = t,
                history = history,
                risk = RegionalAnalytics.locationRisk(o, s, r, t, history, riskEngine::categorizeScore)
            )
        }

        analysisMutex.withLock { analysisCache[key] = result }
        return result
    }

    private suspend fun <T> safely(label: String, block: suspend () -> DataResult<T>): DataResult<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(tag, "$label request failed", e)
            DataResult.Unavailable("$label data unavailable (${e.message?.take(80) ?: "request failed"})")
        }

    // ─────────────────────────────────────────────────────────
    // Disk cache for the (static, historical) catalog
    // ─────────────────────────────────────────────────────────

    private data class CacheRecord(val events: List<LandslideEvent>, val sourceUrl: String, val fetchedAtMillis: Long)

    private fun readCache(): CacheRecord? = runCatching {
        if (!cacheFile.exists()) return null
        gson.fromJson<CacheRecord>(cacheFile.readText(), object : TypeToken<CacheRecord>() {}.type)
            ?.takeIf { !it.events.isNullOrEmpty() }
    }.getOrNull()

    private fun writeCache(record: CacheRecord) {
        runCatching { cacheFile.writeText(gson.toJson(record)) }
    }

    private fun readTerrainCache(): Map<String, TerrainReading>? = runCatching {
        if (!terrainFile.exists()) return null
        gson.fromJson<Map<String, TerrainReading>>(
            terrainFile.readText(),
            object : TypeToken<Map<String, TerrainReading>>() {}.type
        )
    }.getOrNull()

    private fun writeTerrainCache(map: Map<String, TerrainReading>) {
        runCatching { terrainFile.writeText(gson.toJson(map)) }
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val CATALOG_TTL_MS = 7 * DAY_MS
        // Regional rainfall for every monitored area: every 2 h (Open-Meteo free-tier limits
        // count each location as a call). The user's own location refreshes every 30 min.
        private const val CONDITIONS_TTL_MS = 2L * 60 * 60 * 1000
        private const val ANALYSIS_TTL_MS = 30L * 60 * 1000

        const val ALOS4_UNAVAILABLE =
            "No public ALOS-4 PALSAR-3 data service is available to this app. " +
                    "JAXA distributes ALOS-4 data through G-Portal to registered users only."

        fun formatDate(millis: Long): String =
            SimpleDateFormat("d MMM yyyy", Locale.US).format(Date(millis))
    }
}
