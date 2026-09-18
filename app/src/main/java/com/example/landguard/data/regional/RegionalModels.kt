package com.example.landguard.data.regional

import com.example.landguard.domain.model.Severity

/*
 * Regional monitoring models.
 *
 * Every measured value carries its provenance (source + acquisition time).
 * Anything that could not be obtained from a real source is represented as
 * [DataResult.Unavailable] with a human-readable reason — never a placeholder.
 */

sealed interface DataResult<out T> {
    data class Available<T>(val value: T) : DataResult<T>
    data class Unavailable(val reason: String) : DataResult<Nothing>
}

fun <T> DataResult<T>.valueOrNull(): T? = (this as? DataResult.Available)?.value

// ─────────────────────────────────────────────────────────────
// Historical record (NASA Global Landslide Catalog)
// ─────────────────────────────────────────────────────────────

data class LandslideEvent(
    val id: String,
    val title: String,
    val dateMillis: Long?,
    val latitude: Double,
    val longitude: Double,
    val state: String,
    val nearestPlace: String?,
    val locationDescription: String?,
    val locationAccuracy: String?,
    val category: String?,
    val trigger: String?,
    val size: String?,
    val fatalities: Int,
    val injuries: Int,
    val sourceName: String?,
    val sourceLink: String?
)

/** A cluster of recorded landslides — a data-derived monitoring area. */
data class LandslideHotspot(
    val id: String,
    val name: String,
    val state: String,
    val latitude: Double,
    val longitude: Double,
    val eventCount: Int,
    val fatalities: Int,
    val firstEventMillis: Long?,
    val lastEventMillis: Long?,
    val dominantTrigger: String?,
    val events: List<LandslideEvent>
)

data class CatalogSnapshot(
    val events: List<LandslideEvent>,
    val hotspots: List<LandslideHotspot>,
    val sourceUrl: String,
    val fetchedAtMillis: Long,
    val fromCache: Boolean
)

// ─────────────────────────────────────────────────────────────
// Live / latest-available observations
// ─────────────────────────────────────────────────────────────

data class RainfallReading(
    val past72hMm: Double,
    val next24hMm: Double,
    val soilMoistureM3M3: Double?,
    val source: String,
    val fetchedAtMillis: Long
)

data class TerrainReading(
    val elevationM: Double,
    val slopeDeg: Double,
    val source: String
)

data class OpticalReading(
    val sceneId: String,
    val acquiredMillis: Long,
    val clearFraction: Double,
    val ndvi: Double,
    val baseline: OpticalBaseline?,
    val source: String
) {
    val ndviChange: Double? get() = baseline?.let { ndvi - it.ndvi }
}

data class OpticalBaseline(
    val sceneId: String,
    val acquiredMillis: Long,
    val ndvi: Double
)

data class SarReading(
    val sceneId: String,
    val acquiredMillis: Long,
    val vvDb: Double,
    val baselineVvDb: Double?,
    val baselineAcquiredMillis: Long?,
    val source: String
) {
    val vvChangeDb: Double? get() = baselineVvDb?.let { vvDb - it }
}

data class HistoryReading(
    val eventsWithin10Km: Int,
    val fatalitiesWithin10Km: Int,
    val nearestEventKm: Double?,
    val lastEventMillis: Long?
)

data class RiskFactorScore(val name: String, val score: Int, val weight: Double, val detail: String)

data class RiskIndex(
    val score: Int,
    val severity: Severity,
    val factors: List<RiskFactorScore>,
    /** Share of the model weight that was backed by available data (0..1). */
    val coverage: Double
)

data class LocationAnalysis(
    val latitude: Double,
    val longitude: Double,
    val analysedAtMillis: Long,
    val optical: DataResult<OpticalReading>,
    val sar: DataResult<SarReading>,
    val alos4: DataResult<SarReading>,
    val rainfall: DataResult<RainfallReading>,
    val terrain: DataResult<TerrainReading>,
    val history: DataResult<HistoryReading>,
    val risk: DataResult<RiskIndex>
)

/** Live context for a hotspot, fetched in regional batches. */
data class HotspotConditions(
    val rainfall: RainfallReading?,
    val terrain: TerrainReading?
)
