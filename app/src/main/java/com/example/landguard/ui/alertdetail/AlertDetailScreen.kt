package com.example.landguard.ui.alertdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

/** Screen 4: Alert Detail — full information for a single alert, opened from Home or History. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AlertDetailScreen(
    viewModel: AlertDetailViewModel = hiltViewModel()
) {
    val alert by viewModel.alert.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Alert Detail") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (alert == null) {
                Text("Alert not found or still loading.")
            } else {
                val a = alert!!
                Text(text = a.title)
                Text(text = "Severity: ${a.severity}", modifier = Modifier.padding(top = 8.dp))
                Text(text = "Zone: ${a.zone}", modifier = Modifier.padding(top = 8.dp))
                Text(text = "Reported: ${a.createdAt}", modifier = Modifier.padding(top = 8.dp))
                Text(text = a.description, modifier = Modifier.padding(top = 16.dp))

                // TODO: add "Mark as safe" / share / directions-to-safety actions once backend supports them.
            }
        }
    }
}
