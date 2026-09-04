package com.example.landguard.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.landguard.domain.model.Zone

/**
 * Screen 3: Risk Map — list-based view of zone risk levels.
 * TODO: swap for an actual map (e.g. Google Maps Compose) with colored risk overlays
 * once a maps API key / SDK dependency is added to the project.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MapScreen(
    onOpenZone: (String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Risk Map") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            } else if (state.errorMessage != null) {
                Text(
                    text = "Could not load zones: ${state.errorMessage}",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                ZoneList(zones = state.zones, onOpenZone = onOpenZone)
            }
        }
    }
}

@Composable
private fun ZoneList(zones: List<Zone>, onOpenZone: (String) -> Unit) {
    LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
        items(zones, key = { it.id }) { zone ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = zone.name)
                    Text(text = "Risk: ${zone.riskLevel}")
                    Text(text = "Updated: ${zone.lastUpdated}")
                }
            }
        }
    }
}
