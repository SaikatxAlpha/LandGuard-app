package com.example.landguard.ui.alertdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.landguard.data.repository.AlertRepository
import com.example.landguard.domain.model.Alert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AlertRepository
) : ViewModel() {

    private val alertId: String = savedStateHandle.get<String>("alertId") ?: ""

    private val _alert = MutableStateFlow<Alert?>(null)
    val alert: StateFlow<Alert?> = _alert.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeHistory().collect { alerts ->
                _alert.value = alerts.firstOrNull { it.id == alertId }
            }
        }
    }
}
