package com.example.landguard.offline

import com.example.landguard.data.alerts.AlertDto

/**
 * The offline mesh carries the canonical LandGuard alert unchanged (same
 * alertId, expiry and status as the backend); only hopCount grows per relay.
 * Encoding/decoding lives in [com.example.landguard.data.alerts.AlertContract].
 */
typealias OfflineAlert = AlertDto
