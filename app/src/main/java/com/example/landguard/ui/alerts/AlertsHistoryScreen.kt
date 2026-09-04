package com.example.landguard.ui.alerts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Screen 6: Notification history — all past alerts the device has received. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AlertHistoryScreen(
    onOpenAlert: (String) -> Unit,
    viewModel: AlertHistoryViewModel = hiltViewModel()
) {
    val alerts by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Notification History") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(alerts, key = { it.id }) { alert ->
                ListItem(
                    headlineContent = { Text(alert.title) },
                    supportingContent = { Text("${alert.severity} • ${alert.createdAt}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { onOpenAlert(alert.id) }
                )
            }
        }
    }
}
