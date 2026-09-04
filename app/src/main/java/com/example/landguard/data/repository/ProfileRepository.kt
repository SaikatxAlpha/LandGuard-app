package com.example.landguard.data.repository

import com.example.landguard.domain.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface ProfileRepository {
    fun observeProfile(): StateFlow<UserProfile>
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
}

@Singleton
class ProfileRepositoryImpl @Inject constructor() : ProfileRepository {

    // TODO: back this with DataStore / backend once auth contract is fixed.
    private val _profile = MutableStateFlow(
        UserProfile(phoneNumber = "", selectedZone = "Zone A", notificationsEnabled = true)
    )

    override fun observeProfile(): StateFlow<UserProfile> = _profile.asStateFlow()

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            delay(200) // simulate network/storage write
            _profile.value = profile
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
