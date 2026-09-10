@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.landguard

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import com.example.landguard.ui.home.HomeScreen
import com.example.landguard.ui.map.MapScreen
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.CyanContainer
import com.example.landguard.ui.theme.CyanPrimary
import com.example.landguard.ui.theme.EmeraldContainer
import com.example.landguard.ui.theme.EmeraldPrimary
import com.example.landguard.ui.theme.LandGuardTheme
import com.example.landguard.ui.theme.PurpleContainer
import com.example.landguard.ui.theme.PurplePrimary
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.RiskCriticalContainer
import com.example.landguard.ui.theme.RiskHigh
import com.example.landguard.ui.theme.RiskHighContainer
import com.example.landguard.ui.theme.RiskLow
import com.example.landguard.ui.theme.RiskLowContainer
import com.example.landguard.ui.theme.RiskModerate
import com.example.landguard.ui.theme.RiskModerateContainer
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary
import com.example.landguard.ui.theme.TextWhite
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// ─── Data Models ─────────────────────────────────────────────────────────────



private enum class Tab(val label: String, val icon: ImageVector) {
    HOME    ("Overview",  Icons.Filled.Analytics),
    MAP     ("Risk Map",  Icons.Filled.Radar),
    ALERTS  ("Alerts",    Icons.Filled.Warning),
    PROFILE ("Settings",  Icons.Filled.Settings)
}

// ─── Activity ────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alertId = intent.getStringExtra("alertId")
        setContent { LandGuardTheme { LandGuardAppUI(alertId) } }
    }
}

