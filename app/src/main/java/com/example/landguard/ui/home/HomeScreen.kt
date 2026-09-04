package com.example.landguard.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.Alert

/**
 * Screen 2: Home — current regional risk and active alerts.
 * This is the app's default landing screen after onboarding.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    onOpenAlert: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenAlertHistory: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Landslide Early Warning") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isOffline) {
                Card(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Offline — showing last known data.",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            } else {
                AlertList(alerts = state.activeAlerts, onOpenAlert = onOpenAlert)
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenMap) { Text("View Risk Map") }
                Button(onClick = onOpenAlertHistory) { Text("Notification History") }
                Button(onClick = onOpenReport) { Text("Report an Observation") }
                Button(onClick = onOpenProfile) { Text("Profile & Settings") }
            }
        }
    }
}

@Composable
private fun AlertList(alerts: List<Alert>, onOpenAlert: (String) -> Unit) {
    LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
        items(alerts) { alert ->
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clickable { onOpenAlert(alert.id) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = alert.title)
                    Text(text = alert.severity.name)
                }
            }
        }
    }
}
