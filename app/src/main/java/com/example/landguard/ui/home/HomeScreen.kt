package com.example.landguard.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.AlertStatus
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.components.GlassSurface
import com.example.landguard.ui.components.LandGuardRiskMap
import com.example.landguard.ui.components.LandGuardWordmark
import com.example.landguard.ui.components.MapControlButton
import com.example.landguard.ui.components.MapFitAll
import com.example.landguard.ui.components.MapFocus
import com.example.landguard.ui.components.MapStyleSwitcher
import com.example.landguard.ui.components.MetricTile
import com.example.landguard.ui.components.RiskMapStyle
import com.example.landguard.ui.components.SeverityDot
import com.example.landguard.ui.components.SeverityPill
import com.example.landguard.ui.components.accent
import com.example.landguard.ui.components.container
import com.example.landguard.ui.components.pressClickable
import com.example.landguard.ui.location.LocalRequestUserLocation
import com.example.landguard.ui.location.LocalUserLocation
import com.example.landguard.ui.location.LocationStatus
import com.example.landguard.ui.location.UserLocation
import com.example.landguard.ui.location.distanceKm
import com.example.landguard.ui.location.formatDistance
import com.example.landguard.ui.risk.RiskArea
import com.example.landguard.ui.risk.RiskAreasViewModel
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.BrandPrimaryLight
import com.example.landguard.ui.theme.EarthOchre
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary

/** Space reserved at the bottom of full-screen content for the floating nav bar. */
val FloatingNavClearance = 92.dp

private const val NEARBY_RADIUS_KM = 75.0

