package com.example.landguard.offline

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TestMeshActivity : ComponentActivity() {

    @Inject
    lateinit var meshManager: NearbyMeshManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Permissions granted for Mesh", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions denied for Mesh", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TestMeshScreen(meshManager, this@TestMeshActivity)
                }
            }
        }

        requestMeshPermissions()
    }

    private fun requestMeshPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager.stopAll()
    }
}

@Composable
fun TestMeshScreen(meshManager: NearbyMeshManager, context: Context) {
    val isAdvertising by meshManager.isAdvertising.collectAsState()
    val isDiscovering by meshManager.isDiscovering.collectAsState()
    val connectedEndpoints by meshManager.connectedEndpoints.collectAsState()

    val prefs: SharedPreferences = context.getSharedPreferences("LandGuardNetworkPrefs", Context.MODE_PRIVATE)
    var baseUrl by remember { mutableStateOf(prefs.getString("base_url", "") ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("LandGuard Developer Test", style = MaterialTheme.typography.titleLarge)
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("API Base URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                prefs.edit().putString("base_url", baseUrl).apply()
                Toast.makeText(context, "Base URL updated. Restart app to re-register.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save API URL")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Offline Mesh", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (isAdvertising) meshManager.stopAdvertising()
                else meshManager.startAdvertising()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isAdvertising) "Stop Advertising" else "Start Advertising")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (isDiscovering) meshManager.stopDiscovery()
                else meshManager.startDiscovery()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isDiscovering) "Stop Discovery" else "Start Discovery")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                meshManager.sendTestMessage()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = connectedEndpoints.isNotEmpty()
        ) {
            Text("Send 'HELLO LANDGUARD'")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                meshManager.stopAll()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Disconnect All")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Connected Endpoints: ${connectedEndpoints.size}")
        connectedEndpoints.forEach { endpoint ->
            Text("- $endpoint")
        }
    }
}