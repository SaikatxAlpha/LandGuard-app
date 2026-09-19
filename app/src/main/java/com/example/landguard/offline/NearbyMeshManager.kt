package com.example.landguard.offline

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.landguard.data.alerts.AlertContract
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/** A canonical alert received from a nearby device. */
data class MeshAlert(val fromEndpointId: String, val alert: OfflineAlert)

/**
 * Nearby Connections transport for the offline disaster channel. It only
 * moves canonical alerts between phones; storage, de-duplication, expiry,
 * notifications and onward relaying are handled by AlertIngestor.
 */
@Singleton
class NearbyMeshManager @Inject constructor(@ApplicationContext private val context: Context) {

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

    private val _incomingAlerts = MutableSharedFlow<MeshAlert>(extraBufferCapacity = 64)
    /** Alerts received from nearby devices (not yet de-duplicated). */
    val incomingAlerts: SharedFlow<MeshAlert> = _incomingAlerts.asSharedFlow()

    private val _peerConnected = MutableSharedFlow<String>(extraBufferCapacity = 16)
    /** Emits an endpoint id whenever a new peer connects (store-and-forward trigger). */
    val peerConnected: SharedFlow<String> = _peerConnected.asSharedFlow()

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "Endpoint discovered: $endpointId")
            try {
                connectionsClient.requestConnection(Build.MODEL, endpointId, connectionLifecycleCallback)
                    .addOnFailureListener { e -> Log.w(TAG, "Connection request to $endpointId failed: ${e.message}") }
            } catch (e: Exception) {
                Log.e(TAG, "Exception requesting connection", e)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.i(TAG, "Endpoint lost: $endpointId")
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            try {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
                    .addOnFailureListener { e -> Log.e(TAG, "Failed to accept connection from $endpointId", e) }
            } catch (e: Exception) {
                Log.e(TAG, "Exception accepting connection", e)
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.i(TAG, "Connection established with $endpointId")
                _connectedEndpoints.value = _connectedEndpoints.value + endpointId
                _peerConnected.tryEmit(endpointId)
            } else {
                Log.w(TAG, "Connection failed with $endpointId: ${result.status.statusCode}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i(TAG, "Connection disconnected: $endpointId")
            _connectedEndpoints.value = _connectedEndpoints.value - endpointId
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val message = payload.asBytes()?.toString(StandardCharsets.UTF_8) ?: return
            if (message == "HELLO LANDGUARD") {
                Log.i(TAG, "Received HELLO LANDGUARD from $endpointId")
                return
            }
            val alert = AlertContract.fromMeshJson(message)
            if (alert == null) {
                Log.w(TAG, "Ignoring unrecognised mesh payload from $endpointId")
                return
            }
            Log.i(TAG, "Offline alert ${alert.alertId} received from $endpointId (hop ${alert.hopCount})")
            _incomingAlerts.tryEmit(MeshAlert(endpointId, alert))
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    /** Runtime permissions Nearby Connections needs on this Android version. */
    fun requiredPermissions(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    fun missingPermissions(): List<String> = requiredPermissions().filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    /** Starts advertising + discovery once the permissions are granted; safe to call repeatedly. */
    fun startIfPermitted(): Boolean {
        if (missingPermissions().isNotEmpty()) {
            Log.i(TAG, "Offline mesh waiting for permissions: ${missingPermissions()}")
            return false
        }
        startAdvertising()
        startDiscovery()
        return true
    }

    fun startAdvertising() {
        if (_isAdvertising.value) return
        try {
            val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
            connectionsClient.startAdvertising(Build.MODEL, SERVICE_ID, connectionLifecycleCallback, options)
                .addOnSuccessListener {
                    Log.i(TAG, "Advertising started")
                    _isAdvertising.value = true
                }
                .addOnFailureListener { e -> Log.e(TAG, "Failed to start advertising", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting advertising", e)
        }
    }

    fun stopAdvertising() {
        try {
            connectionsClient.stopAdvertising()
            _isAdvertising.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping advertising", e)
        }
    }

    fun startDiscovery() {
        if (_isDiscovering.value) return
        try {
            val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
            connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
                .addOnSuccessListener {
                    Log.i(TAG, "Discovery started")
                    _isDiscovering.value = true
                }
                .addOnFailureListener { e -> Log.e(TAG, "Failed to start discovery", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting discovery", e)
        }
    }

    fun stopDiscovery() {
        try {
            connectionsClient.stopDiscovery()
            _isDiscovering.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping discovery", e)
        }
    }

    fun sendTestMessage() {
        send(_connectedEndpoints.value.toList(), "HELLO LANDGUARD")
    }

    /** Sends a canonical alert to every connected peer except [excludeEndpointId]. */
    fun relay(alert: OfflineAlert, excludeEndpointId: String? = null): Int {
        val targets = _connectedEndpoints.value.filter { it != excludeEndpointId }
        if (targets.isEmpty()) {
            Log.i(TAG, "No nearby peers for alert ${alert.alertId}")
            return 0
        }
        send(targets, AlertContract.toMeshJson(alert))
        Log.i(TAG, "Alert ${alert.alertId} relayed to ${targets.size} peer(s) at hop ${alert.hopCount}")
        return targets.size
    }

    fun sendTo(endpointId: String, alert: OfflineAlert) {
        send(listOf(endpointId), AlertContract.toMeshJson(alert))
    }

    private fun send(endpoints: List<String>, text: String) {
        if (endpoints.isEmpty()) return
        try {
            connectionsClient.sendPayload(endpoints, Payload.fromBytes(text.toByteArray(StandardCharsets.UTF_8)))
                .addOnFailureListener { e -> Log.e(TAG, "Failed to send mesh payload", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending mesh payload", e)
        }
    }

    fun stopAll() {
        try {
            connectionsClient.stopAllEndpoints()
            _connectedEndpoints.value = emptySet()
            stopAdvertising()
            stopDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping all", e)
        }
    }
}
