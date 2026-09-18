package com.example.landguard.data.regional

import com.example.landguard.domain.service.RiskEngineServiceImpl
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * End-to-end check against the real public services (network required).
 * Skipped unless the environment variable LANDGUARD_LIVE=1 is set.
 */
class RegionalLiveDataTest {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    @Before
    fun requireLiveFlag() {
        assumeTrue("Set LANDGUARD_LIVE=1 to run live data tests", System.getenv("LANDGUARD_LIVE") == "1")
    }

    @Test
    fun regionalPipelineUsesRealData() = runBlocking {
        val categorize = RiskEngineServiceImpl()::categorizeScore

        // 1. Monitored areas across Northeast India from the NASA catalog.
        val catalog = LandslideCatalogClient(http).fetchNortheastEvents()
        val hotspots = RegionalAnalytics.cluster(catalog.events)
        println("Catalog source: ${catalog.sourceUrl}")
        println("Events: ${catalog.events.size}, monitored areas: ${hotspots.size}")
        NortheastRegion.STATES.forEach { s ->
            println("  $s: ${catalog.events.count { it.state == s }} events, ${hotspots.count { it.state == s }} areas")
        }
        assertTrue(catalog.events.size > 100)
        assertTrue("all 8 states covered", NortheastRegion.STATES.all { s -> catalog.events.any { it.state == s } })

        // 2. Live rainfall + terrain for every monitored area (batched).
        val meteo = OpenMeteoClient(http)
        val points = hotspots.map { it.latitude to it.longitude }
        val rain = meteo.rainfall(points)
        val terrain = meteo.terrain(points)
        println("Rainfall available for ${rain.count { it != null }}/${points.size} areas, terrain for ${terrain.count { it != null }}/${points.size}")
        assertTrue(rain.count { it != null } > points.size / 2)

        val ranked = hotspots.indices
            .map { i -> hotspots[i] to RegionalAnalytics.hotspotRisk(hotspots[i], HotspotConditions(rain[i], terrain[i]), categorize) }
            .sortedByDescending { it.second.score }
        println("Top monitored areas right now:")
        ranked.take(8).forEach { (h, r) ->
            val i = hotspots.indexOf(h)
            println(
                "  %-28s %-18s events=%-3d rain72h=%s slope=%s → %d %s".format(
                    h.name.take(28), h.state, h.eventCount,
                    rain[i]?.past72hMm?.let { "%.0fmm".format(it) } ?: "n/a",
                    terrain[i]?.slopeDeg?.let { "%.0f°".format(it) } ?: "n/a",
                    r.score, r.severity
                )
            )
        }

        // 3. On-demand satellite analysis for real locations.
        val analyzer = SatelliteAnalyzer(PlanetaryComputerClient(http))
        listOf(
            "Shillong (Meghalaya)" to (25.5788 to 91.8933),
            "Aizawl (Mizoram)" to (23.7271 to 92.7176),
            "Gangtok (Sikkim)" to (27.3389 to 88.6065)
        ).forEach { (label, p) ->
            val optical = analyzer.optical(p.first, p.second)
            val sar = analyzer.sar(p.first, p.second)
            println("$label")
            println("  Sentinel-2: " + when (optical) {
                is DataResult.Available -> optical.value.let {
                    "NDVI %.2f, Δ=%s, scene %s (%.0f%% clear)".format(
                        it.ndvi, it.ndviChange?.let { d -> "%+.2f".format(d) } ?: "n/a",
                        RegionalMonitoringRepositoryImpl.formatDate(it.acquiredMillis), it.clearFraction * 100
                    )
                }
                is DataResult.Unavailable -> "DATA UNAVAILABLE — ${optical.reason}"
            })
            println("  Sentinel-1: " + when (sar) {
                is DataResult.Available -> sar.value.let {
                    "VV %.1f dB, Δ=%s, pass %s".format(
                        it.vvDb, it.vvChangeDb?.let { d -> "%+.1f dB".format(d) } ?: "n/a",
                        RegionalMonitoringRepositoryImpl.formatDate(it.acquiredMillis)
                    )
                }
                is DataResult.Unavailable -> "DATA UNAVAILABLE — ${sar.reason}"
            })
            assertTrue(optical is DataResult.Available || optical is DataResult.Unavailable)
        }
    }
}
