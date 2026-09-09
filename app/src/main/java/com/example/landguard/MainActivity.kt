package com.example.landguard

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.landguard.ui.home.HomeScreen
import com.example.landguard.ui.map.MapScreen
import com.example.landguard.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private data class AlertItem(val title: String, val area: String, val time: String, val severity: String, val score: Int)
private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Overview", Icons.Filled.Analytics),
    MAP("Risk Map", Icons.Filled.Radar),
    ALERTS("Alerts", Icons.Filled.Warning),
    PROFILE("Settings", Icons.Filled.Settings)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LandGuardTheme { LandGuardAppUI() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LandGuardAppUI() {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var showLocationSheet by remember { mutableStateOf(false) }
    var selectedAlert by remember { mutableStateOf<AlertItem?>(null) }
    var notifications by remember { mutableStateOf(true) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Toast.makeText(context, if (granted) "GPS Locked" else "GPS Denied", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            if (tab != Tab.MAP) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SoftMint), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Landscape, null, tint = ForestPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LandGuard", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextCharcoal)
                        Text("INTELLIGENT LAND RISK PLATFORM", fontSize = 9.sp, letterSpacing = 1.2.sp, color = ForestPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    BadgedBox(badge = { if (notifications) Badge(containerColor = RiskCriticalRed) { Text("3", color = Color.White) } }) {
                        IconButton(onClick = { tab = Tab.ALERTS }) { Icon(Icons.Filled.Notifications, "Alerts", tint = TextCharcoal) }
                    }
                    IconButton(onClick = { showLocationSheet = true }) { Icon(Icons.Filled.GpsFixed, "Location", tint = ForestPrimary) }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(if(tab != Tab.MAP) padding else androidx.compose.foundation.layout.PaddingValues(0.dp))) {
            
            // Main Content
            when (tab) {
                Tab.HOME -> HomeScreen(
                    onOpenAlert = { id -> scope.launch { snackbar.showSnackbar("Alert $id opened") } },
                    onOpenMap = { tab = Tab.MAP },
                    onOpenAlertHistory = { tab = Tab.ALERTS },
                    onOpenProfile = { tab = Tab.PROFILE }
                )
                Tab.MAP -> MapScreen(onOpenZone = { tab = Tab.HOME })
                Tab.ALERTS -> AlertsScreen(onAlert = { selectedAlert = it })
                Tab.PROFILE -> ProfileScreen(
                    notifications = notifications,
                    onNotifications = { notifications = it },
                    requestLocation = { locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    onOpenSatelliteApi = { tab = Tab.MAP }
                )
            }

            // Custom Floating Equal-Weight Navigation Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(0.92f)
                    .height(62.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(CardSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Tab.entries.forEach { item ->
                        val isSelected = tab == item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isSelected) SoftMint else Color.Transparent)
                                .clickable { tab = item },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) ForestPrimary else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.label,
                                        color = ForestDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLocationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardSurface
        ) {
            Column(Modifier.padding(24.dp).padding(bottom = 24.dp)) {
                Text("Select Monitored Location", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
                Spacer(Modifier.height(4.dp))
                Text("Target regional coordinates for land risk and satellite observation analysis.", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { showLocationSheet = false; locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.GpsFixed, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("USE DEVICE GPS", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                listOf("Kalimpong Ridge, West Bengal", "Teesta River Gorge, Sikkim", "Siliguri Bypass Basin").forEach { place ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftMintContainer)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { showLocationSheet = false; scope.launch { snackbar.showSnackbar("Target locked: $place") } }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(place, color = TextCharcoal, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ChevronRight, null, tint = ForestPrimary)
                        }
                    }
                }
            }
        }
    }

    selectedAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = { selectedAlert = null },
            containerColor = CardSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = RiskCriticalRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LAND RISK ALERT", color = RiskCriticalRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(alert.title, color = TextCharcoal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(alert.area, color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("RISK CONFIDENCE: ${alert.score}%", color = ForestPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { alert.score / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = RiskCriticalRed, trackColor = BorderSubtle)
                }
            },
            confirmButton = {
                Button(onClick = { selectedAlert = null; scope.launch { snackbar.showSnackbar("Alert acknowledged") } }, colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary)) {
                    Text("ACKNOWLEDGE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAlert = null }) {
                    Text("CLOSE", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun AlertsScreen(onAlert: (AlertItem) -> Unit) {
    val list = listOf(
        AlertItem("Critical InSAR slope shift detected", "Kalimpong Ridge (-28.4mm/y)", "2 hours ago", "HIGH", 92),
        AlertItem("Vegetation index drop (NDVI: 0.22)", "Teesta River Gorge", "3 hours ago", "HIGH", 89),
        AlertItem("Soil moisture threshold crossed (84%)", "Darjeeling Observatory Hill", "5 hours ago", "MODERATE", 57),
        AlertItem("All clear - Stable terrace", "Siliguri Bypass Basin", "12 hours ago", "LOW", 18)
    )
    var criticalOnly by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(LightBackground)) {
        Column(Modifier.padding(20.dp)) { 
            Text("Active Risk Alerts", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Text("Show high & critical events only", modifier = Modifier.weight(1f), color = TextMuted, fontSize = 13.sp)
                Switch(criticalOnly, { criticalOnly = it }) 
            } 
        }
        HorizontalDivider(color = BorderSubtle)
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { 
            items(list.filter { !criticalOnly || it.severity == "HIGH" }) { AlertCard(it) { onAlert(it) } }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun AlertCard(alert: AlertItem, onClick: () -> Unit) {
    val severityColor = when (alert.severity) {
        "HIGH", "CRITICAL" -> RiskCriticalRed
        "MODERATE" -> RiskWarningAmber
        else -> RiskLowGreen
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(severityColor))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(alert.title, color = TextCharcoal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text("${alert.area} • ${alert.time}", color = TextMuted, fontSize = 11.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
        }
    }
}

@Composable
private fun ProfileScreen(
    notifications: Boolean,
    onNotifications: (Boolean) -> Unit,
    requestLocation: () -> Unit,
    onOpenSatelliteApi: () -> Unit
) {
    var safety by remember { mutableStateOf(true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LightBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("System & Operator Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextCharcoal); Text("Configure LandGuard monitoring parameters.", color = TextMuted, fontSize = 13.sp) }
        
        item { 
            Card(
                colors = CardDefaults.cardColors(containerColor = SoftMintContainer),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftMint)
            ) { 
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { 
                    Box(Modifier.size(52.dp).clip(CircleShape).background(ForestPrimary), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Person, null, tint = Color.White) }
                    Spacer(Modifier.width(16.dp))
                    Column { Text("Environmental GIS Specialist", fontWeight = FontWeight.Bold, color = TextCharcoal, fontSize = 15.sp); Text("Land Monitoring Operator", color = ForestPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } 
                } 
            } 
        }
        
        item { Text("SYSTEM CONFIGURATION", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp) }
        item { SettingRow("Backend Uplink Routes", "Configure server gateways for satellite processing", Icons.Filled.Api, null, { onOpenSatelliteApi() }) }
        item { SettingRow("Location Positioning", "Enable device-based GPS positioning", Icons.Filled.LocationOn, false, { requestLocation() }) }
        
        item { Text("PREFERENCES", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp) }
        item { SettingRow("Critical Risk Push Alerts", "Push notifications for active displacement events", Icons.Filled.Notifications, notifications, onNotifications) }
        item { SettingRow("Safety & Emergency Mode", "Keep emergency response tools prominent", Icons.Filled.Security, safety, { safety = it }) }
        item { SettingRow("About LandGuard", "v2.0 • Professional GIS Platform", Icons.Filled.Info, null, {}) }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, value: Boolean?, action: (Boolean) -> Unit) { 
    Card(
        onClick = { if (value == null) action(true) },
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) { 
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { 
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SoftMintContainer), contentAlignment = Alignment.Center) { Icon(icon, null, tint = ForestPrimary) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCharcoal); Text(subtitle, color = TextMuted, fontSize = 11.sp) }
            if (value != null) Switch(value, action) else Icon(Icons.Filled.ChevronRight, null, tint = TextMuted) 
        } 
    } 
}
