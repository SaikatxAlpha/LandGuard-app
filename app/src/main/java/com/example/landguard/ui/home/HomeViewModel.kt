package com.example.landguard.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.landguard.data.repository.AlertRepository
import example.landguard.domain.model.Alert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeAlerts: List<Alert> = emptyList(),
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.refreshActiveAlerts()
                .onSuccess { alerts ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeAlerts = alerts,
                        isOffline = false
                    )
                }
                .onFailure { throwable ->
                    // Network unavailable: fall back to cached state and surface staleness,
                    // per the platform's offline/degraded-mode rules.
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOffline = true,
                        errorMessage = throwable.message
                    )
                }
        }
    }
}