@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.landguard.ui.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.alertdetail.AlertDetailViewModel
import com.example.landguard.ui.alerts.AlertHistoryViewModel
import com.example.landguard.ui.components.GlassSurface
import com.example.landguard.ui.components.LandGuardWordmark
import com.example.landguard.ui.components.SeverityDot
import com.example.landguard.ui.components.SeverityPill
import com.example.landguard.ui.components.accent
import com.example.landguard.ui.components.container
import com.example.landguard.ui.components.pressClickable
import com.example.landguard.ui.home.FloatingNavClearance
import com.example.landguard.ui.home.HomeScreen
import com.example.landguard.ui.home.PanelButton
import com.example.landguard.ui.location.LocalRequestUserLocation
import com.example.landguard.ui.location.LocalUserLocation
import com.example.landguard.ui.location.LocationStatus
import com.example.landguard.ui.location.UserLocationState
import com.example.landguard.ui.location.fetchUserLocation
import com.example.landguard.ui.location.hasLocationPermission
import com.example.landguard.ui.map.MapScreen
import com.example.landguard.ui.parcels.ParcelsScreen
import com.example.landguard.ui.risk.RiskAreasScreen
import com.example.landguard.ui.risk.RiskAreasUiState
import com.example.landguard.ui.risk.RiskAreasViewModel
import com.example.landguard.ui.risk.nearestArea
import com.example.landguard.ui.navigation.MapTarget
import com.example.landguard.ui.report.ReportPlace
import com.example.landguard.ui.report.ReportScreen
import com.example.landguard.di.BackendConfig
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NotificationsActive
import com.example.landguard.ui.satellite.SatelliteScreen
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.BrandContainer
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.BrandPrimaryLight
import com.example.landguard.ui.theme.EarthOchre
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═════════════════════════════════════════════════════════════════════
// NAVIGATION
// ═════════════════════════════════════════════════════════════════════

private enum class LandGuardTab(
    val label: String,
    val icon: ImageVector
) {
    HOME("Explore", Icons.Filled.Explore),
    MAP("Map", Icons.Filled.Map),
    RISK("Risk Areas", Icons.Filled.Terrain),
    ALERTS("Alerts", Icons.Filled.Warning),
    MORE("More", Icons.Filled.Menu)
}

/** Tabs whose content is a full-bleed map and manages its own insets. */
private val LandGuardTab.isMapFirst: Boolean
    get() = this == LandGuardTab.HOME || this == LandGuardTab.MAP || this == LandGuardTab.RISK

// ═════════════════════════════════════════════════════════════════════
// ROOT UI
// ═════════════════════════════════════════════════════════════════════

