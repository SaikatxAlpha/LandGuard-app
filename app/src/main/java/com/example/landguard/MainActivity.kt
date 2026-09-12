@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.landguard

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import com.example.landguard.ui.satellite.SatelliteScreen
import com.example.landguard.ui.parcels.ParcelsScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.ui.alerts.AlertHistoryViewModel
import com.example.landguard.ui.alertdetail.AlertDetailViewModel
import com.example.landguard.ui.home.HomeScreen
import com.example.landguard.ui.map.MapScreen
import com.example.landguard.ui.profile.ProfileScreen
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.BrandContainer
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.LandGuardTheme
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
import kotlinx.coroutines.launch
import com.example.landguard.data.network.DeviceRegisterRequest
import com.example.landguard.data.network.LandGuardApiService
import com.google.firebase.messaging.FirebaseMessaging
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// ═════════════════════════════════════════════════════════════════════
// NAVIGATION
// ═════════════════════════════════════════════════════════════════════

private enum class LandGuardTab(
    val label: String
) {
    HOME("Home"),
    MAP("Map"),
    PARCELS("Parcels"),
    ALERTS("Alerts"),
    MORE("More")
}

// ═════════════════════════════════════════════════════════════════════
// ACTIVITY
// ═════════════════════════════════════════════════════════════════════

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var apiService: LandGuardApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.e(
            "LandGuardFCM",
            "MAIN ACTIVITY UPDATED BUILD IS RUNNING"
        )

        // ---------------------------------------------------------
        // FCM DEVICE REGISTRATION
        // ---------------------------------------------------------
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    android.util.Log.e(
                        "LandGuardFCM",
                        "Failed to get FCM token",
                        task.exception
                    )
                    return@addOnCompleteListener
                }

                val token = task.result

                if (token.isNullOrBlank()) {
                    android.util.Log.e(
                        "LandGuardFCM",
                        "FCM token is empty"
                    )
                    return@addOnCompleteListener
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        apiService.registerDevice(
                            DeviceRegisterRequest(token)
                        )

                        android.util.Log.d(
                            "LandGuardFCM",
                            "Device registered with LandGuard backend"
                        )
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "LandGuardFCM",
                            "Device registration failed",
                            e
                        )
                    }
                }
            }

        // ---------------------------------------------------------
        // FCM NOTIFICATION DEEP LINK
        // ---------------------------------------------------------
        val alertId = intent.getStringExtra("alertId")

        setContent {
            LandGuardTheme {
                LandGuardAppUI(
                    notificationAlertId = alertId
                )
            }
        }
    }
}
// ═════════════════════════════════════════════════════════════════════
// ROOT UI
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun LandGuardAppUI(
    notificationAlertId: String?
) {
    var selectedTab by remember {
        mutableStateOf(
            if (notificationAlertId != null) {
                LandGuardTab.ALERTS
            } else {
                LandGuardTab.HOME
            }
        )
    }

    var selectedAlert by remember {
        mutableStateOf<Alert?>(null)
    }

    var showSatelliteScreen by remember {
        mutableStateOf(false)
    }

    var notificationsEnabled by remember {
        mutableStateOf(true)
    }

    var showLocationSheet by remember {
        mutableStateOf(false)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current

    val locationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val granted =
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            Toast.makeText(
                context,
                if (granted) {
                    "Location enabled"
                } else {
                    "Location permission denied"
                },
                Toast.LENGTH_SHORT
            ).show()
        }

    Scaffold(
        containerColor = BgDeep,

        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },

        bottomBar = {
            LandGuardBottomNavigation(
                selectedTab = selectedTab,
                alertCount = if (notificationsEnabled) 1 else 0,
                onTabSelected = {
                    selectedTab = it
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    PaddingValues(
                        top = padding.calculateTopPadding()
                    )
                )
        ) {

            when (selectedTab) {

                // ─────────────────────────────────────────────
                // HOME
                // ─────────────────────────────────────────────

                LandGuardTab.HOME -> {

                    HomeScreen(
                        onOpenAlert = { alertId ->
                            selectedTab = LandGuardTab.ALERTS

                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Opening alert $alertId"
                                )
                            }
                        },

                        onOpenMap = {
                            selectedTab = LandGuardTab.MAP
                        },

                        onOpenAlertHistory = {
                            selectedTab = LandGuardTab.ALERTS
                        },

                        onOpenProfile = {
                            selectedTab = LandGuardTab.MORE
                        }
                    )
                }

                // ─────────────────────────────────────────────
                // MAP
                // ─────────────────────────────────────────────

                LandGuardTab.MAP -> {

                    MapScreen(
                        onOpenZone = { zoneId ->

                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Selected zone: $zoneId"
                                )
                            }
                        }
                    )
                }

                // ─────────────────────────────────────────────
                // PARCELS
                // ─────────────────────────────────────────────

                LandGuardTab.PARCELS -> {

                    com.example.landguard.ui.parcels.ParcelsScreen(
                        onOpenMap = {
                            selectedTab = LandGuardTab.MAP
                        }
                    )
                }

                // ─────────────────────────────────────────────
                // ALERTS
                // ─────────────────────────────────────────────

                LandGuardTab.ALERTS -> {

                    AlertsTabScreen(
                        notificationAlertId = notificationAlertId,
                        onAlertSelected = {
                            selectedAlert = it
                        }
                    )
                }

                // ─────────────────────────────────────────────
                // MORE
                // ─────────────────────────────────────────────

                LandGuardTab.MORE -> {
                    com.example.landguard.ui.more.MoreScreen(
                        onOpenProfile = {
                            // Profile screen will be connected here
                        },
                        onOpenSatellite = {
                            showSatelliteScreen = true
                        },
                        onOpenReports = {
                            showSatelliteScreen = true
                            // Reports screen will be connected next
                        },
                        onOpenSettings = {
                            // Existing ProfileScreen/settings will be connected here
                        }
                    )
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ALERT DETAIL
    // ═══════════════════════════════════════════════════════════

    selectedAlert?.let { alert ->

        AlertDetailBottomSheet(
            alert = alert,
            onDismiss = {
                selectedAlert = null
            }
        )
    }

    if (showSatelliteScreen) {
        ModalBottomSheet(
            onDismissRequest = {
                showSatelliteScreen = false
            },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            containerColor = BgDeep
        ) {
            SatelliteScreen(
                onBack = {
                    showSatelliteScreen = false
                }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LOCATION
    // ═══════════════════════════════════════════════════════════

    if (showLocationSheet) {

        ModalBottomSheet(
            onDismissRequest = {
                showLocationSheet = false
            },

            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),

            containerColor = BgSurface
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 22.dp,
                        end = 22.dp,
                        bottom = 36.dp
                    )
            ) {

                Text(
                    text = "Your Location",
                    color = TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Use your device location to center the land monitoring map.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(22.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showLocationSheet = false

                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },

                    shape = RoundedCornerShape(16.dp),
                    color = BrandContainer
                ) {

                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary),

                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Use Device Location",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(3.dp))

                            Text(
                                text = "Center the map on your current position.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        showLocationSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = "Cancel",
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// BOTTOM NAVIGATION
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun LandGuardBottomNavigation(
    selectedTab: LandGuardTab,
    alertCount: Int,
    onTabSelected: (LandGuardTab) -> Unit
) {
    Surface(
        color = BgSurface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(
                    horizontal = 6.dp,
                    vertical = 5.dp
                ),

            horizontalArrangement = Arrangement.SpaceEvenly,

            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomNavItem(
                tab = LandGuardTab.HOME,
                icon = Icons.Default.Home,
                selected = selectedTab == LandGuardTab.HOME,
                onClick = {
                    onTabSelected(LandGuardTab.HOME)
                }
            )

            BottomNavItem(
                tab = LandGuardTab.MAP,
                icon = Icons.Default.LocationOn,
                selected = selectedTab == LandGuardTab.MAP,
                onClick = {
                    onTabSelected(LandGuardTab.MAP)
                }
            )

            BottomNavItem(
                tab = LandGuardTab.PARCELS,
                icon = Icons.Default.Layers,
                selected = selectedTab == LandGuardTab.PARCELS,
                onClick = {
                    onTabSelected(LandGuardTab.PARCELS)
                }
            )

            BottomNavItem(
                tab = LandGuardTab.ALERTS,
                icon = Icons.Default.Warning,
                selected = selectedTab == LandGuardTab.ALERTS,
                badge = alertCount,
                onClick = {
                    onTabSelected(LandGuardTab.ALERTS)
                }
            )

            BottomNavItem(
                tab = LandGuardTab.MORE,
                icon = Icons.Default.Person,
                selected = selectedTab == LandGuardTab.MORE,
                onClick = {
                    onTabSelected(LandGuardTab.MORE)
                }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: LandGuardTab,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badge: Int = 0,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(
                vertical = 5.dp
            ),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        BadgedBox(
            badge = {

                if (badge > 0) {

                    Badge(
                        containerColor = RiskCritical
                    ) {
                        Text(
                            text = if (badge > 9) "9+" else badge.toString(),
                            color = Color.White,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        ) {

            Icon(
                imageVector = icon,
                contentDescription = tab.label,

                tint = if (selected) {
                    BrandPrimary
                } else {
                    TextMuted
                },

                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text = tab.label,

            color = if (selected) {
                BrandPrimary
            } else {
                TextMuted
            },

            fontSize = 10.sp,

            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════
// ALERTS
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun AlertsTabScreen(
    notificationAlertId: String?,
    onAlertSelected: (Alert) -> Unit,

    viewModel: AlertHistoryViewModel = hiltViewModel()
) {
    val alerts by viewModel.history.collectAsStateWithLifecycle()

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    val filteredAlerts = when (selectedFilter) {

        "Critical" ->
            alerts.filter {
                it.severity.name == "CRITICAL"
            }

        "High" ->
            alerts.filter {
                it.severity.name == "HIGH"
            }

        "Medium" ->
            alerts.filter {
                it.severity.name == "MODERATE"
            }

        else -> alerts
    }

    /*
     * FCM notification handling.
     *
     * When MainActivity was opened with alertId,
     * find that exact alert and open it.
     */
    LaunchedEffect(
        notificationAlertId,
        alerts
    ) {

        if (
            notificationAlertId != null &&
            alerts.isNotEmpty()
        ) {

            alerts
                .firstOrNull {
                    it.id == notificationAlertId
                }
                ?.let {
                    onAlertSelected(it)
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {

        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 20.dp,
                    bottom = 10.dp
                )
        ) {

            Text(
                text = "Alerts",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Stay informed about changes affecting your land.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf(
                    "All",
                    "Critical",
                    "High",
                    "Medium"
                ).forEach { filter ->

                    AlertFilterChip(
                        text = filter,
                        selected = selectedFilter == filter,
                        onClick = {
                            selectedFilter = filter
                        }
                    )
                }
            }
        }

        if (filteredAlerts.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(BrandContainer),

                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "No alerts found",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Your monitored land is quiet.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),

                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 18.dp
                ),

                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = filteredAlerts,
                    key = { it.id }
                ) { alert ->

                    com.example.landguard.ui.components.LandGuardAlertCard(
                        title = alert.title,
                        location = alert.affectedLocation.ifBlank {
                            "Location unavailable"
                        },
                        timestamp = alert.timestamp.ifBlank {
                            "Recently detected"
                        },
                        severity = alert.severity,
                        description = alert.description,
                        onClick = {
                            onAlertSelected(alert)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),

        color = if (selected) {
            BrandPrimary
        } else {
            BgSurface
        },

        shape = RoundedCornerShape(20.dp),

        border = if (!selected) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                BgBorder
            )
        } else {
            null
        }
    ) {

        Text(
            text = text,

            color = if (selected) {
                Color.White
            } else {
                TextSecondary
            },

            fontSize = 11.sp,

            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            },

            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 8.dp
            )
        )
    }
}

// ═════════════════════════════════════════════════════════════════════
// PARCELS TEMPORARY SCREEN
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ParcelsTabScreen(
    onOpenMap: () -> Unit
) {
    val viewModel: com.example.landguard.ui.home.HomeViewModel =
        hiltViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(horizontal = 16.dp)
    ) {

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Parcels",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Your monitored land areas",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(18.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),

            contentPadding = PaddingValues(
                bottom = 90.dp
            )
        ) {

            items(
                items = state.parcels,
                key = { it.id }
            ) { parcel ->

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenMap),

                    color = BgSurface,
                    shape = RoundedCornerShape(16.dp),

                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        BgBorder
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (parcel.riskCategory.name) {
                                        "CRITICAL" -> RiskCriticalContainer
                                        "HIGH" -> RiskHighContainer
                                        "MODERATE" -> RiskModerateContainer
                                        else -> RiskLowContainer
                                    }
                                ),

                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,

                                tint = when (parcel.riskCategory.name) {
                                    "CRITICAL" -> RiskCritical
                                    "HIGH" -> RiskHigh
                                    "MODERATE" -> RiskModerate
                                    else -> RiskLow
                                },

                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = parcel.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(3.dp))

                            Text(
                                text = parcel.villageOrDistrict,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "${parcel.areaHectares} ha • ${parcel.landType}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {

                            Text(
                                text = parcel.riskCategory.name,
                                color = when (parcel.riskCategory.name) {
                                    "CRITICAL" -> RiskCritical
                                    "HIGH" -> RiskHigh
                                    "MODERATE" -> RiskModerate
                                    else -> RiskLow
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "${parcel.riskScore}/100",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// MORE
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun MoreTabScreen(
    notificationsEnabled: Boolean,
    onNotificationsChanged: (Boolean) -> Unit,
    onLocationRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(horizontal = 16.dp)
    ) {

        Spacer(Modifier.height(20.dp))

        Text(
            text = "More",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "LandGuard preferences and tools",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(18.dp))

        // Profile card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BrandContainer,
            shape = RoundedCornerShape(18.dp)
        ) {

            Row(
                modifier = Modifier.padding(17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "E",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Land Explorer",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = "LandGuard user",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "TOOLS",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(8.dp))

        MoreRow(
            icon = Icons.Default.Layers,
            title = "Satellite Data",
            subtitle = "Explore recent observations",
            onClick = {}
        )

        MoreRow(
            icon = Icons.Default.Warning,
            title = "Reports",
            subtitle = "Risk summaries and reports",
            onClick = {}
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "SETTINGS",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(8.dp))

        MoreRow(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = if (notificationsEnabled) {
                "Alerts are enabled"
            } else {
                "Alerts are disabled"
            },
            onClick = {
                onNotificationsChanged(!notificationsEnabled)
            }
        )

        MoreRow(
            icon = Icons.Default.LocationOn,
            title = "Location",
            subtitle = "Choose device location",
            onClick = onLocationRequest
        )

        MoreRow(
            icon = Icons.Default.Person,
            title = "Profile",
            subtitle = "Account information",
            onClick = {}
        )
    }
}

@Composable
private fun MoreRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),

        color = BgSurface,
        shape = RoundedCornerShape(15.dp),

        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            BgBorder
        )
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(BrandContainer),

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            Text(
                text = "›",
                color = TextMuted,
                fontSize = 22.sp
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// ALERT DETAIL
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun AlertDetailBottomSheet(
    alert: Alert,
    onDismiss: () -> Unit
) {
    val viewModel: AlertDetailViewModel = hiltViewModel()

    val severityColor = when (alert.severity.name) {
        "CRITICAL" -> RiskCritical
        "HIGH" -> RiskHigh
        "MODERATE" -> RiskModerate
        else -> RiskLow
    }

    val severityBackground = when (alert.severity.name) {
        "CRITICAL" -> RiskCriticalContainer
        "HIGH" -> RiskHighContainer
        "MODERATE" -> RiskModerateContainer
        else -> RiskLowContainer
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgSurface
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 30.dp
                )
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(severityBackground),

                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Land Risk Alert",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = alert.severity.name,
                        color = severityColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = alert.title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = alert.description.ifBlank {
                    "A land-risk event has been detected in the monitored area."
                },
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(18.dp))

            AlertDetailRow(
                label = "Location",
                value = alert.affectedLocation.ifBlank {
                    "Unknown"
                }
            )

            AlertDetailRow(
                label = "Detected Event",
                value = alert.detectedEvent.ifBlank {
                    "Land change detected"
                }
            )

            AlertDetailRow(
                label = "Confidence",
                value = "${alert.confidencePercentage}%"
            )

            AlertDetailRow(
                label = "Status",
                value = alert.status.name
            )

            AlertDetailRow(
                label = "Source",
                value = alert.sourceProvider
            )

            Spacer(Modifier.height(18.dp))

            if (alert.status == AlertStatus.NEW) {

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {

                            viewModel.updateAlertStatus(
                                alert.id,
                                AlertStatus.ACKNOWLEDGED
                            )

                            onDismiss()
                        },

                    color = BrandPrimary,
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp),

                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "Acknowledge Alert",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
    ) {

        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.width(105.dp)
        )

        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}