@Composable
private fun AlertDetailScreen(
    alert: com.example.landguard.domain.model.Alert,
    onBack: () -> Unit,
    viewModel: com.example.landguard.ui.alertdetail.AlertDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val (accent, bg) = when (alert.severity.name) {
        "CRITICAL" -> Pair(RiskCritical,  RiskCriticalContainer)
        "HIGH"     -> Pair(RiskHigh,      RiskHighContainer)
        "MODERATE" -> Pair(RiskModerate,  RiskModerateContainer)
        else       -> Pair(RiskLow,       RiskLowContainer)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface)
                .border(androidx.compose.foundation.BorderStroke(0.dp, BgBorder))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("<", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Alert Details", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, letterSpacing = (-0.3).sp)
                    Text("ID: ${alert.id}", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        HorizontalDivider(color = BgBorder)
        
        Column(modifier = Modifier.padding(16.dp)) {
            Text(alert.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(alert.severity.name, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(alert.description, color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Text("Location: ${alert.affectedLocation}", color = TextSecondary, fontSize = 14.sp)
            Text("Status: ${alert.status.name}", color = TextMuted, fontSize = 14.sp)
            
            Spacer(Modifier.height(32.dp))
            if (alert.status == com.example.landguard.domain.model.AlertStatus.NEW) {
                Button(
                    onClick = { 
                        viewModel.updateAlertStatus(alert.id, com.example.landguard.domain.model.AlertStatus.ACKNOWLEDGED)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = BgDeep),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Acknowledge Alert")
                }
            }
        }
    }
}


// ─── App UI Shell ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LandGuardAppUI(alertId: String? = null) {
    var tab                by remember { mutableStateOf(if (alertId != null) Tab.ALERTS else Tab.HOME) }
    var showLocationSheet  by remember { mutableStateOf(false) }
    var selectedAlert      by remember { mutableStateOf<com.example.landguard.domain.model.Alert?>(null) }
    var notifications      by remember { mutableStateOf(true) }
    val snackbar            = remember { SnackbarHostState() }
    val scope               = rememberCoroutineScope()
    val context             = LocalContext.current

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Toast.makeText(context, if (granted) "GPS Locked ✓" else "GPS Permission Denied", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = BgDeep,
        topBar = {
            if (tab != Tab.MAP) ProTopBar(
                onNotifications = { tab = Tab.ALERTS },
                onGps           = { showLocationSheet = true },
                notifications   = notifications
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (tab != Tab.MAP) padding else PaddingValues(0.dp))
        ) {
            // ── Screens ──────────────────────────────────────────────────────
            when (tab) {
                Tab.HOME    -> HomeScreen(
                    onOpenAlert        = { scope.launch { snackbar.showSnackbar("Alert opened") } },
                    onOpenMap          = { tab = Tab.MAP },
                    onOpenAlertHistory = { tab = Tab.ALERTS },
                    onOpenProfile      = { tab = Tab.PROFILE }
                )
                Tab.MAP     -> MapScreen(onOpenZone = { tab = Tab.HOME })
                Tab.ALERTS  -> AlertsScreen(
                    onAlert = { selectedAlert = it },
                    targetAlertId = alertId
                )
                Tab.PROFILE -> ProfileScreen(
                    notifications    = notifications,
                    onNotifications  = { notifications = it },
                    requestLocation  = {
                        locationLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    },
                    onOpenSatelliteApi = { tab = Tab.MAP }
                )
            }

            // ── Floating Navigation Bar ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
                    .height(64.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(BgSurface)
                    .border(1.dp, BgBorder, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Tab.entries.forEach { item ->
                        val isSelected = tab == item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(
                                    if (isSelected)
                                        Brush.linearGradient(listOf(CyanPrimary.copy(alpha = 0.2f), EmeraldPrimary.copy(alpha = 0.1f)))
                                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) CyanPrimary.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = RoundedCornerShape(25.dp)
                                )
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
                                    tint = if (isSelected) CyanPrimary else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (isSelected) {
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        item.label,
                                        color      = CyanPrimary,
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines   = 1,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Location Bottom Sheet ─────────────────────────────────────────────────
    if (showLocationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = BgSurface,
            tonalElevation   = 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.GpsFixed, null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Select Monitored Location", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Target zone for risk analysis", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        showLocationSheet = false
                        locationLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape  = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.GpsFixed, null, tint = Color(0xFF001822))
                    Spacer(Modifier.width(10.dp))
                    Text("USE DEVICE GPS", color = Color(0xFF001822), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }

                Spacer(Modifier.height(16.dp))
                Text("PRESET LOCATIONS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))

                listOf(
                    "Kalimpong Ridge, West Bengal",
                    "Teesta River Gorge, Sikkim",
                    "Darjeeling Observatory Hill",
                    "Siliguri Bypass Basin"
                ).forEach { place ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgElevated)
                            .border(1.dp, BgBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                showLocationSheet = false
                                scope.launch { snackbar.showSnackbar("Target locked: $place") }
                            }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(place, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // ── Alert Detail Dialog ───────────────────────────────────────────────────
    selectedAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = { selectedAlert = null },
            containerColor   = BgSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RiskCriticalContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = RiskCritical, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("LAND RISK ALERT", color = RiskCritical, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text(alert.severity.name, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column {
                    Text(alert.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(alert.affectedLocation, color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(alert.timestamp.takeIf { it.isNotBlank() } ?: "Just now", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RISK CONFIDENCE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Text("${alert.confidencePercentage}%", color = RiskCritical, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress         = { alert.confidencePercentage / 100f },
                        modifier         = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color            = RiskCritical,
                        trackColor       = BgBorder
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedAlert = null
                        scope.launch { snackbar.showSnackbar("Alert acknowledged") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Text("ACKNOWLEDGE", color = Color(0xFF001822), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 0.8.sp)
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
private fun ProTopBar(
    onNotifications: () -> Unit,
    onGps: () -> Unit,
    notifications: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "topbar_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "topbar_dot"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgDeep)
    ) {
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = BgBorder
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(CyanContainer, EmeraldContainer))
                    )
                    .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Landscape, null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("LandGuard", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary, letterSpacing = (-0.3).sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = dotAlpha))
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "SATELLITE RISK PLATFORM",
                        fontSize    = 8.sp,
                        letterSpacing = 1.6.sp,
                        color       = CyanPrimary,
                        fontWeight  = FontWeight.Bold
                    )
                }
            }
            // Notifications
            BadgedBox(badge = {
                if (notifications) Badge(containerColor = RiskCritical) {
                    Text("3", color = Color.White, fontSize = 8.sp)
                }
            }) {
                IconButton(onClick = onNotifications) {
                    Icon(Icons.Filled.Notifications, null, tint = TextPrimary)
                }
            }
            // GPS
            IconButton(onClick = onGps) {
                Icon(Icons.Filled.GpsFixed, null, tint = CyanPrimary)
            }
        }
    }
}

@Composable
private fun AlertsScreen(
    onAlert: (com.example.landguard.domain.model.Alert) -> Unit,
    targetAlertId: String? = null,
    viewModel: com.example.landguard.ui.alerts.AlertHistoryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val list by viewModel.history.collectAsState()
    var criticalOnly by remember { mutableStateOf(false) }
    
    androidx.compose.runtime.LaunchedEffect(targetAlertId, list) {
        if (targetAlertId != null && list.isNotEmpty()) {
            val alert = list.find { it.id == targetAlertId }
            if (alert != null) {
                onAlert(alert)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface)
                .border(androidx.compose.foundation.BorderStroke(0.dp, BgBorder))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Text("Risk Alert Feed", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, letterSpacing = (-0.3).sp)
                Spacer(Modifier.height(4.dp))
                Text("Real-time satellite displacement events", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Critical & High only", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = criticalOnly,
                        onCheckedChange = { criticalOnly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor  = CyanPrimary,
                            checkedTrackColor  = CyanContainer,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = BgElevated
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = BgBorder)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val filtered = list.filter { !criticalOnly || it.severity.name in listOf("CRITICAL", "HIGH") }
            items(filtered) { alert ->
                AlertFeedCard(alert = alert, onClick = { onAlert(alert) })
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun AlertFeedCard(alert: com.example.landguard.domain.model.Alert, onClick: () -> Unit) {
    val (accent, bg) = when (alert.severity.name) {
        "CRITICAL" -> Pair(RiskCritical,  RiskCriticalContainer)
        "HIGH"     -> Pair(RiskHigh,      RiskHighContainer)
        "MODERATE" -> Pair(RiskModerate,  RiskModerateContainer)
        else       -> Pair(RiskLow,       RiskLowContainer)
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = BgSurface),
        shape    = RoundedCornerShape(16.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, BgBorder)
    ) {
        Row(modifier = Modifier.height(90.dp)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.3f))))
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Warning, null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(alert.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text("${alert.affectedLocation}  •  ${alert.timestamp.takeIf { it.isNotBlank() } ?: "Just now"}", color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = bg, shape = RoundedCornerShape(5.dp)) {
                            Text(alert.severity.name, color = accent, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Status: ${alert.status.name}", color = TextMuted, fontSize = 10.sp)
                    }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
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
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                Text("System Settings", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, letterSpacing = (-0.3).sp)
                Text("Configure LandGuard monitoring parameters", color = TextSecondary, fontSize = 13.sp)
            }
        }

        // Operator card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(CyanContainer, EmeraldContainer.copy(alpha = 0.5f)))
                    )
                    .border(1.dp, CyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CyanPrimary, EmeraldPrimary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, null, tint = Color(0xFF001822), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("GIS Field Specialist", fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 16.sp)
                        Text("Land Monitoring Operator", color = CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Surface(color = EmeraldContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "AUTHORIZED  •  LEVEL 3",
                                color = EmeraldPrimary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // System config section
        item { SectionLabel("SYSTEM CONFIGURATION") }
        item { SettingCard("Satellite Backend Routes", "Configure server gateways & API endpoints", Icons.Filled.Api,       CyanPrimary,    null,          { onOpenSatelliteApi() }) }
        item { SettingCard("GPS Positioning",          "Enable device-based location lock",          Icons.Filled.LocationOn, EmeraldPrimary, null,          { requestLocation() }) }

        // Preferences section
        item { SectionLabel("PREFERENCES") }
        item { SettingCard("Critical Push Alerts",    "Notifications for displacement events",      Icons.Filled.Notifications, RiskCritical, notifications, onNotifications) }
        item { SettingCard("Safety & Emergency Mode", "Keep emergency tools prominent",             Icons.Filled.Security,      EmeraldPrimary, safety,       { safety = it }) }

        // Info
        item { SectionLabel("ABOUT") }
        item { SettingCard("About LandGuard Pro",     "v3.0 • Stellar Command Edition • 2026",     Icons.Filled.Info, PurplePrimary, null, {}) }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color        = TextMuted,
        fontSize     = 10.sp,
        fontWeight   = FontWeight.ExtraBold,
        letterSpacing = 1.8.sp,
        modifier     = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingCard(
    title: String, subtitle: String,
    icon: ImageVector, accent: Color,
    value: Boolean?, action: (Boolean) -> Unit
) {
    Card(
        onClick = { if (value == null) action(true) },
        colors  = CardDefaults.cardColors(containerColor = BgSurface),
        shape   = RoundedCornerShape(16.dp),
        border  = androidx.compose.foundation.BorderStroke(1.dp, BgBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
            if (value != null) {
                Switch(
                    checked          = value,
                    onCheckedChange  = action,
                    colors           = SwitchDefaults.colors(
                        checkedThumbColor   = accent,
                        checkedTrackColor   = accent.copy(alpha = 0.2f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = BgElevated
                    )
                )
            } else {
                Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
