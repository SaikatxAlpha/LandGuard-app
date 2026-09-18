package com.example.landguard.data.regional

import com.example.landguard.domain.model.Severity
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure (Android-free) regional analytics: clustering of recorded landslides
 * into monitored areas, and the transparent LandGuard risk index.
 *
 * Risk index — a documented heuristic over real inputs only:
 *   slope      0 at ≤10°, 1 at ≥35°            (Copernicus DEM)
 *   rainfall   (72 h observed + 24 h forecast) / 150 mm, capped  (Open-Meteo)
 *   record     recorded landslides nearby       (NASA GLC)
 *   vegetation NDVI drop vs. a year earlier; −0.15 → 1  (Sentinel-2)
 *   SAR        |ΔVV| from 0.5 dB to 3 dB → 0..1  (Sentinel-1)
 * Weights are renormalised over the inputs that are actually available; the
 * share of weight backed by data is reported as `coverage`.
 */
object RegionalAnalytics {

    const val CLUSTER_RADIUS_KM = 15.0
    const val RAIN_SATURATION_MM = 150.0

    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0088
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** Greedy spatial clustering of events into areas of [CLUSTER_RADIUS_KM]. */
    fun cluster(events: List<LandslideEvent>, radiusKm: Double = CLUSTER_RADIUS_KM): List<LandslideHotspot> {
        class Acc(var lat: Double, var lng: Double) {
            val members = mutableListOf<LandslideEvent>()
        }

        val clusters = mutableListOf<Acc>()
        for (event in events.sortedBy { it.dateMillis ?: 0L }) {
            val target = clusters
                .map { it to distanceKm(it.lat, it.lng, event.latitude, event.longitude) }
                .filter { it.second <= radiusKm }
                .minByOrNull { it.second }
                ?.first
            if (target == null) {
                clusters += Acc(event.latitude, event.longitude).also { it.members += event }
            } else {
                target.members += event
                target.lat = target.members.map { it.latitude }.average()
                target.lng = target.members.map { it.longitude }.average()
            }
        }

        val hotspots = clusters.map { c ->
            val members = c.members
            // Catalog gazetteer names are the nearest named place, so label them as "Near …".
            val name = members.mapNotNull { it.nearestPlace }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key?.let { "Near $it" }
                ?: members.firstNotNullOfOrNull { it.locationDescription?.take(48) }
                ?: "Landslide cluster"
            val state = members.groupingBy { it.state }.eachCount().maxByOrNull { it.value }!!.key
            LandslideHotspot(
                id = "glc_%d_%d".format(Locale.US, Math.round(c.lat * 100), Math.round(c.lng * 100)),
                name = name,
                state = state,
                latitude = c.lat,
                longitude = c.lng,
                eventCount = members.size,
                fatalities = members.sumOf { it.fatalities },
                firstEventMillis = members.mapNotNull { it.dateMillis }.minOrNull(),
                lastEventMillis = members.mapNotNull { it.dateMillis }.maxOrNull(),
                dominantTrigger = members.mapNotNull { it.trigger }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
                events = members.sortedByDescending { it.dateMillis ?: 0L }
            )
        }

        // Several clusters can share a nearest place; tell them apart by position.
        val duplicates = hotspots.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
        return hotspots.map { h ->
            if (h.name in duplicates) {
                h.copy(name = "%s (%.2f°N, %.2f°E)".format(Locale.US, h.name, h.latitude, h.longitude))
            } else h
        }
    }

    fun slopeFactor(slopeDeg: Double) = ((slopeDeg - 10.0) / 25.0).coerceIn(0.0, 1.0)

    fun rainFactor(r: RainfallReading) = ((r.past72hMm + r.next24hMm) / RAIN_SATURATION_MM).coerceIn(0.0, 1.0)

