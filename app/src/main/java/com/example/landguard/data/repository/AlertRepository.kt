package com.example.landguard.data.repository

import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.domain.model.Severity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface AlertRepository {
    suspend fun refreshActiveAlerts(): Result<List<Alert>>
    fun observeHistory(): Flow<List<Alert>>
    suspend fun updateAlertStatus(alertId: String, newStatus: AlertStatus): Result<Unit>
}

@Singleton
class AlertRepositoryImpl @Inject constructor() : AlertRepository {

    private val alertsState = MutableStateFlow(
        listOf(
            Alert(
                id = "alt_101",
                title = "Critical InSAR slope shift detected",
                description = "Displacement rate reached -28.4 mm/year near inhabited slope.",
                severity = Severity.CRITICAL,
                affectedLocation = "Kalimpong Ridge, West Bengal",
                parcelId = "pcl_01",
                detectedEvent = "Slope Instability Anomaly",
                timestamp = "2 hours ago",
                status = AlertStatus.NEW,
                confidencePercentage = 92,
                sourceProvider = "Sentinel-1 / ALOS-4 SAR InSAR",
                isDemoData = true
            ),
            Alert(
                id = "alt_102",
                title = "Vegetation index drop (NDVI: 0.22)",
                description = "Rapid loss of canopy cover detected along Teesta Gorge cut.",
                severity = Severity.HIGH,
                affectedLocation = "Teesta River Gorge, Sikkim Border",
                parcelId = "pcl_02",
                detectedEvent = "Landslide Scar Formation",
                timestamp = "3 hours ago",
                status = AlertStatus.NEW,
                confidencePercentage = 89,
                sourceProvider = "ESA Copernicus Sentinel-2 MSI",
                isDemoData = true
            ),
            Alert(
                id = "alt_103",
                title = "Soil moisture saturation threshold (84%)",
                description = "Monsoon accumulated rainfall triggered soil saturation risk.",
                severity = Severity.MODERATE,
                affectedLocation = "Darjeeling Observatory Hill",
                parcelId = "pcl_03",
                detectedEvent = "Water Saturation Warning",
                timestamp = "5 hours ago",
                status = AlertStatus.ACKNOWLEDGED,
                confidencePercentage = 81,
                sourceProvider = "Sentinel-2 NDWI / Local Gauge",
                isDemoData = true
            )
        )
    )

    override suspend fun refreshActiveAlerts(): Result<List<Alert>> {
        delay(250)
        return Result.success(alertsState.value)
    }

    override fun observeHistory(): Flow<List<Alert>> = alertsState.asStateFlow()

    override suspend fun updateAlertStatus(alertId: String, newStatus: AlertStatus): Result<Unit> {
        val updated = alertsState.value.map {
            if (it.id == alertId) it.copy(status = newStatus) else it
        }
        alertsState.value = updated
        return Result.success(Unit)
    }
}
