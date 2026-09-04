package com.example.landguard.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Screen 1: Onboarding / registration / phone verification. */
@Composable
fun OnboardingScreen(
    onVerified: () -> Unit,
    @Suppress("UNUSED_PARAMETER")
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpRequested by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Welcome — verify your phone to receive zone-based landslide alerts.")

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone number") },
            modifier = Modifier.padding(top = 16.dp)
        )

        if (!otpRequested) {
            Button(
                onClick = { otpRequested = true /* TODO: call auth/otp request use case */ },
                modifier = Modifier.padding(top = 16.dp)
            ) { Text("Send OTP") }
        } else {
            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("Enter OTP") },
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                onClick = onVerified, // TODO: verify OTP via backend, register FCM token + zone selection
                modifier = Modifier.padding(top = 16.dp)
            ) { Text("Verify & Continue") }
        }
    }
}
