package com.example.landguard.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/*
 * Device location for the map-first UI.
 *
 * Uses the platform LocationManager so it works on every device
 * (with or without Google Play Services) and never depends on a
 * developer-specific configuration.
 */

@Immutable
data class UserLocation(
    val latitude: Double,
    val longitude: Double
)

enum class LocationStatus {
    /** Permission not requested yet or request in flight. */
    PENDING,
    /** Permission granted, waiting for a fix. */
    LOCATING,
    /** A fix is available. */
    READY,
    /** Permission denied by the user. */
    DENIED,
    /** Permission granted but no provider could supply a fix. */
    UNAVAILABLE
}

@Immutable
data class UserLocationState(
    val location: UserLocation? = null,
    val status: LocationStatus = LocationStatus.PENDING,
    val placeLabel: String? = null
)

/** Provided once at the root so every screen shares the same fix. */
val LocalUserLocation = staticCompositionLocalOf { UserLocationState() }

/** Asks the root to re-request permission / refresh the fix. */
val LocalRequestUserLocation = staticCompositionLocalOf<() -> Unit> { {} }

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

private const val FRESH_FIX_AGE_MS = 10 * 60 * 1000L
private const val FIX_TIMEOUT_MS = 12_000L

/**
 * Returns a recent last-known location when one exists, otherwise waits
 * (bounded) for a single fresh fix from any enabled provider.
 */
@SuppressLint("MissingPermission")
suspend fun fetchUserLocation(context: Context): UserLocation? {
    if (!hasLocationPermission(context)) return null

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null

    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    ).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

    val lastKnown = providers
        .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }

    if (lastKnown != null && System.currentTimeMillis() - lastKnown.time < FRESH_FIX_AGE_MS) {
        return lastKnown.toUserLocation()
    }

    val liveProviders = providers.filter { it != LocationManager.PASSIVE_PROVIDER }
    if (liveProviders.isEmpty()) return lastKnown?.toUserLocation()

    val fresh = withTimeoutOrNull(FIX_TIMEOUT_MS) {
        suspendCancellableCoroutine<Location?> { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { manager.removeUpdates(this) }
                    if (continuation.isActive) continuation.resume(location)
                }

                // Overridden explicitly: these are abstract before API 30.
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }

            liveProviders.forEach { provider ->
                runCatching {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }
            }

            continuation.invokeOnCancellation {
                runCatching { manager.removeUpdates(listener) }
            }
        }
    }

    return (fresh ?: lastKnown)?.toUserLocation()
}

private fun Location.toUserLocation() = UserLocation(latitude, longitude)

/** Great-circle distance in kilometres. */
fun distanceKm(from: UserLocation, lat: Double, lng: Double): Double {
    val results = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, lat, lng, results)
    return results[0] / 1000.0
}

fun formatDistance(km: Double): String = when {
    km < 1.0 -> "${(km * 1000).toInt()} m"
    km < 10.0 -> String.format("%.1f km", km)
    else -> "${km.toInt()} km"
}