@Composable
fun HomeScreen(
    onOpenAlert: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenAlertHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenRiskAreas: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    riskViewModel: RiskAreasViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val riskState by riskViewModel.uiState.collectAsStateWithLifecycle()
    val locationState = LocalUserLocation.current
    val requestLocation = LocalRequestUserLocation.current
    val userLocation = locationState.location

    var selectedAreaId by remember { mutableStateOf<String?>(null) }
    var mapStyle by remember { mutableStateOf(RiskMapStyle.STREETS) }
    var panelDismissed by remember { mutableStateOf(false) }
    var showStyles by remember { mutableStateOf(false) }
    var focus by remember { mutableStateOf<MapFocus?>(null) }
    var fitAll by remember { mutableStateOf<MapFitAll?>(null) }
    var focusToken by remember { mutableIntStateOf(0) }

    val activeAlerts = uiState.alerts.count {
        it.severity != Severity.LOW && it.status != AlertStatus.RESOLVED
    }
    val latestAlert = uiState.alerts.firstOrNull { it.status != AlertStatus.RESOLVED }

    // Nearest first when we know where the user is, otherwise highest ranked first.
    val orderedAreas = remember(riskState.areas, userLocation) {
        if (userLocation == null) riskState.areas
        else riskState.areas.sortedBy { distanceKm(userLocation, it.latitude, it.longitude) }
    }
    val nearbyAreas = remember(orderedAreas, userLocation) {
        if (userLocation == null) emptyList()
        else orderedAreas.filter { distanceKm(userLocation, it.latitude, it.longitude) <= NEARBY_RADIUS_KM }
    }
    val selectedArea = riskState.areas.firstOrNull { it.id == selectedAreaId }

    fun focusOn(area: RiskArea) {
        selectedAreaId = area.id
        focus = MapFocus(area.latitude, area.longitude, zoom = 11.5, token = ++focusToken)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {

        // ── Map ────────────────────────────────────────────────────────────
        LandGuardRiskMap(
            areas = riskState.areas,
            userLocation = userLocation,
            selectedAreaId = selectedAreaId,
            onAreaClick = { focusOn(it) },
            onMapBackgroundClick = {
                selectedAreaId = null
                showStyles = false
            },
            mapStyle = mapStyle,
            focus = focus,
            fitAll = fitAll,
            topInset = 110.dp,
            bottomInset = 300.dp,
            modifier = Modifier.fillMaxSize()
        )

        // Top scrim keeps the header legible over any basemap.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(Brush.verticalGradient(listOf(BgDeep.copy(alpha = 0.92f), Color.Transparent)))
        )

        // ── Header ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LandGuardWordmark(
                    modifier = Modifier.pressClickable(onClick = onOpenProfile)
                )
                Spacer(Modifier.weight(1f))
                Box {
                    MapControlButton(
                        icon = Icons.Filled.Notifications,
                        contentDescription = "Alerts",
                        onClick = onOpenAlertHistory
                    )
                    if (activeAlerts > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(RiskCritical)
                                .border(2.dp, BgDeep, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (activeAlerts > 9) "9+" else activeAlerts.toString(),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            LocationChip(
                status = locationState.status,
                placeLabel = locationState.placeLabel,
                onClick = {
                    val loc = userLocation
                    if (loc != null) {
                        selectedAreaId = null
                        focus = MapFocus(loc.latitude, loc.longitude, 11.0, ++focusToken)
                    } else {
                        requestLocation()
                    }
                }
            )
        }

        // ── Floating map controls ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 124.dp, end = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            AnimatedVisibility(
                visible = showStyles,
                enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.9f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)),
                exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.9f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f))
            ) {
                MapStyleSwitcher(
                    current = mapStyle,
                    onSelect = {
                        mapStyle = it
                        showStyles = false
                    },
                    modifier = Modifier.padding(end = 10.dp, top = 56.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MapControlButton(
                    icon = Icons.Filled.MyLocation,
                    contentDescription = "My location",
                    tint = if (userLocation != null) BrandPrimaryLight else TextPrimary,
                    onClick = {
                        val loc = userLocation
                        if (loc != null) {
                            selectedAreaId = null
                            focus = MapFocus(loc.latitude, loc.longitude, 11.0, ++focusToken)
                        } else {
                            requestLocation()
                        }
                    }
                )
                MapControlButton(
                    icon = Icons.Filled.Layers,
                    contentDescription = "Map style",
                    active = showStyles,
                    onClick = { showStyles = !showStyles }
                )
                MapControlButton(
                    icon = Icons.Filled.ZoomOutMap,
                    contentDescription = "Show all areas",
                    onClick = {
                        selectedAreaId = null
                        fitAll = MapFitAll(++focusToken)
                    }
                )
            }
        }

        // ── Bottom panel ───────────────────────────────────────────────────
        val panelVisible = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = panelVisible,
            enter = slideInVertically(tween(420)) { it / 2 } + fadeIn(tween(420)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = FloatingNavClearance)
        ) {
            val panel: HomePanel = when {
                selectedArea != null -> HomePanel.Detail(selectedArea)
                panelDismissed -> HomePanel.Collapsed
                else -> HomePanel.Nearby
            }
            AnimatedContent(
                targetState = panel,
                transitionSpec = {
                    (slideInVertically(tween(260)) { it / 3 } + fadeIn(tween(260))) togetherWith
                            (slideOutVertically(tween(180)) { it / 3 } + fadeOut(tween(180)))
                },
                contentKey = { it.key },
                contentAlignment = Alignment.BottomCenter,
                label = "homePanel"
            ) { target ->
                when (target) {
                    HomePanel.Nearby -> NearbyPanel(
                        isLoading = riskState.isLoading,
                        locationStatus = locationState.status,
                        userLocation = userLocation,
                        nearbyAreas = nearbyAreas,
                        orderedAreas = orderedAreas,
                        activeAlerts = activeAlerts,
                        latestAlert = latestAlert,
                        onAreaClick = { focusOn(it) },
                        onSeeAll = onOpenRiskAreas,
                        onOpenAlert = onOpenAlert,
                        onClose = { panelDismissed = true }
                    )

                    HomePanel.Collapsed -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ShowDetailsPill(
                            areaCount = if (nearbyAreas.isNotEmpty()) nearbyAreas.size else orderedAreas.size,
                            nearby = nearbyAreas.isNotEmpty(),
                            highest = (nearbyAreas.ifEmpty { orderedAreas }).maxByOrNull { it.severity.ordinal }?.severity,
                            onClick = { panelDismissed = false }
                        )
                    }

                    is HomePanel.Detail -> AreaDetailPanel(
                        area = target.area,
                        userLocation = userLocation,
                        onClose = { selectedAreaId = null },
                        onAnalyze = onOpenMap,
                        onOpenAlert = onOpenAlert,
                        onOpenAlerts = onOpenAlertHistory
                    )
                }
            }
        }
    }
}

