package com.example.landguard.offline

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

class NearbyMeshManager(private val context: Context) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = context.packageName
    private val STRATEGY = Strategy.P2P_CLUSTER
    private val TAG = "LandGuardMesh"

    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints: StateFlow<Set<String>> = _connectedEndpoints.asStateFlow()
    
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "Endpoint discovered")
            Log.i(TAG, "Connection initiated")
            
            try {
                connectionsClient.requestConnection(
                    android.os.Build.MODEL,
                    endpointId,
                    connectionLifecycleCallback
                ).addOnSuccessListener {
                    // Connection request sent
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to request connection to $endpointId", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception requesting connection", e)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.i(TAG, "Discovery: Endpoint lost: $endpointId")
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.i(TAG, "Connection initiated")
            // Accept the connection automatically for this Phase A test
            try {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to accept connection from $endpointId", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception accepting connection", e)
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.i(TAG, "Connection established")
                _connectedEndpoints.value = _connectedEndpoints.value + endpointId
            } else {
                Log.w(TAG, "Connection failed with $endpointId: ${result.status.statusCode}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i(TAG, "Connection disconnected")
            _connectedEndpoints.value = _connectedEndpoints.value - endpointId
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    val message = String(bytes, StandardCharsets.UTF_8)
                    Log.i(TAG, "Message received")
                    if (message == "HELLO LANDGUARD") {
                        Log.i(TAG, "Received HELLO LANDGUARD")
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Optional: Handle transfer progress
        }
    }

    fun startAdvertising() {
        if (_isAdvertising.value) return
        
        try {
            val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
            connectionsClient.startAdvertising(
                android.os.Build.MODEL, // Use device model as name
                SERVICE_ID,
                connectionLifecycleCallback,
                options
            ).addOnSuccessListener {
                Log.i(TAG, "Advertising started")
                _isAdvertising.value = true
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start advertising", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting advertising", e)
        }
    }

    fun stopAdvertising() {
        try {
            connectionsClient.stopAdvertising()
            Log.i(TAG, "Advertising stopped")
            _isAdvertising.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping advertising", e)
        }
    }

    fun startDiscovery() {
        if (_isDiscovering.value) return
        
        try {
            val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
            connectionsClient.startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                options
            ).addOnSuccessListener {
                Log.i(TAG, "Discovery started")
                _isDiscovering.value = true
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start discovery", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting discovery", e)
        }
    }

    fun stopDiscovery() {
        try {
            connectionsClient.stopDiscovery()
            Log.i(TAG, "Discovery stopped")
            _isDiscovering.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping discovery", e)
        }
    }
    
    fun sendTestMessage() {
        val testMessage = "HELLO LANDGUARD"
        val payload = Payload.fromBytes(testMessage.toByteArray(StandardCharsets.UTF_8))
        
        val currentEndpoints = _connectedEndpoints.value
        if (currentEndpoints.isNotEmpty()) {
            try {
                connectionsClient.sendPayload(currentEndpoints.toList(), payload)
                    .addOnSuccessListener {
                        Log.i(TAG, "Message sent")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to send message", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception sending message", e)
            }
        } else {
            Log.w(TAG, "Cannot send message, no endpoints connected")
        }
    }

    fun stopAll() {
        try {
            connectionsClient.stopAllEndpoints()
            Log.i(TAG, "All endpoints disconnected")
            _connectedEndpoints.value = emptySet()
            stopAdvertising()
            stopDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping all", e)
        }
    }
}