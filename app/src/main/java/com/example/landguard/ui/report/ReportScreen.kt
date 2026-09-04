package com.example.landguard.ui.report

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Screen 5: Report an Observation — lets a user submit a ground-level landslide sighting. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ReportScreen(
    onSubmitted: () -> Unit = {},
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.submitSuccess) {
        if (state.submitSuccess) {
            viewModel.consumeSuccess()
            onSubmitted()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Report an Observation") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Describe what you're seeing — cracks in the ground, unusual water flow, leaning trees, etc.")

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )

            // TODO: add photo capture (CameraX) and GPS location attachment once permissions are wired.

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage ?: "",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            } else {
                Button(
                    onClick = viewModel::submit,
                    modifier = Modifier.padding(top = 16.dp)
                ) { Text("Submit Report") }
            }
        }
    }
}