    fun hotspotRisk(
        hotspot: LandslideHotspot,
        conditions: HotspotConditions?,
        categorize: (Int) -> Severity
    ): RiskIndex {
        val factors = mutableListOf<RiskFactorScore>()
        val history = (ln(1.0 + hotspot.eventCount) / ln(11.0) + hotspot.fatalities * 0.01).coerceIn(0.0, 1.0)
        factors += RiskFactorScore(
            "Landslide record", (history * 100).toInt(), 0.45,
            "${hotspot.eventCount} recorded landslide${if (hotspot.eventCount == 1) "" else "s"}"
        )
        conditions?.rainfall?.let { r ->
            factors += RiskFactorScore(
                "Rainfall", (rainFactor(r) * 100).toInt(), 0.35,
                "%.0f mm past 72 h, %.0f mm next 24 h".format(Locale.US, r.past72hMm, r.next24hMm)
            )
        }
        conditions?.terrain?.let { t ->
            factors += RiskFactorScore(
                "Slope", (slopeFactor(t.slopeDeg) * 100).toInt(), 0.20,
                "%.0f° mean slope".format(Locale.US, t.slopeDeg)
            )
        }
        return combine(factors, categorize)
    }

    fun locationRisk(
        optical: DataResult<OpticalReading>,
        sar: DataResult<SarReading>,
        rain: DataResult<RainfallReading>,
        terrain: DataResult<TerrainReading>,
        history: DataResult<HistoryReading>,
        categorize: (Int) -> Severity
    ): DataResult<RiskIndex> {
        val factors = mutableListOf<RiskFactorScore>()
        terrain.valueOrNull()?.let {
            factors += RiskFactorScore("Slope", (slopeFactor(it.slopeDeg) * 100).toInt(), 0.25, "%.0f°".format(Locale.US, it.slopeDeg))
        }
        rain.valueOrNull()?.let {
            factors += RiskFactorScore(
                "Rainfall", (rainFactor(it) * 100).toInt(), 0.30,
                "%.0f mm past 72 h, %.0f mm next 24 h".format(Locale.US, it.past72hMm, it.next24hMm)
            )
        }
        history.valueOrNull()?.let {
            val f = (it.eventsWithin10Km / 5.0).coerceIn(0.0, 1.0)
            factors += RiskFactorScore("Landslide record", (f * 100).toInt(), 0.25, "${it.eventsWithin10Km} recorded within 10 km")
        }
        optical.valueOrNull()?.ndviChange?.let { d ->
            val f = (-d / 0.15).coerceIn(0.0, 1.0)
            factors += RiskFactorScore("Vegetation loss (NDVI)", (f * 100).toInt(), 0.12, "%+.2f vs. a year earlier".format(Locale.US, d))
        }
        sar.valueOrNull()?.vvChangeDb?.let { d ->
            val f = ((abs(d) - 0.5) / 2.5).coerceIn(0.0, 1.0)
            factors += RiskFactorScore("Surface change (SAR)", (f * 100).toInt(), 0.08, "%+.1f dB vs. a year earlier".format(Locale.US, d))
        }
        if (factors.sumOf { it.weight } < 0.5) {
            return DataResult.Unavailable("Not enough real data to compute a risk index here")
        }
        return DataResult.Available(combine(factors, categorize))
    }

    fun historyReading(events: List<LandslideEvent>, lat: Double, lng: Double): HistoryReading {
        val withDistance = events.map { it to distanceKm(lat, lng, it.latitude, it.longitude) }
        val near = withDistance.filter { it.second <= 10.0 }.map { it.first }
        return HistoryReading(
            eventsWithin10Km = near.size,
            fatalitiesWithin10Km = near.sumOf { it.fatalities },
            nearestEventKm = withDistance.minOfOrNull { it.second },
            lastEventMillis = near.mapNotNull { it.dateMillis }.maxOrNull()
        )
    }

    private fun combine(factors: List<RiskFactorScore>, categorize: (Int) -> Severity): RiskIndex {
        val weight = factors.sumOf { it.weight }
        val score = if (weight == 0.0) 0 else (factors.sumOf { it.score * it.weight } / weight).toInt().coerceIn(0, 100)
        return RiskIndex(score = score, severity = categorize(score), factors = factors, coverage = weight)
    }
}
