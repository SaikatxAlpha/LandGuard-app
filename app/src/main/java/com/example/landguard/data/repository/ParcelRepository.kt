// app/src/main/java/com/example/landguard/data/repository/ParcelRepository.kt

package com.example.landguard.data.repository

import com.example.landguard.domain.model.LandCoverBreakdown
import com.example.landguard.domain.model.LandParcel
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

/**
 * User land parcels. The LandGuard backend has no parcel registry yet, so no
 * parcels exist: the list is empty rather than filled with invented parcels,
 * risk scores or timestamps.
 */
@Singleton
class ParcelRepositoryImpl @Inject constructor() : ParcelRepository {

    private val parcelsFlow = MutableStateFlow<List<LandParcel>>(emptyList())

    override fun observeParcels(): Flow<List<LandParcel>> = parcelsFlow.asStateFlow()

    /** No land-cover source is connected; all shares are zero (data unavailable). */
    override fun observeLandCoverBreakdown(): Flow<LandCoverBreakdown> =
        MutableStateFlow(LandCoverBreakdown()).asStateFlow()

    override suspend fun getParcelById(id: String): Result<LandParcel> =
        parcelsFlow.value.find { it.id == id }?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Parcel $id not found"))

    override suspend fun refreshParcels(): Result<Unit> = Result.success(Unit)
}
