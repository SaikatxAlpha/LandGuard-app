package com.sih.landslide.ui.alerts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Screen 6: Notification history — all past alerts the device has received. */
@Composable
fun AlertHistoryScreen(
    onOpenAlert: (String) -> Unit,
    viewModel: AlertHistoryViewModel = hiltViewModel()
) {
    val alerts by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Notification History") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(alerts) { alert ->
                ListItem(
                    headlineContent = { Text(alert.title) },
                    supportingContent = { Text("${alert.severity} • ${alert.createdAt}") },
                    modifier = Modifier.padding(4.dp = androidx.compose.ui.unit.dp)
                )
            }
        }
    }
}