@Composable
fun LandGuardAppUI(
    notificationAlertId: String?,
    onNotificationHandled: () -> Unit,
    canPromptForLocation: Boolean
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable {
        mutableStateOf(
            if (notificationAlertId != null) LandGuardTab.ALERTS else LandGuardTab.HOME
        )
    }

    // The open alert is held by id, so its sheet always shows the live stored copy.
    var selectedAlertId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSatelliteScreen by remember { mutableStateOf(false) }
    var showParcels by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var showLocationSheet by remember { mutableStateOf(false) }
    // Cross-screen "show on map" requests, consumed by the destination screen.
    var mapTarget by remember { mutableStateOf<MapTarget?>(null) }
    var riskTarget by remember { mutableStateOf<MapTarget?>(null) }
    var openMapSearch by remember { mutableStateOf(false) }
    var missingAlertSyncRequested by remember { mutableStateOf<String?>(null) }
    var deepLinkShownFor by remember { mutableStateOf<String?>(null) }

    val alertsViewModel: AlertHistoryViewModel = hiltViewModel()
    val alerts by alertsViewModel.history.collectAsStateWithLifecycle()
    // Activity-scoped: the same instance backs Home, Risk Areas, the Map search and More.
    val riskViewModel: RiskAreasViewModel = hiltViewModel()
    val riskState by riskViewModel.uiState.collectAsStateWithLifecycle()

    val location = rememberUserLocationController(canPromptForLocation)

    fun openAlert(alertId: String) {
        if (alerts.any { it.id == alertId }) selectedAlertId = alertId else selectedTab = LandGuardTab.ALERTS
    }

    fun switchTab(tab: LandGuardTab) {
        showParcels = false
        showReport = false
        selectedTab = tab
    }

    /*
     * Notification deep link (cold start or onNewIntent): open that exact alert as soon as
     * it is in the local store. If it is not there yet, pull it from the backend once.
     */
    LaunchedEffect(notificationAlertId, alerts) {
        val id = notificationAlertId ?: return@LaunchedEffect
        if (deepLinkShownFor != id) {
            deepLinkShownFor = id
            selectedAlertId = null
            switchTab(LandGuardTab.ALERTS)
        }
        if (alerts.any { it.id == id }) {
            selectedAlertId = id
            onNotificationHandled()
        } else if (missingAlertSyncRequested != id) {
            missingAlertSyncRequested = id
            alertsViewModel.syncNow()
        }
    }

    BackHandler(enabled = showReport) { showReport = false }
    BackHandler(enabled = !showReport && showParcels) { showParcels = false }
    BackHandler(enabled = !showReport && !showParcels && selectedTab != LandGuardTab.HOME) {
        selectedTab = LandGuardTab.HOME
    }

    CompositionLocalProvider(
        LocalUserLocation provides location.state,
        LocalRequestUserLocation provides location.request
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDeep)
        ) {

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(220, delayMillis = 40)) togetherWith fadeOut(tween(160))
                },
                label = "tabContent"
            ) { tab ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgDeep)
                        .then(if (tab.isMapFirst) Modifier else Modifier.statusBarsPadding())
                ) {
                    when (tab) {

                        LandGuardTab.HOME -> HomeScreen(
                            onOpenAlert = { openAlert(it) },
                            onOpenMap = { target ->
                                mapTarget = target
                                switchTab(LandGuardTab.MAP)
                            },
                            onOpenAlertHistory = { switchTab(LandGuardTab.ALERTS) },
                            onOpenProfile = { switchTab(LandGuardTab.MORE) },
                            onOpenRiskAreas = { switchTab(LandGuardTab.RISK) },
                            onOpenSearch = {
                                openMapSearch = true
                                switchTab(LandGuardTab.MAP)
                            },
                            riskViewModel = riskViewModel
                        )

                        LandGuardTab.MAP -> MapScreen(
                            onOpenZone = { areaId ->
                                riskTarget = MapTarget(areaId, null, null, null)
                                switchTab(LandGuardTab.RISK)
                            },
                            onOpenAlert = { openAlert(it) },
                            target = mapTarget,
                            onTargetConsumed = { mapTarget = null },
                            openSearch = openMapSearch,
                            onSearchOpened = { openMapSearch = false },
                            riskViewModel = riskViewModel
                        )

                        LandGuardTab.RISK -> RiskAreasScreen(
                            onOpenOnMap = { target ->
                                mapTarget = target
                                switchTab(LandGuardTab.MAP)
                            },
                            onOpenAlerts = { switchTab(LandGuardTab.ALERTS) },
                            onOpenAlert = { openAlert(it) },
                            target = riskTarget,
                            onTargetConsumed = { riskTarget = null },
                            viewModel = riskViewModel
                        )

                        LandGuardTab.ALERTS -> AlertsTabScreen(
                            alerts = alerts,
                            waitingForAlertId = notificationAlertId,
                            onAlertSelected = { selectedAlertId = it.id }
                        )

                        LandGuardTab.MORE -> MoreTabScreen(
                            notificationsEnabled = notificationsEnabled,
                            locationState = location.state,
                            serverStatus = serverStatusLine(riskState),
                            onNotificationsChanged = { notificationsEnabled = it },
                            onLocationRequest = { showLocationSheet = true },
                            onOpenParcels = { showParcels = true },
                            onOpenSatellite = { showSatelliteScreen = true },
                            onOpenReport = { showReport = true },
                            onOpenNotificationSettings = { openAppNotificationSettings(context) },
                            onRetryServer = {
                                riskViewModel.refresh(force = true)
                                alertsViewModel.syncNow()
                            }
                        )
                    }
                }
            }

            // Parcels (moved under More to make room for Risk Areas)
            AnimatedVisibility(
                visible = showParcels,
                enter = slideInHorizontally(tween(280)) { it } + fadeIn(tween(200)),
                exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(200))
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(BgDeep)
                        .statusBarsPadding()
                ) {
                    ParcelsScreen(
                        onOpenMap = { switchTab(LandGuardTab.MAP) }
                    )
                }
            }

            LandGuardBottomNavigation(
                selectedTab = selectedTab,
                alertCount = if (notificationsEnabled) alerts.count { it.status == AlertStatus.NEW } else 0,
                onTabSelected = { switchTab(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Report an observation (full screen, above the navigation bar)
            AnimatedVisibility(
                visible = showReport,
                enter = slideInHorizontally(tween(280)) { it } + fadeIn(tween(200)),
                exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(200))
            ) {
                val loc = location.state.location
                val nearest = loc?.let { nearestArea(riskState.areas, it.latitude, it.longitude) }
                    ?.takeIf { it.second <= 25.0 }?.first
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(BgDeep)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    ReportScreen(
                        place = ReportPlace(loc?.latitude, loc?.longitude, nearest?.id, nearest?.name),
                        placeLabel = location.state.placeLabel,
                        onClose = { showReport = false },
                        onSubmitted = {
                            showReport = false
                            Toast.makeText(context, "Report sent to LandGuard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════
        // ALERT DETAIL
        // ═══════════════════════════════════════════════════════════

        selectedAlertId?.let { id ->
            val alert = alerts.firstOrNull { it.id == id }
            if (alert != null) {
                AlertDetailBottomSheet(
                    alert = alert,
                    onDismiss = { selectedAlertId = null },
                    onShowOnMap = if (alert.parcelId.isNotBlank() || (alert.latitude != null && alert.longitude != null)) {
                        {
                            selectedAlertId = null
                            riskTarget = MapTarget(
                                areaId = alert.parcelId.ifBlank { null },
                                latitude = alert.latitude,
                                longitude = alert.longitude,
                                label = alert.affectedLocation.ifBlank { alert.title }
                            )
                            switchTab(LandGuardTab.RISK)
                        }
                    } else null
                )
            }
        }

        if (showSatelliteScreen) {
            ModalBottomSheet(
                onDismissRequest = { showSatelliteScreen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = BgDeep
            ) {
                SatelliteScreen(onBack = { showSatelliteScreen = false }, viewModel = riskViewModel)
            }
        }

        // ═══════════════════════════════════════════════════════════
        // LOCATION
        // ═══════════════════════════════════════════════════════════

        if (showLocationSheet) {
            LocationSheet(
                state = location.state,
                onUseDeviceLocation = {
                    showLocationSheet = false
                    location.request()
                },
                onDismiss = { showLocationSheet = false }
            )
        }
    }
}

/** Connection to the production LandGuard API, as shown under More. */
private fun serverStatusLine(state: RiskAreasUiState): String = when {
    !state.online -> "Offline · ${BackendConfig.host} · cached data only"
    state.backendReachable == true -> "Connected · ${BackendConfig.host}"
    state.backendReachable == false -> "Unreachable · ${BackendConfig.host} · public sources in use · tap to retry"
    else -> "Connecting · ${BackendConfig.host}"
}

private fun openAppNotificationSettings(context: Context) {
    val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

// ═════════════════════════════════════════════════════════════════════
// LOCATION CONTROLLER
// ═════════════════════════════════════════════════════════════════════

private class UserLocationController(
    val state: UserLocationState,
    val request: () -> Unit
)

@Composable
private fun rememberUserLocationController(canPrompt: Boolean): UserLocationController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember {
        mutableStateOf(
            UserLocationState(
                status = if (hasLocationPermission(context)) LocationStatus.LOCATING else LocationStatus.PENDING
            )
        )
    }
    var fetchGeneration by remember { mutableIntStateOf(0) }
    var userInitiated by remember { mutableStateOf(false) }
    var autoPrompted by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it } || hasLocationPermission(context)
        if (granted) {
            state = state.copy(status = LocationStatus.LOCATING)
            fetchGeneration++
        } else {
            state = state.copy(status = LocationStatus.DENIED)
            val activity = context as? Activity
            val permanentlyDenied = activity != null &&
                    !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            if (userInitiated && permanentlyDenied) {
                // The system will no longer show the dialog; send the user to app settings.
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
        userInitiated = false
    }

    val launchPermission = {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    // Launch-time: ask once, after the notification prompt has settled.
    LaunchedEffect(canPrompt) {
        if (hasLocationPermission(context)) {
            fetchGeneration++
        } else if (canPrompt && !autoPrompted) {
            autoPrompted = true
            launchPermission()
        } else if (autoPrompted) {
            state = state.copy(status = LocationStatus.DENIED)
        }
    }

    LaunchedEffect(fetchGeneration) {
        if (fetchGeneration == 0) return@LaunchedEffect
        state = state.copy(status = if (state.location == null) LocationStatus.LOCATING else state.status)
        val fix = fetchUserLocation(context)
        if (fix == null) {
            state = state.copy(status = if (state.location != null) LocationStatus.READY else LocationStatus.UNAVAILABLE)
            return@LaunchedEffect
        }
        state = state.copy(location = fix, status = LocationStatus.READY)

        val place = withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) return@runCatching null
                @Suppress("DEPRECATION")
                Geocoder(context).getFromLocation(fix.latitude, fix.longitude, 1)
                    ?.firstOrNull()
                    ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
            }.getOrNull()
        }
        if (place != null) state = state.copy(placeLabel = place)
    }

    val request: () -> Unit = remember {
        {
            if (hasLocationPermission(context)) {
                scope.launch { fetchGeneration++ }
            } else {
                userInitiated = true
                launchPermission()
            }
        }
    }

    return UserLocationController(state, request)
}

// ═════════════════════════════════════════════════════════════════════
// BOTTOM NAVIGATION — floating pill with sliding indicator
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun LandGuardBottomNavigation(
    selectedTab: LandGuardTab,
    alertCount: Int,
    onTabSelected: (LandGuardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = LandGuardTab.entries

    GlassSurface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 16.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            val itemWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedTab.ordinal,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "navIndicator"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BrandContainer)
                    .border(1.dp, BrandPrimary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            )

            Row(Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    BottomNavItem(
                        tab = tab,
                        selected = tab == selectedTab,
                        badge = if (tab == LandGuardTab.ALERTS) alertCount else 0,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: LandGuardTab,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint by animateColorAsState(
        if (selected) BrandPrimaryLight else TextMuted,
        tween(220),
        label = "navTint"
    )
    Column(
        modifier = modifier.pressClickable(pressedScale = 0.9f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-5).dp)
                        .height(15.dp)
                        .clip(RoundedCornerShape(50))
                        .background(RiskCritical)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badge > 9) "9+" else badge.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 9.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═════════════════════════════════════════════════════════════════════
// ALERTS
// ═════════════════════════════════════════════════════════════════════

private enum class AlertFilter(val label: String, val severity: Severity?) {
    ALL("All", null),
    CRITICAL("Critical", Severity.CRITICAL),
    HIGH("High", Severity.HIGH),
    MODERATE("Moderate", Severity.MODERATE)
}

@Composable
private fun AlertsTabScreen(
    alerts: List<Alert>,
    waitingForAlertId: String?,
    onAlertSelected: (Alert) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf(AlertFilter.ALL) }

    val filteredAlerts = remember(alerts, selectedFilter) {
        selectedFilter.severity?.let { sev -> alerts.filter { it.severity == sev } } ?: alerts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Text(
                text = "Alerts",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = run {
                    val active = alerts.count { it.status != AlertStatus.RESOLVED }
                    if (active > 0) "$active active · ${alerts.size} total" else "Stay informed about changes affecting your land"
                },
                color = TextSecondary,
                fontSize = 12.sp
            )

            // A notification was tapped but its alert is not on this device yet (sync in progress).
            if (waitingForAlertId != null && alerts.none { it.id == waitingForAlertId }) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = BrandPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Opening the alert from your notification — fetching it from LandGuard…",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlertFilter.entries.forEach { filter ->
                    AlertFilterChip(
                        filter = filter,
                        count = filter.severity?.let { sev -> alerts.count { it.severity == sev } } ?: alerts.size,
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }
        }

        AnimatedContent(
            targetState = filteredAlerts.isEmpty(),
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "alertsEmpty"
        ) { isEmpty ->
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = FloatingNavClearance),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(BrandContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Landscape,
                                contentDescription = null,
                                tint = BrandPrimaryLight,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("All quiet", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("No alerts match this filter.", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = FloatingNavClearance + 40.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = filteredAlerts, key = { it.id }) { alert ->
                        AlertRow(
                            alert = alert,
                            onClick = { onAlertSelected(alert) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertRow(
    alert: Alert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNew = alert.status == AlertStatus.NEW
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgSurface)
            .border(1.dp, if (isNew) alert.severity.accent.copy(alpha = 0.3f) else BgBorder, RoundedCornerShape(18.dp))
            .pressClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Severity rail
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(50))
                .background(alert.severity.accent)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeverityPill(alert.severity)
                if (isNew) {
                    Spacer(Modifier.width(6.dp))
                    SeverityDot(alert.severity, size = 6.dp, pulsing = alert.severity >= Severity.HIGH)
                    Text("NEW", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = alert.timestamp.ifBlank { "Recently" },
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = alert.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = TextMuted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    text = alert.affectedLocation.ifBlank { "Location unavailable" },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (alert.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = alert.description,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AlertFilterChip(
    filter: AlertFilter,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = filter.severity?.accent ?: BrandPrimaryLight
    val bg by animateColorAsState(
        if (selected) (filter.severity?.container ?: BrandContainer) else BgSurface,
        tween(200),
        label = "chipBg"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, if (selected) accent.copy(alpha = 0.6f) else BgBorder, RoundedCornerShape(12.dp))
            .pressClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = filter.label,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "$count",
            color = if (selected) accent else TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═════════════════════════════════════════════════════════════════════
// MORE
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun MoreTabScreen(
    notificationsEnabled: Boolean,
    locationState: UserLocationState,
    serverStatus: String,
    onNotificationsChanged: (Boolean) -> Unit,
    onLocationRequest: () -> Unit,
    onOpenParcels: () -> Unit,
    onOpenSatellite: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRetryServer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = FloatingNavClearance + 40.dp)
    ) {

        Text(
            text = "More",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Tools, data and preferences",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(18.dp))

        // Identity card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BgSurface)
                .border(1.dp, BgBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandContainer)
                    .border(1.dp, BrandPrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, tint = BrandPrimaryLight, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Land Explorer", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = when (locationState.status) {
                        LocationStatus.READY -> "Monitoring near " + (locationState.placeLabel ?: "your location")
                        LocationStatus.DENIED -> "Location access is off"
                        else -> "LandGuard user"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        MoreSection("Land & data")

        MoreRow(
            icon = Icons.Filled.Layers,
            title = "My Parcels",
            subtitle = "Monitored land parcels and details",
            onClick = onOpenParcels
        )
        MoreRow(
            icon = Icons.Filled.Satellite,
            title = "Satellite Data",
            subtitle = "ALOS-4 and Sentinel-2 observations",
            onClick = onOpenSatellite
        )

        MoreSection("Reports")

        MoreRow(
            icon = Icons.Filled.EditNote,
            title = "Report an observation",
            subtitle = "Send a geotagged field report to LandGuard authorities",
            onClick = onOpenReport
        )

        MoreSection("Settings")

        MoreRow(
            icon = if (notificationsEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
            title = "Alert badge",
            subtitle = if (notificationsEnabled) "Showing new-alert count" else "Hidden",
            trailing = { ToggleIndicator(on = notificationsEnabled) },
            onClick = { onNotificationsChanged(!notificationsEnabled) }
        )
        MoreRow(
            icon = Icons.Filled.LocationOn,
            title = "Location",
            subtitle = when (locationState.status) {
                LocationStatus.READY -> "Using device location"
                LocationStatus.DENIED -> "Permission denied · Tap to enable"
                LocationStatus.UNAVAILABLE -> "No fix available · Tap to retry"
                else -> "Locating…"
            },
            onClick = onLocationRequest
        )
        MoreRow(
            icon = Icons.Filled.NotificationsActive,
            title = "Alert notifications",
            subtitle = "System notification settings for LandGuard alerts",
            onClick = onOpenNotificationSettings
        )
        MoreRow(
            icon = Icons.Filled.Cloud,
            title = "LandGuard server",
            subtitle = serverStatus,
            onClick = onRetryServer
        )
    }
}

@Composable
private fun MoreSection(title: String) {
    Text(
        text = title.uppercase(),
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun ToggleIndicator(on: Boolean) {
    val track by animateColorAsState(if (on) BrandPrimary else BgBorder, tween(200), label = "toggleTrack")
    val knobOffset by animateDpAsState(if (on) 18.dp else 2.dp, spring(stiffness = Spring.StiffnessMedium), label = "toggleKnob")
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .align(Alignment.CenterStart)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BgSurface)
            .border(1.dp, BgBorder, RoundedCornerShape(16.dp))
            .pressClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = BrandPrimaryLight, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// LOCATION SHEET
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun LocationSheet(
    state: UserLocationState,
    onUseDeviceLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BgSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 36.dp)
        ) {
            Text("Your location", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "LandGuard centres the map on you and highlights risk areas nearby. Your location stays on this device.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            state.location?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = (state.placeLabel?.let { "$it · " } ?: "") + "%.4f, %.4f".format(it.latitude, it.longitude),
                    color = BrandPrimaryLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandContainer)
                    .border(1.dp, BrandPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .pressClickable(pressedScale = 0.98f, onClick = onUseDeviceLocation)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocationOn, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (state.location != null) "Refresh device location" else "Use device location",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (state.status == LocationStatus.DENIED) "Opens the permission prompt or app settings"
                        else "Centre the map on your current position",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = TextSecondary)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// ALERT DETAIL
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun AlertDetailBottomSheet(
    alert: Alert,
    onDismiss: () -> Unit,
    onShowOnMap: (() -> Unit)?
) {
    val viewModel: AlertDetailViewModel = hiltViewModel()
    val severityColor = alert.severity.accent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 30.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(alert.severity.container)
                        .border(1.dp, severityColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Warning, null, tint = severityColor, modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "LAND RISK ALERT",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    SeverityPill(alert.severity)
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = alert.title,
                color = TextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 27.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = alert.description.ifBlank {
                    "A land-risk event has been detected in the monitored area."
                },
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgElevated)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                AlertDetailRow("Location", alert.affectedLocation.ifBlank { "Unknown" })
                if (alert.latitude != null && alert.longitude != null) {
                    AlertDetailRow("Coordinates", "%.4f, %.4f".format(java.util.Locale.US, alert.latitude, alert.longitude))
                }
                AlertDetailRow("Detected event", alert.detectedEvent.ifBlank { "Land change detected" })
                if (alert.isDemoData) AlertDetailRow("Confidence", "${alert.confidencePercentage}%")
                AlertDetailRow("Status", alert.status.name.lowercase().replaceFirstChar { it.uppercase() })
                if (alert.serverStatus != "active") AlertDetailRow("Authority status", alert.serverStatus.replaceFirstChar { it.uppercase() })
                AlertDetailRow("Source", alert.sourceProvider)
                if (alert.timestamp.isNotBlank()) AlertDetailRow("Issued", formatAlertTime(alert.timestamp))
                alert.expiresAt?.let { AlertDetailRow("Valid until", formatAlertTime(it)) }
                if (alert.receivedVia.isNotBlank()) {
                    AlertDetailRow(
                        "Received via",
                        when (alert.receivedVia) {
                            "mesh" -> "Offline mesh · ${alert.hopCount} hop${if (alert.hopCount == 1) "" else "s"}"
                            "sync" -> "Sync after reconnect"
                            else -> "Push notification"
                        }
                    )
                }
                if (!alert.isDemoData) AlertDetailRow("Alert ID", alert.id)
            }

            if (alert.status == AlertStatus.NEW || onShowOnMap != null) {
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (alert.status == AlertStatus.NEW) {
                        PanelButton(
                            text = "Acknowledge",
                            primary = true,
                            onClick = {
                                viewModel.updateAlertStatus(alert.id, AlertStatus.ACKNOWLEDGED)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (onShowOnMap != null) {
                        PanelButton(
                            text = "Show on map",
                            primary = alert.status != AlertStatus.NEW,
                            onClick = onShowOnMap,
                            modifier = Modifier.weight(1f)
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
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

/** ISO-8601 UTC → local "d MMM yyyy, HH:mm"; other formats are shown as-is. */
private fun formatAlertTime(value: String): String {
    val millis = com.example.landguard.data.alerts.AlertContract.parseIso(value) ?: return value
    return java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))
}
