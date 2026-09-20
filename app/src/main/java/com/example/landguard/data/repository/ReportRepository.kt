package com.example.landguard.data.repository

import com.example.landguard.data.network.FieldReportRequest
import com.example.landguard.data.network.LandGuardApiService
import com.example.landguard.domain.model.Observation
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

interface ReportRepository {
    suspend fun submitObservation(observation: Observation): Result<Unit>
}

/** Sends a ground observation to the LandGuard backend (authority "Field reports"). */
@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val api: LandGuardApiService
) : ReportRepository {

    override suspend fun submitObservation(observation: Observation): Result<Unit> = try {
        val response = api.submitReport(
            FieldReportRequest(
                zoneId = observation.zone.takeIf { it.isNotBlank() },
                zoneName = observation.zoneName.takeIf { it.isNotBlank() },
                note = observation.description,
                lat = observation.latitude,
                lng = observation.longitude
            )
        )
        response.body()?.close()
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(IllegalStateException("LandGuard server rejected the report (HTTP ${response.code()})"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(IllegalStateException("Could not reach the LandGuard server. Check your connection and try again.", e))
    }
}