private sealed class HomePanel(val key: String) {
    data object Nearby : HomePanel("nearby")
    data object Collapsed : HomePanel("collapsed")
    data class Detail(val area: RiskArea) : HomePanel("area:${area.id}")
}

// ═════════════════════════════════════════════════════════════════════
// COLLAPSED PANEL PILL
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ShowDetailsPill(
    areaCount: Int,
    nearby: Boolean,
    highest: Severity?,
    onClick: () -> Unit
) {
    GlassSurface(
        shape = RoundedCornerShape(50),
        elevation = 8.dp,
        modifier = Modifier
            .padding(bottom = 4.dp)
            .pressClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (highest != null) {
                SeverityDot(severity = highest, size = 8.dp)
            } else {
                Icon(Icons.Filled.Radar, null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$areaCount ${if (nearby) "risk areas nearby" else "monitored areas"}",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Show details", tint = BrandPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// LOCATION CHIP
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun LocationChip(
    status: LocationStatus,
    placeLabel: String?,
    onClick: () -> Unit
) {
    GlassSurface(
        shape = RoundedCornerShape(50),
        elevation = 6.dp,
        modifier = Modifier.pressClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (status) {
                LocationStatus.PENDING, LocationStatus.LOCATING -> CircularProgressIndicator(
                    color = BrandPrimaryLight,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(12.dp)
                )
                LocationStatus.READY -> Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(BrandPrimaryLight)
                )
                else -> Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(EarthOchre)
                )
            }
            Spacer(Modifier.width(8.dp))
            AnimatedContent(
                targetState = when (status) {
                    LocationStatus.PENDING, LocationStatus.LOCATING -> "Finding your location…"
                    LocationStatus.READY -> "Near you" + (placeLabel?.let { " · $it" } ?: "")
                    LocationStatus.DENIED -> "Location off · Tap to enable"
                    LocationStatus.UNAVAILABLE -> "Location unavailable · Retry"
                },
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "locationLabel"
            ) { text ->
                Text(
                    text = text,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// NEARBY PANEL
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun NearbyPanel(
    isLoading: Boolean,
    locationStatus: LocationStatus,
    userLocation: UserLocation?,
    nearbyAreas: List<RiskArea>,
    orderedAreas: List<RiskArea>,
    activeAlerts: Int,
    latestAlert: Alert?,
    onAreaClick: (RiskArea) -> Unit,
    onSeeAll: () -> Unit,
    onOpenAlert: (String) -> Unit,
    onClose: () -> Unit
) {
    val highestNearby = nearbyAreas.maxByOrNull { it.severity.ordinal }?.severity
    val cards = if (nearbyAreas.isNotEmpty()) nearbyAreas else orderedAreas

    val (headline, subline) = when {
        isLoading -> "Scanning terrain…" to "Loading monitored areas"
        userLocation == null && locationStatus != LocationStatus.READY ->
            "Monitored risk areas" to "${orderedAreas.size} areas · $activeAlerts active alerts"
        nearbyAreas.isEmpty() ->
            "No monitored risk near you" to "Nearest areas shown below"
        highestNearby != null && highestNearby >= Severity.HIGH ->
            "Elevated risk nearby" to "${nearbyAreas.size} areas within ${NEARBY_RADIUS_KM.toInt()} km · $activeAlerts active alerts"
        else ->
            "Conditions stable nearby" to "${nearbyAreas.size} areas within ${NEARBY_RADIUS_KM.toInt()} km"
    }

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, bottom = 14.dp)) {

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (highestNearby != null) {
                    SeverityDot(severity = highestNearby, size = 9.dp)
                } else {
                    Icon(Icons.Filled.Radar, null, tint = BrandPrimaryLight, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(headline, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(subline, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .pressClickable(onClick = onSeeAll)
                        .padding(start = 10.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ranked", color = BrandPrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.ChevronRight, null, tint = BrandPrimaryLight, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                MapControlButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Hide details",
                    onClick = onClose,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandPrimaryLight, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cards.take(8), key = { it.id }) { area ->
                        NearbyAreaCard(
                            area = area,
                            distance = userLocation?.let { formatDistance(distanceKm(it, area.latitude, area.longitude)) },
                            onClick = { onAreaClick(area) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            if (latestAlert != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(latestAlert.severity.container)
                        .pressClickable(pressedScale = 0.98f) { onOpenAlert(latestAlert.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, null, tint = latestAlert.severity.accent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = latestAlert.title,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = latestAlert.affectedLocation.ifBlank { "Latest alert" } +
                                    (latestAlert.timestamp.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun NearbyAreaCard(
    area: RiskArea,
    distance: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(196.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BgElevated)
            .border(1.dp, area.severity.accent.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .pressClickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SeverityPill(area.severity)
            Spacer(Modifier.weight(1f))
            Text(
                text = distance ?: "${area.score}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = area.name,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = buildString {
                append("${"%.1f".format(area.displacementMmPerYr)} mm/yr")
                if (area.activeAlertCount > 0) append(" · ${area.activeAlertCount} alert${if (area.activeAlertCount > 1) "s" else ""}")
            },
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(10.dp))
        RiskScoreBar(score = area.score, severity = area.severity)
    }
}

@Composable
fun RiskScoreBar(score: Int, severity: Severity, modifier: Modifier = Modifier) {
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(score) {
        progress.animateTo(score / 100f, tween(700))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(BgBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.value)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(severity.accent.copy(alpha = 0.6f), severity.accent)))
        )
    }
}

// ═════════════════════════════════════════════════════════════════════
// SELECTED AREA PANEL
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun AreaDetailPanel(
    area: RiskArea,
    userLocation: UserLocation?,
    onClose: () -> Unit,
    onAnalyze: () -> Unit,
    onOpenAlert: (String) -> Unit,
    onOpenAlerts: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeverityPill(area.severity)
                        userLocation?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = formatDistance(distanceKm(it, area.latitude, area.longitude)) + " away",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = area.name,
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (area.lastUpdated.isNotBlank()) {
                        Text(area.lastUpdated, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                    }
                }
                MapControlButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Close",
                    onClick = onClose,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("${area.score}", "Risk score", Modifier.weight(1f), accent = area.severity.accent)
                MetricTile("%.1f".format(area.displacementMmPerYr), "mm/yr shift", Modifier.weight(1f))
                MetricTile("${area.soilMoisturePercent}%", "Soil moisture", Modifier.weight(1f))
                MetricTile("${area.slopeDegrees.toInt()}°", "Slope", Modifier.weight(1f))
            }

            area.latestAlert?.let { alert ->
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(alert.severity.container)
                        .pressClickable(pressedScale = 0.98f) { onOpenAlert(alert.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, null, tint = alert.severity.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = alert.title,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PanelButton(
                    text = "Analyze on map",
                    primary = true,
                    onClick = onAnalyze,
                    modifier = Modifier.weight(1f)
                )
                PanelButton(
                    text = if (area.activeAlertCount > 0) "Alerts (${area.activeAlertCount})" else "Alerts",
                    primary = false,
                    onClick = onOpenAlerts,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PanelButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) BrandPrimary else BgElevated)
            .border(1.dp, if (primary) Color.Transparent else BgBorder, RoundedCornerShape(14.dp))
            .pressClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (primary) Color.White else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
