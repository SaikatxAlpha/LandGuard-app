package com.example.landguard.data.regional

import com.example.landguard.domain.service.RiskEngineServiceImpl
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Web = backend = Android: the fixture is a snapshot of the live LandGuard
 * backend (real NASA GLC catalog + Open-Meteo / Copernicus DEM conditions).
 * The app's own RegionalAnalytics must reproduce every monitored area id,
 * name and risk score the backend serves to the authority control center.
 */
class BackendParityTest {

    private data class Zone(
        val id: String,
        val name: String,
        val state: String,
        val eventCount: Int,
        val rainfall: RainfallReading?,
        val terrain: TerrainReading?,
        val expectedScore: Int,
        val expectedLevel: String,
        val expectedFactors: List<String>
    )

    private data class Fixture(
        val events: List<LandslideEvent>,
        @SerializedName("zones") val zones: List<Zone>
    )

    private val fixture: Fixture by lazy {
        val stream = javaClass.classLoader!!.getResourceAsStream("parity/monitoring-fixture.json")
        Gson().fromJson(stream.reader(), Fixture::class.java)
    }

    private val categorize = RiskEngineServiceImpl()::categorizeScore

    @Test
    fun clustersMatchBackendIdsAndNames() {
        val hotspots = RegionalAnalytics.cluster(fixture.events).associateBy { it.id }
        assertEquals(fixture.zones.size, hotspots.size)
        for (zone in fixture.zones) {
            val h = hotspots[zone.id]
            assertNotNull("Android is missing backend area ${zone.id}", h)
            assertEquals(zone.name, h!!.name)
            assertEquals(zone.state, h.state)
            assertEquals(zone.eventCount, h.eventCount)
        }
    }

    @Test
    fun riskScoresLevelsAndFactorsMatchBackend() {
        val hotspots = RegionalAnalytics.cluster(fixture.events).associateBy { it.id }
        for (zone in fixture.zones) {
            val risk = RegionalAnalytics.hotspotRisk(hotspots.getValue(zone.id), HotspotConditions(zone.rainfall, zone.terrain), categorize)
            assertEquals("score for ${zone.id}", zone.expectedScore, risk.score)
            assertEquals("level for ${zone.id}", zone.expectedLevel.uppercase(), risk.severity.name)
            assertEquals("factors for ${zone.id}", zone.expectedFactors, risk.factors.map { "${it.name}|${it.score}|${it.detail}" })
        }
    }
}
