package com.example.landguard.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.landguard.data.repository.ProfileRepository
import com.example.landguard.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    val profile: StateFlow<UserProfile> = repository.observeProfile()

    fun setZone(zone: String) {
        viewModelScope.launch {
            repository.updateProfile(profile.value.copy(selectedZone = zone))
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateProfile(profile.value.copy(notificationsEnabled = enabled))
        }
    }
}
