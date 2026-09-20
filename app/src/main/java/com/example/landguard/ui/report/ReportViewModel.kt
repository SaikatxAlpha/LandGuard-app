package com.example.landguard.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.landguard.data.repository.ReportRepository
import com.example.landguard.domain.model.Observation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val description: String = "",
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String? = null
)

/** Where the observation was made: the device GPS fix and the nearest monitored area. */
data class ReportPlace(
    val latitude: Double?,
    val longitude: Double?,
    val zoneId: String?,
    val zoneName: String?
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun onDescriptionChange(text: String) {
        _uiState.value = _uiState.value.copy(description = text, errorMessage = null)
    }

    fun submit(place: ReportPlace) {
        val description = _uiState.value.description.trim()
        if (description.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Describe what you observed before sending.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            repository.submitObservation(
                Observation(
                    description = description,
                    zone = place.zoneId.orEmpty(),
                    zoneName = place.zoneName.orEmpty(),
                    latitude = place.latitude,
                    longitude = place.longitude
                )
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitSuccess = true,
                        description = ""
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Something went wrong"
                    )
                }
        }
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(submitSuccess = false)
    }
}
