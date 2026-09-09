package com.example.landguard.data.repository

import com.example.landguard.domain.model.Observation
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

interface ReportRepository {
    suspend fun submitObservation(observation: Observation): Result<Unit>
}

@Singleton
class ReportRepositoryImpl @Inject constructor() : ReportRepository {

    // TODO: replace with the backend submission endpoint once its contract is available.
    override suspend fun submitObservation(observation: Observation): Result<Unit> = try {
        delay(300) // Simulate sending the report.
        Result.success(Unit)
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}
