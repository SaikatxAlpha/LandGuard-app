package com.example.landguard.data.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The multi-hop rules the offline mesh relies on: A → B → C → D carries the
 * same alertId, grows hopCount by exactly one per hop, and stops at expiry or
 * MAX_MESH_HOPS. This mirrors what AlertIngestor.relay does before handing the
 * alert to NearbyMeshManager.
 */
class MeshRelayTest {

    private val now = System.currentTimeMillis()

    private fun alert(hopCount: Int = 0, expiresInMs: Long = 3_600_000) = AlertDto(
        alertId = "731bc18e-ca46-4d24-adcf-1d0d2c73fbfb",
        zoneId = "glc_2754_8856",
        zoneName = "Near Mangan",
        level = "critical",
        message = "Evacuate now.",
        timestamp = AlertContract.isoUtc(now - 60_000),
        expiresAt = AlertContract.isoUtc(now + expiresInMs),
        origin = "authority_web",
        hopCount = hopCount
    )

    /** What a device does before passing an alert on. */
    private fun nextHop(alert: AlertDto) = alert.copy(hopCount = alert.hopCount + 1, originDeviceId = "TestDevice")

    @Test
    fun alertSurvivesPhoneAToPhoneDWithGrowingHopCount() {
        // A receives by FCM at hop 0 and passes it on.
        val atA = alert(hopCount = 0)
        val atB = nextHop(atA)
        val atC = nextHop(atB)
        val atD = nextHop(atC)

        assertEquals(1, atB.hopCount)
        assertEquals(2, atC.hopCount)
        assertEquals(3, atD.hopCount)

        // The identity of the alert never changes along the chain.
        listOf(atB, atC, atD).forEach {
            assertEquals(atA.alertId, it.alertId)
            assertEquals(atA.zoneId, it.zoneId)
            assertEquals(atA.level, it.level)
            assertEquals(atA.expiresAt, it.expiresAt)
            assertEquals(atA.timestamp, it.timestamp)
            assertEquals(atA.status, it.status)
            assertTrue(it.isRelayable(now))
        }
    }

    @Test
    fun relayingStopsAtMaxHops() {
        var current = alert(hopCount = 0)
        var hops = 0
        while (nextHop(current).isRelayable(now)) {
            current = nextHop(current)
            hops++
            check(hops <= 100) { "relay chain did not terminate" }
        }
        assertEquals(AlertContract.MAX_MESH_HOPS - 1, current.hopCount)
        assertFalse(nextHop(current).isRelayable(now))
    }

    @Test
    fun expiredAlertIsNeverPassedOn() {
        val expired = alert(hopCount = 1, expiresInMs = -1)
        assertTrue(expired.isExpired(now))
        assertFalse(nextHop(expired).isRelayable(now))
    }

    @Test
    fun cancelledAlertIsNeverPassedOn() {
        assertFalse(nextHop(alert().copy(status = "cancelled")).isRelayable(now))
    }

    @Test
    fun relayedAlertStillDecodesToTheSameAlertOnTheNextPhone() {
        val onTheWire = AlertContract.toMeshJson(nextHop(alert()))
        val received = AlertContract.fromMeshJson(onTheWire)!!
        assertEquals(1, received.hopCount)
        assertEquals(alert().alertId, received.alertId)
        assertEquals("TestDevice", received.originDeviceId)
        assertTrue(received.isRelayable(now))
    }
}
