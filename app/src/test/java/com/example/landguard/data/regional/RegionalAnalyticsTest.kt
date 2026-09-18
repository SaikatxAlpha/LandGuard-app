package com.example.landguard.data.regional

import com.example.landguard.domain.service.RiskEngineServiceImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalAnalyticsTest {

    private val categorize = RiskEngineServiceImpl()::categorizeScore

    private fun event(id: String, lat: Double, lng: Double, state: String = "Meghalaya", place: String? = "Shillong") =
        LandslideEvent(
            id = id, title = "t", dateMillis = 1_500_000_000_000L + id.hashCode(), latitude = lat, longitude = lng,
            state = state, nearestPlace = place, locationDescription = null, locationAccuracy = null,
            category = null, trigger = "downpour", size = null, fatalities = 1, injuries = 0,
            sourceName = null, sourceLink = null
        )

    @Test
    fun haversineDistanceIsCorrect() {
        // Shillong → Guwahati is ≈ 66 km great-circle.
        val d = RegionalAnalytics.distanceKm(25.5788, 91.8933, 26.1445, 91.7362)
        assertTrue("was $d", d in 60.0..70.0)
    }

    @Test
    fun nearbyEventsClusterAndDistantOnesDoNot() {
        val events = listOf(
            event("a", 25.57, 91.88),
            event("b", 25.60, 91.90),                                   // ~4 km from a
            event("c", 23.73, 92.72, state = "Mizoram", place = "Aizawl") // ~230 km away
        )
        val clusters = RegionalAnalytics.cluster(events)
        assertEquals(2, clusters.size)
        val shillong = clusters.first { it.state == "Meghalaya" }
        assertEquals(2, shillong.eventCount)
        assertEquals("Near Shillong", shillong.name)
        assertEquals(2, shillong.fatalities)
    }

    @Test
    fun hotspotRiskUsesOnlyAvailableInputs() {
        val hotspot = RegionalAnalytics.cluster(listOf(event("a", 25.57, 91.88))).single()
        val historyOnly = RegionalAnalytics.hotspotRisk(hotspot, null, categorize)
        assertEquals(1, historyOnly.factors.size)
        assertEquals(0.45, historyOnly.coverage, 1e-9)

        val withLive = RegionalAnalytics.hotspotRisk(
            hotspot,
            HotspotConditions(
                rainfall = RainfallReading(150.0, 0.0, 0.4, "test", 0L),
                terrain = TerrainReading(1500.0, 35.0, "test")
            ),
            categorize
        )
        assertEquals(3, withLive.factors.size)
        assertEquals(1.0, withLive.coverage, 1e-9)
        assertTrue(withLive.score > historyOnly.score)
    }

    @Test
    fun locationRiskIsUnavailableWhenMostInputsAreMissing() {
        val none = DataResult.Unavailable("x")
        val risk = RegionalAnalytics.locationRisk(
            optical = none, sar = none, rain = none,
            terrain = DataResult.Available(TerrainReading(100.0, 40.0, "t")),
            history = none,
            categorize = categorize
        )
        assertTrue(risk is DataResult.Unavailable)
    }
}
