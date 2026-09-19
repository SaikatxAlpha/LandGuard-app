package com.example.landguard.data.alerts

import com.example.landguard.domain.model.Severity
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The ONE LandGuard alert structure — identical field names and meaning on the
 * backend (backend/services/alert-contract.js), the authority web, FCM data
 * messages, Room and the Nearby Connections offline mesh.
 *
 * alertId is never regenerated on any hop; hopCount grows by one per mesh relay.
 */
data class AlertDto(
    val schemaVersion: Int? = 1,
    val alertId: String,
    val zoneId: String,
    val zoneName: String,
    /** low | moderate | high | critical */
    val level: String,
    val message: String,
    /** ISO-8601 UTC issue time. */
    val timestamp: String,
    /** ISO-8601 UTC expiry. */
    val expiresAt: String,
    /** authority | risk_engine */
    val source: String = "authority",
    /** active | cancelled | expired */
    val status: String = "active",
    /** authority_web | api */
    val origin: String = "api",
    val hopCount: Int = 0,
    val lat: Double? = null,
    val lng: Double? = null,
    /** Mesh only: the device that relayed this copy. */
    val originDeviceId: String? = null
) {
    val severity: Severity
        get() = runCatching { Severity.valueOf(level.uppercase(Locale.US)) }.getOrDefault(Severity.MODERATE)

    /** Same title the backend, web preview and notifications use. */
    val title: String get() = "${level.uppercase(Locale.US)} — $zoneName"

    val expiresAtMillis: Long? get() = AlertContract.parseIso(expiresAt)

    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
        expiresAtMillis?.let { it <= nowMillis } ?: false

    fun isRelayable(nowMillis: Long = System.currentTimeMillis()): Boolean =
        status == "active" && !isExpired(nowMillis) && hopCount < AlertContract.MAX_MESH_HOPS
}

object AlertContract {
    const val SCHEMA_VERSION = 1
    /** Mirrors MAX_MESH_HOPS on the backend. */
    const val MAX_MESH_HOPS = 6
    private const val DEFAULT_TTL_MS = 24L * 60 * 60 * 1000
    private val LEVELS = setOf("low", "moderate", "high", "critical")
    private val gson = Gson()

    // java.time needs API 26 (minSdk is 24), so ISO-8601 is handled by hand.
    fun isoUtc(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(millis))

    fun parseIso(value: String?): Long? = runCatching {
        if (value.isNullOrBlank()) return null
        val base = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(value.take(19))!!.time
        val fraction = value.drop(19).removePrefix(".").takeWhile { it.isDigit() }
        base + if (fraction.isEmpty()) 0L else fraction.padEnd(3, '0').take(3).toLong()
    }.getOrNull()

    /** FCM data payload (all strings) → canonical alert. Returns null if it is not a valid alert. */
    fun fromFcmData(data: Map<String, String>): AlertDto? {
        val alertId = data["alertId"]?.takeIf { it.isNotBlank() } ?: return null
        val level = data["level"]?.lowercase(Locale.US)?.takeIf { it in LEVELS } ?: return null
        val now = System.currentTimeMillis()
        val timestamp = data["timestamp"]?.takeIf { parseIso(it) != null } ?: isoUtc(now)
        return AlertDto(
            schemaVersion = data["schemaVersion"]?.toIntOrNull() ?: SCHEMA_VERSION,
            alertId = alertId,
            zoneId = data["zoneId"].orEmpty(),
            zoneName = data["zoneName"]?.takeIf { it.isNotBlank() } ?: "LandGuard area",
            level = level,
            message = data["message"] ?: data["body"].orEmpty(),
            timestamp = timestamp,
            expiresAt = data["expiresAt"]?.takeIf { parseIso(it) != null } ?: isoUtc((parseIso(timestamp) ?: now) + DEFAULT_TTL_MS),
            source = data["source"] ?: "authority",
            status = data["status"] ?: "active",
            origin = data["origin"] ?: "api",
            hopCount = data["hopCount"]?.toIntOrNull() ?: 0,
            lat = data["lat"]?.toDoubleOrNull(),
            lng = data["lng"]?.toDoubleOrNull()
        )
    }

    /** Mesh wire format = the canonical JSON. */
    fun toMeshJson(alert: AlertDto): String = gson.toJson(alert)

    /**
     * Parses a mesh payload. Also accepts the earlier OfflineAlert format
     * (millisecond timestamps) so older app builds stay interoperable.
     */
    fun fromMeshJson(json: String): AlertDto? =
        runCatching { fromJson(JsonParser.parseString(json).asJsonObject) }.getOrNull()

    /** Tolerant parser used for REST responses and mesh payloads alike. */
    fun fromJson(o: JsonObject): AlertDto? = runCatching {
        val alertId = o.str("alertId") ?: o.str("id") ?: return null
        val level = o.str("level")?.lowercase(Locale.US)?.takeIf { it in LEVELS } ?: return null
        val now = System.currentTimeMillis()
        val timestamp = o.get("timestamp").isoOrMillis() ?: isoUtc(now)
        AlertDto(
            schemaVersion = o.get("schemaVersion")?.takeIf { it.isJsonPrimitive }?.asInt ?: SCHEMA_VERSION,
            alertId = alertId,
            zoneId = o.str("zoneId").orEmpty(),
            zoneName = o.str("zoneName") ?: "LandGuard area",
            level = level,
            message = o.str("message").orEmpty(),
            timestamp = timestamp,
            expiresAt = o.get("expiresAt").isoOrMillis() ?: isoUtc((parseIso(timestamp) ?: now) + DEFAULT_TTL_MS),
            source = o.str("source") ?: "authority",
            status = o.str("status") ?: "active",
            origin = o.str("origin") ?: "api",
            hopCount = o.get("hopCount")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
            lat = o.get("lat")?.takeIf { it.isJsonPrimitive }?.asDouble,
            lng = o.get("lng")?.takeIf { it.isJsonPrimitive }?.asDouble,
            originDeviceId = o.str("originDeviceId")
        )
    }.getOrNull()

    private fun JsonObject.str(name: String): String? =
        get(name)?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }

    /** ISO string, or legacy epoch millis (number or numeric string). */
    private fun JsonElement?.isoOrMillis(): String? {
        if (this == null || isJsonNull || !isJsonPrimitive) return null
        val p = asJsonPrimitive
        if (p.isNumber) return isoUtc(p.asLong)
        val s = p.asString
        s.toLongOrNull()?.let { return isoUtc(it) }
        return s.takeIf { parseIso(it) != null }
    }
}
