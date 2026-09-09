package com.example.landguard.data.repository

import com.example.landguard.domain.model.LandCoverBreakdown
import com.example.landguard.domain.model.LandParcel
import com.example.landguard.domain.model.Severity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface ParcelRepository {
    fun observeParcels(): Flow<List<LandParcel>>
    fun observeLandCoverBreakdown(): Flow<LandCoverBreakdown>
    suspend fun getParcelById(id: String): Result<LandParcel>
    suspend fun refreshParcels(): Result<Unit>
}

@Singleton
class ParcelRepositoryImpl @Inject constructor() : ParcelRepository {

    private val sampleParcels = listOf(
        LandParcel(
            id = "pcl_01",
            name = "Kalimpong Slope - Sector 04",
            villageOrDistrict = "Kalimpong Ridge",
            stateName = "West Bengal",
            latitude = 27.0600,
            longitude = 88.4700,
            areaHectares = 142.5,
            landType = "Forest",
            riskScore = 82,
            riskCategory = Severity.HIGH,
            lastUpdated = "2 hours ago",
            isDemoData = true
        ),
        LandParcel(
            id = "pcl_02",
            name = "Teesta River Valley Cut",
            villageOrDistrict = "Teesta Gorge",
            stateName = "Sikkim Border",
            latitude = 26.9820,
            longitude = 88.4210,
            areaHectares = 210.8,
            landType = "Water / Terrace",
            riskScore = 91,
            riskCategory = Severity.CRITICAL,
            lastUpdated = "1 hour ago",
            isDemoData = true
        ),
        LandParcel(
            id = "pcl_03",
            name = "Observatory Hill Ridge",
            villageOrDistrict = "Darjeeling Central",
            stateName = "West Bengal",
            latitude = 27.0410,
            longitude = 88.2630,
            areaHectares = 88.2,
            landType = "Urban/Built-up",
            riskScore = 63,
            riskCategory = Severity.MODERATE,
            lastUpdated = "3 hours ago",
            isDemoData = true
        ),
        LandParcel(
            id = "pcl_04",
            name = "Siliguri Foothills Basin",
            villageOrDistrict = "Siliguri Bypass",
            stateName = "West Bengal",
            latitude = 26.7270,
            longitude = 88.3950,
            areaHectares = 320.0,
            landType = "Agricultural",
            riskScore = 18,
            riskCategory = Severity.LOW,
            lastUpdated = "5 hours ago",
            isDemoData = true
        )
    )

    private val breakdown = LandCoverBreakdown(
        agriculturalPercentage = 42,
        forestPercentage = 35,
        urbanPercentage = 12,
        waterPercentage = 8,
        otherPercentage = 3
    )

    override fun observeParcels(): Flow<List<LandParcel>> = MutableStateFlow(sampleParcels).asStateFlow()

    override fun observeLandCoverBreakdown(): Flow<LandCoverBreakdown> = MutableStateFlow(breakdown).asStateFlow()

    override suspend fun getParcelById(id: String): Result<LandParcel> {
        val parcel = sampleParcels.find { it.id == id } ?: sampleParcels.first()
        return Result.success(parcel)
    }

    override suspend fun refreshParcels(): Result<Unit> {
        delay(300)
        return Result.success(Unit)
    }
}
