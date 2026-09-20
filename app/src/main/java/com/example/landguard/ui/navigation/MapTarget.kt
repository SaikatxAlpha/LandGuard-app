package com.example.landguard.ui.navigation

import androidx.compose.runtime.Immutable

/**
 * A cross-screen "show this on the map" request (Home → Map, Map → Risk Areas,
 * Alert → Risk Areas, …).
 *
 * [areaId] selects a monitored area when it exists; otherwise the coordinates
 * (e.g. an alert's lat/lng) are shown as a pinned place. [token] makes two
 * requests for the same target distinct.
 */
@Immutable
data class MapTarget(
    val areaId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val label: String?,
    val token: Long = System.nanoTime()
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}
