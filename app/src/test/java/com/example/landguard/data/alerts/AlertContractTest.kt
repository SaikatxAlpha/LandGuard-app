package com.example.landguard.data.alerts

import com.example.landguard.domain.model.Severity
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The same alert must parse identically from FCM, REST and the offline mesh. */
class AlertContractTest {

    private val now = System.currentTimeMillis()
    private val issued = AlertContract.isoUtc(now - 60_000)
    private val expires = AlertContract.isoUtc(now + 3_600_000)

    /** Exactly what backend/services/alert-contract.js#toFcmData produces. */
    private val fcm = mapOf(
        "type" to "alert", "schemaVersion" to "1",
        "alertId" to "731bc18e-ca46-4d24-adcf-1d0d2c73fbfb", "zoneId" to "glc_2754_8856", "zoneName" to "Near Mangan",
        "level" to "critical", "severity" to "CRITICAL", "message" to "Evacuate now.",
        "timestamp" to issued, "expiresAt" to expires, "source" to "authority", "status" to "active",
        "origin" to "authority_web", "hopCount" to "0", "lat" to "27.54", "lng" to "88.56",
        "title" to "CRITICAL — Near Mangan"
    )

    @Test
    fun fcmRestAndMeshProduceTheSameAlert() {
        val fromFcm = AlertContract.fromFcmData(fcm)!!
        val rest = JsonParser.parseString(
            """{"schemaVersion":1,"alertId":"731bc18e-ca46-4d24-adcf-1d0d2c73fbfb","id":"731bc18e-ca46-4d24-adcf-1d0d2c73fbfb",
              "zoneId":"glc_2754_8856","zoneName":"Near Mangan","level":"critical","severity":"CRITICAL","message":"Evacuate now.",
              "timestamp":"$issued","expiresAt":"$expires","source":"authority","status":"active","origin":"authority_web",
              "hopCount":0,"lat":27.54,"lng":88.56,"updatedAt":"$issued"}"""
        ).asJsonObject
        val fromRest = AlertContract.fromJson(rest)!!
        val fromMesh = AlertContract.fromMeshJson(AlertContract.toMeshJson(fromFcm))!!

        assertEquals(fromFcm, fromRest)
        assertEquals(fromFcm, fromMesh)
        assertEquals("731bc18e-ca46-4d24-adcf-1d0d2c73fbfb", fromMesh.alertId)
        assertEquals(Severity.CRITICAL, fromFcm.severity)
        assertEquals("CRITICAL — Near Mangan", fromFcm.title)
        assertEquals(fcm["title"], fromFcm.title)
    }

    @Test
    fun meshAcceptsTheEarlierOfflineAlertFormat() {
        val legacy = """{"alertId":"a1","zoneId":"z","zoneName":"Zone","level":"HIGH","message":"m",
            "timestamp":"${now - 1000}","expiresAt":${now + 86_400_000},"originDeviceId":"Pixel","hopCount":0}"""
        val alert = AlertContract.fromMeshJson(legacy)
        assertNotNull(alert)
        assertEquals("high", alert!!.level)
        assertEquals(now + 86_400_000, alert.expiresAtMillis)
        assertTrue(alert.isRelayable(now))
    }

    @Test
    fun expiryCancellationAndHopLimitStopRelaying() {
        val base = AlertContract.fromFcmData(fcm)!!
        assertTrue(base.isRelayable(now))
        assertFalse(base.copy(expiresAt = AlertContract.isoUtc(now - 1)).isRelayable(now))
        assertFalse(base.copy(status = "cancelled").isRelayable(now))
        assertFalse(base.copy(hopCount = AlertContract.MAX_MESH_HOPS).isRelayable(now))
        assertTrue(base.copy(hopCount = AlertContract.MAX_MESH_HOPS - 1).isRelayable(now))
    }

    @Test
    fun invalidPayloadsAreRejected() {
        assertNull(AlertContract.fromFcmData(fcm - "alertId"))
        assertNull(AlertContract.fromFcmData(fcm + ("level" to "extreme")))
        assertNull(AlertContract.fromMeshJson("HELLO LANDGUARD"))
        assertNull(AlertContract.fromMeshJson("{\"level\":\"low\"}"))
    }

    @Test
    fun isoRoundTrip() {
        assertEquals(1_789_740_756_973L, AlertContract.parseIso(AlertContract.isoUtc(1_789_740_756_973L)))
        assertEquals(1_789_740_756_000L, AlertContract.parseIso("2026-09-18T14:12:36Z"))
    }
}
