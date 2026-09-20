package com.example.landguard.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.landguard.data.repository.AlertRepository
import com.example.landguard.domain.model.Alert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertHistoryViewModel @Inject constructor(
    private val repository: AlertRepository
) : ViewModel() {

    val history: StateFlow<List<Alert>> = repository.observeHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Pulls alerts from the LandGuard backend now (e.g. a notification's alert is not on the device yet). */
    fun syncNow() {
        viewModelScope.launch { repository.refreshActiveAlerts() }
    }
}
