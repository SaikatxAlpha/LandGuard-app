package com.example.landguard.data.regional

/**
 * Monitoring region: the eight Northeast Indian states.
 *
 * Only the region envelope and state names are defined here; every
 * monitored area is derived at runtime from real data sources.
 */
object NortheastRegion {

    val STATES = listOf(
        "Arunachal Pradesh",
        "Assam",
        "Manipur",
        "Meghalaya",
        "Mizoram",
        "Nagaland",
        "Sikkim",
        "Tripura"
    )

    // Envelope covering all eight states (WGS84).
    const val MIN_LAT = 21.9
    const val MAX_LAT = 29.5
    const val MIN_LNG = 88.0
    const val MAX_LNG = 97.5

    fun contains(lat: Double, lng: Double): Boolean =
        lat in MIN_LAT..MAX_LAT && lng in MIN_LNG..MAX_LNG
}
