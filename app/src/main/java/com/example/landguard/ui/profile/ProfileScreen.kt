package com.example.landguard.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Screen 7: Profile & Settings — zone selection, notification preferences. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Profile & Settings") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Phone: ${profile.phoneNumber.ifBlank { "Not verified" }}")
            Text(
                text = "Selected zone: ${profile.selectedZone}",
                modifier = Modifier.padding(top = 12.dp)
            )

            // TODO: replace with a proper zone picker (dropdown fed by ZoneRepository) once UX is finalized.

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Push notifications", modifier = Modifier.padding(end = 12.dp))
                Switch(
                    checked = profile.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled
                )
            }
        }
    }
}
