package com.example.landguard.data.regional

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10

/**
 * Real Sentinel-2 / Sentinel-1 readings for a location, computed server-side
 * by Microsoft Planetary Computer over a ≈2.2 km box.
 *
 * Optical: newest scene (≤120 days) with ≥40 % cloud-free pixels in the box
 * (Sentinel-2 scene classification), NDVI averaged over vegetated/bare land
 * only, compared with a cloud-free scene from the same season a year earlier.
 *
 * SAR: newest Sentinel-1 RTC pass (≤45 days), median VV backscatter,
 * compared with a pass on the same relative orbit about a year earlier.
 */
@Singleton
class SatelliteAnalyzer @Inject constructor(
    private val planetary: PlanetaryComputerClient
) {

    suspend fun optical(lat: Double, lng: Double, now: Long = System.currentTimeMillis()): DataResult<OpticalReading> {
        val recent = planetary.search(
            collection = "sentinel-2-l2a", lat = lat, lng = lng,
            fromMillis = now - 120 * DAY_MS, toMillis = now,
            limit = 12, maxCloudCover = 85.0
        )
        if (recent.isEmpty()) return DataResult.Unavailable("No Sentinel-2 scene over this location in the last 120 days")

        val current = firstClearNdvi(recent, lat, lng)
            ?: return DataResult.Unavailable(
                "Recent Sentinel-2 passes are cloud-covered here (latest ${
                    RegionalMonitoringRepositoryImpl.formatDate(recent.first().acquiredMillis)
                })"
            )

        val baseline = runCatching {
            val candidates = planetary.search(
                collection = "sentinel-2-l2a", lat = lat, lng = lng,
                fromMillis = current.item.acquiredMillis - 395 * DAY_MS,
                toMillis = current.item.acquiredMillis - 335 * DAY_MS,
                limit = 10, maxCloudCover = 70.0
            )
            firstClearNdvi(candidates, lat, lng)?.let { OpticalBaseline(it.item.id, it.item.acquiredMillis, it.ndvi) }
        }.getOrNull()

        return DataResult.Available(
            OpticalReading(
                sceneId = current.item.id,
                acquiredMillis = current.item.acquiredMillis,
                clearFraction = current.clearFraction,
                ndvi = current.ndvi,
                baseline = baseline,
                source = "ESA Sentinel-2 L2A via Microsoft Planetary Computer"
            )
        )
    }

    private data class ClearNdvi(val item: PlanetaryComputerClient.StacItem, val ndvi: Double, val clearFraction: Double)

    private suspend fun firstClearNdvi(
        items: List<PlanetaryComputerClient.StacItem>,
        lat: Double,
        lng: Double
    ): ClearNdvi? {
        for (item in items.take(6)) {
            val means = planetary.meanOverBox(item, lat, lng, BOX_HALF_DEG, expression = NDVI_MASKED_EXPRESSION)
            if (means.size < 3) continue
            val ndviSum = means[0]
            val landFraction = means[1]
            val clearFraction = means[2]
            if (ndviSum.isNaN() || landFraction.isNaN() || clearFraction.isNaN()) continue
            if (clearFraction >= MIN_CLEAR_FRACTION && landFraction >= 0.05) {
                return ClearNdvi(item, (ndviSum / landFraction).coerceIn(-1.0, 1.0), clearFraction)
            }
        }
        return null
    }

    suspend fun sar(lat: Double, lng: Double, now: Long = System.currentTimeMillis()): DataResult<SarReading> {
        val latest = planetary.search(
            collection = "sentinel-1-rtc", lat = lat, lng = lng,
            fromMillis = now - 45 * DAY_MS, toMillis = now, limit = 1
        ).firstOrNull() ?: return DataResult.Unavailable("No Sentinel-1 SAR pass over this location in the last 45 days")

        val vv = planetary.medianOverBox(latest, lat, lng, BOX_HALF_DEG, asset = "vv")
        if (vv.isNaN() || vv <= 0.0) return DataResult.Unavailable("Sentinel-1 scene has no valid pixels here")

        val baseline = runCatching {
            planetary.search(
                collection = "sentinel-1-rtc", lat = lat, lng = lng,
                fromMillis = latest.acquiredMillis - 380 * DAY_MS,
                toMillis = latest.acquiredMillis - 350 * DAY_MS,
                limit = 1,
                orbitState = latest.orbitState,
                relativeOrbit = latest.relativeOrbit
            ).firstOrNull()?.let { item ->
                val base = planetary.medianOverBox(item, lat, lng, BOX_HALF_DEG, asset = "vv")
                if (base.isNaN() || base <= 0.0) null else item to 10 * log10(base)
            }
        }.getOrNull()

        return DataResult.Available(
            SarReading(
                sceneId = latest.id,
                acquiredMillis = latest.acquiredMillis,
                vvDb = 10 * log10(vv),
                baselineVvDb = baseline?.second,
                baselineAcquiredMillis = baseline?.first?.acquiredMillis,
                source = "ESA Sentinel-1 RTC (C-band SAR) via Microsoft Planetary Computer"
            )
        )
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        const val BOX_HALF_DEG = 0.01 // ≈ 2.2 km box
        private const val MIN_CLEAR_FRACTION = 0.4

        /** masked NDVI sum ; vegetated/bare land fraction ; cloud-free fraction (SCL 4–6). */
        private const val NDVI_MASKED_EXPRESSION =
            "where((SCL==4)|(SCL==5),(B08-B04)/(B08+B04),0);" +
                    "where((SCL==4)|(SCL==5),1,0);" +
                    "where((SCL>=4)&(SCL<=6),1,0)"
    }
}
