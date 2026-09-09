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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.landguard.ui.map.MapScreen
import com.example.landguard.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private data class AlertItem(val title: String, val area: String, val time: String, val severity: String, val score: Int)
private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("DASH", Icons.Filled.Analytics),
    MAP("SAT-MAP", Icons.Filled.Radar),
    ALERTS("ALERTS", Icons.Filled.Warning),
    PROFILE("SYS", Icons.Filled.Settings)
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
    var demoRefresh by remember { mutableIntStateOf(0) }
    var notifications by remember { mutableStateOf(true) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Toast.makeText(context, if (granted) "GPS Locked" else "GPS Denied", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = CoreBackground,
        topBar = {
            if (tab != Tab.MAP) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(CyberCyan.copy(alpha=0.15f)).border(1.dp, CyberCyan.copy(alpha=0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Landscape, null, tint = CyberCyan)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LANDGUARD", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary, letterSpacing = 1.sp)
                        Text("ALOS-4 & SENTINEL-2 RISK ENGINE", fontSize = 9.sp, letterSpacing = 1.5.sp, color = CyberBlue, fontWeight = FontWeight.Bold)
                    }
                    BadgedBox(badge = { if (notifications) Badge(containerColor = CyberRed) { Text("3", color = Color.White) } }) {
                        IconButton(onClick = { tab = Tab.ALERTS }) { Icon(Icons.Filled.Notifications, "Alerts", tint = TextPrimary) }
                    }
                    IconButton(onClick = { showLocationSheet = true }) { Icon(Icons.Filled.GpsFixed, "Location", tint = CyberCyan) }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(if(tab != Tab.MAP) padding else androidx.compose.foundation.layout.PaddingValues(0.dp))) {
            
            // Main Content
            when (tab) {
                Tab.HOME -> HomeScreen(onOpenMap = { tab = Tab.MAP }, onAlert = { selectedAlert = it }, refreshToken = demoRefresh)
                Tab.MAP -> MapScreen(onOpenZone = { tab = Tab.HOME })
                Tab.ALERTS -> AlertsScreen(onAlert = { selectedAlert = it })
                Tab.PROFILE -> ProfileScreen(notifications, { notifications = it }, { locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, onOpenSatelliteApi = { tab = Tab.MAP })
            }

            // Custom Floating Equal-Weight Navigation Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(0.92f)
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(GlassBackground)
                    .border(1.dp, GlassBorder, RoundedCornerShape(30.dp)),
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
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .border(if (isSelected) 1.dp else 0.dp, if (isSelected) CyberCyan.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(24.dp))
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
                                    tint = if (isSelected) CyberCyan else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.label,
                                        color = CyberCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
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
            containerColor = SurfaceDark
        ) {
            Column(Modifier.padding(24.dp).padding(bottom = 24.dp)) {
                Text("ACQUIRE TARGET ZONE", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text("Select a region to calibrate ALOS-4 SAR and Sentinel-2 optical telemetry feeds.", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { showLocationSheet = false; locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
                ) {
                    Icon(Icons.Filled.GpsFixed, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("USE DEVICE GPS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(16.dp))
                listOf("Siliguri / Darjeeling Corridor", "Sikkim Himalayan Belt", "Kalimpong - Teesta Gorge").forEach { place ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceLight)
                            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .clickable { showLocationSheet = false; scope.launch { snackbar.showSnackbar("Target locked: $place") } }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(place, color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ChevronRight, null, tint = CyberCyan)
                        }
                    }
                }
            }
        }
    }

    selectedAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = { selectedAlert = null },
            containerColor = SurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = CyberRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CRITICAL ANOMALY", color = CyberRed, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            },
            text = {
                Column {
                    Text(alert.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text(alert.area, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Text("RISK CONFIDENCE: ${alert.score}%", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { alert.score / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = CyberRed, trackColor = SurfaceLight)
                }
            },
            confirmButton = {
                Button(onClick = { selectedAlert = null; scope.launch { snackbar.showSnackbar("Alert suppressed") } }, colors = ButtonDefaults.buttonColors(containerColor = CyberRed)) {
                    Text("ACKNOWLEDGE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAlert = null }) {
                    Text("CLOSE", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun HomeScreen(onOpenMap: () -> Unit, onAlert: (AlertItem) -> Unit, refreshToken: Int) {
    val alerts = remember(refreshToken) { listOf(
        AlertItem("ALOS-4 InSAR Slope Shift", "Kalimpong Sector 04 (-28.4mm/y)", "8 min ago", "HIGH", 92),
        AlertItem("Sentinel-2 NDVI Collapse", "Teesta River Gorge (NDVI: 0.22)", "15 min ago", "HIGH", 89)
    ) }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text("SYSTEM STATUS", color = CyberGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("ORBITAL FEEDS ACTIVE", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        item { RiskHeroCard(92, onOpenMap) }
        item { SectionTitle("LIVE TELEMETRY") }
        item { ConditionsRow() }
        item { SectionTitle("ANOMALY LOGS") }
        items(alerts) { alert -> AlertCard(alert, { onAlert(alert) }) }
        item { SectionTitle("RISK VECTORS") }
        item { RiskDrivers() }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun RiskHeroCard(score: Int, onOpenMap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2E0916), Color(0xFF150A11))))
            .border(1.dp, CyberRed.copy(alpha=0.3f), RoundedCornerShape(24.dp))
            .clickable { onOpenMap() }
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("REGIONAL THREAT LEVEL", color = CyberRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("CRITICAL", color = TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Immediate slope failure risk detected.", color = Color(0xFFFFB3B3), fontSize = 12.sp)
                }
                Box(Modifier.size(80.dp).clip(CircleShape).background(CyberRed.copy(alpha=0.2f)).border(2.dp, CyberRed.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$score", color = CyberRed, fontSize = 28.sp, fontWeight = FontWeight.Black); Text("INDEX", color = CyberRed, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Radar, null, tint = CyberRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ALOS-4 TARGET: Teesta Gorge", color = TextPrimary, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Filled.ChevronRight, null, tint = CyberRed)
            }
        }
    }
}

@Composable private fun SectionTitle(title: String) {
    Text(title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
}

@Composable private fun ConditionsRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        ConditionCard(Icons.Filled.Speed, "INSAR SHIFT", "-28.4", "mm/y", CyberRed)
        ConditionCard(Icons.Filled.Radar, "BACKSCATTER", "-14.2", "dB", CyberCyan)
        ConditionCard(Icons.Filled.Landscape, "S-2 NDVI", "0.22", "Index", CyberOrange)
        ConditionCard(Icons.Filled.Waves, "SATURATION", "91", "%", CyberBlue)
    }
}
@Composable private fun ConditionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, unit: String, accent: Color) {
    Box(
        modifier = Modifier.width(130.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceDark).border(1.dp, GlassBorder, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Column {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text(unit, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable private fun AlertCard(alert: AlertItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceDark).border(1.dp, CyberRed.copy(alpha=0.2f), RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(CyberRed))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(alert.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text("${alert.area} // ${alert.time}", color = TextSecondary, fontSize = 11.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
        }
    }
}

@Composable private fun RiskDrivers() {
    val drivers = listOf("InSAR Displacement" to 92 to CyberRed, "NDWI Saturation" to 84 to CyberBlue, "NDVI Scar Detect" to 68 to CyberOrange, "DEM Slope Angle" to 78 to CyberCyan)
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(SurfaceDark).border(1.dp, GlassBorder, RoundedCornerShape(20.dp))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            drivers.forEach { (pair, color) ->
                val (name, value) = pair
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, color = TextPrimary, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("$value%", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { value / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = color, trackColor = SurfaceLight)
                }
            }
        }
    }
}

@Composable private fun AlertsScreen(onAlert: (AlertItem) -> Unit) {
    val list = listOf(
        AlertItem("ALOS-4 InSAR Slope Shift Detected", "Kalimpong • Sector 04 (-28.4mm/y)", "8 min ago", "HIGH", 82),
        AlertItem("Sentinel-2 Vegetation NDVI Drop", "Teesta River Gorge (NDVI: 0.22)", "15 min ago", "HIGH", 89),
        AlertItem("Soil moisture rising", "Siliguri • North ridge", "48 min ago", "MODERATE", 57),
        AlertItem("All clear", "Mirik • Valley zone", "1 hr ago", "LOW", 22)
    )
    var criticalOnly by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp)) { 
            Text("THREAT LOGS", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Text("Filter critical events only", modifier = Modifier.weight(1f), color = TextSecondary, fontSize = 13.sp)
                Switch(criticalOnly, { criticalOnly = it }) 
            } 
        }
        HorizontalDivider(color = GlassBorder)
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { 
            items(list.filter { !criticalOnly || it.severity == "HIGH" }) { AlertCard(it) { onAlert(it) } }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable private fun ProfileScreen(
    notifications: Boolean,
    onNotifications: (Boolean) -> Unit,
    requestLocation: () -> Unit,
    onOpenSatelliteApi: () -> Unit
) {
    var safety by remember { mutableStateOf(true) }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("SYSTEM OP", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary); Text("Configure array parameters.", color = TextSecondary, fontSize = 13.sp) }
        
        item { 
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(CyberCyan.copy(alpha=0.1f)).border(1.dp, CyberCyan.copy(alpha=0.3f), RoundedCornerShape(20.dp))) { 
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { 
                    Box(Modifier.size(56.dp).clip(CircleShape).background(CyberCyan), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Person, null, tint = CoreBackground) }
                    Spacer(Modifier.width(16.dp))
                    Column { Text("COMMANDER ZERO", fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 16.sp); Text("Clearance: MAXIMUM", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) } 
                } 
            } 
        }
        
        item { SectionTitle("SATELLITE UPLINKS") }
        item { SettingRow("API Credentials", "Copernicus (S-2) & JAXA (ALOS-4)", Icons.Filled.Api, null, { onOpenSatelliteApi() }) }
        item { SettingRow("Targeting Lock", "Device GPS telemetry", Icons.Filled.LocationOn, false, { requestLocation() }) }
        
        item { SectionTitle("PREFERENCES") }
        item { SettingRow("Critical Overrides", "Push notifications for shift events", Icons.Filled.Notifications, notifications, onNotifications) }
        item { SettingRow("Emergency Mode", "Highlight high-strain UI elements", Icons.Filled.Security, safety, { safety = it }) }
        item { SettingRow("About LandGuard", "v2.0 • Cyber-GIS Edition", Icons.Filled.Info, null, {}) }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable private fun SettingRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, value: Boolean?, action: (Boolean) -> Unit) { 
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceDark).border(1.dp, GlassBorder, RoundedCornerShape(16.dp)).clickable { if (value == null) action(true) }
    ) { 
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { 
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceLight), contentAlignment = Alignment.Center) { Icon(icon, null, tint = CyberCyan) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary); Text(subtitle, color = TextSecondary, fontSize = 11.sp) }
            if (value != null) Switch(value, action) else Icon(Icons.Filled.ChevronRight, null, tint = TextMuted) 
        } 
    } 
}
