@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.landguard.ui.risk

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.data.regional.DataResult
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.components.GlassSurface
import com.example.landguard.ui.components.LandGuardRiskMap
import com.example.landguard.ui.components.MapControlButton
import com.example.landguard.ui.components.MapFitAll
import com.example.landguard.ui.components.MapFocus
import com.example.landguard.ui.components.MapPin
import com.example.landguard.ui.components.RiskMapStyle
import com.example.landguard.ui.components.SeverityDot
import com.example.landguard.ui.components.SeverityPill
import com.example.landguard.ui.components.accent
import com.example.landguard.ui.components.container
import com.example.landguard.ui.components.label
import com.example.landguard.ui.components.pressClickable
import com.example.landguard.ui.home.FloatingNavClearance
import com.example.landguard.ui.home.PanelButton
import com.example.landguard.ui.home.RiskScoreBar
import com.example.landguard.ui.location.LocalRequestUserLocation
import com.example.landguard.ui.location.LocalUserLocation
import com.example.landguard.ui.location.LocationStatus
import com.example.landguard.ui.location.UserLocation
import com.example.landguard.ui.location.distanceKm
import com.example.landguard.ui.location.formatDistance
import com.example.landguard.ui.monitor.headline
import com.example.landguard.ui.monitor.summaryLine
import com.example.landguard.ui.navigation.MapTarget
import com.example.landguard.ui.search.PlaceResult
import com.example.landguard.ui.search.PlaceSearchBar
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.BrandContainer
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.BrandPrimaryLight
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary

private enum class RiskView { MAP, LIST }

/** Ranking of monitored areas. */
enum class AreaSort(val label: String) {
    RISK("Risk score"),
    SEVERITY("Severity"),
    NEAREST("Nearest"),
    RECENT("Latest slide"),
    RAIN("Rain 72 h"),
    ALERTS("Alerts")
}

fun List<RiskArea>.sortedFor(sort: AreaSort, user: UserLocation?): List<RiskArea> = when (sort) {
    AreaSort.RISK -> sortedWith(compareByDescending<RiskArea> { it.score }.thenByDescending { it.eventCount })
    AreaSort.SEVERITY -> sortedWith(compareByDescending<RiskArea> { it.severity.ordinal }.thenByDescending { it.score })
    AreaSort.NEAREST -> if (user == null) this else sortedBy { distanceKm(user, it.latitude, it.longitude) }
    AreaSort.RECENT -> sortedWith(compareByDescending<RiskArea> { it.lastEventMillis ?: Long.MIN_VALUE }.thenByDescending { it.score })
    AreaSort.RAIN -> sortedWith(compareByDescending<RiskArea> { it.rain72hMm ?: -1.0 }.thenByDescending { it.score })
    AreaSort.ALERTS -> sortedWith(compareByDescending<RiskArea> { it.activeAlertCount }.thenByDescending { it.score })
}

/** What the details sheet is showing. */
private sealed interface RiskDetail {
    data class Area(val id: String) : RiskDetail
    data class Place(val place: PlaceResult) : RiskDetail
    data object Mine : RiskDetail
}

@Composable
fun RiskAreasScreen(
    onOpenOnMap: (MapTarget) -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenAlert: (String) -> Unit,
    target: MapTarget? = null,
    onTargetConsumed: () -> Unit = {},
    viewModel: RiskAreasViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val areaAnalysis by viewModel.areaAnalysis.collectAsStateWithLifecycle()
    val userAnalysis by viewModel.userAnalysis.collectAsStateWithLifecycle()
    val locationState = LocalUserLocation.current
    val userLocation = locationState.location
    val requestLocation = LocalRequestUserLocation.current
    val density = LocalDensity.current

    var view by rememberSaveable { mutableStateOf(RiskView.LIST) }
    var filter by rememberSaveable { mutableStateOf<Severity?>(null) }
    var stateFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(AreaSort.RISK) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<RiskDetail?>(null) }
    var pin by remember { mutableStateOf<MapPin?>(null) }
    var focus by remember { mutableStateOf<MapFocus?>(null) }
    var fitAll by remember { mutableStateOf<MapFitAll?>(null) }
    var token by remember { mutableIntStateOf(0) }
    var mapStyle by rememberSaveable { mutableStateOf(RiskMapStyle.STREETS) }
    var threeD by rememberSaveable { mutableStateOf(true) }
    var showZones by rememberSaveable { mutableStateOf(true) }
    var showLayers by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeight = with(density) { headerHeightPx.toDp() }.coerceAtLeast(200.dp)

    val visibleAreas = remember(state.areas, filter, stateFilter, sort, userLocation) {
        state.areas
            .filter { filter == null || it.severity == filter }
            .filter { stateFilter == null || it.state == stateFilter }
            .sortedFor(sort, userLocation)
    }
    val riskRanks = remember(state.areas) {
        state.areas.sortedFor(AreaSort.RISK, null).mapIndexed { i, area -> area.id to i + 1 }.toMap()
    }
    val carouselState = rememberLazyListState()

    // Analyse the device position with real data (shared with Home via the same ViewModel).
    LaunchedEffect(userLocation) {
        userLocation?.let { viewModel.analyzeUserLocation(it.latitude, it.longitude) }
    }

    fun openArea(area: RiskArea) {
        selectedId = area.id
        pin = null
        focus = MapFocus(area.latitude, area.longitude, 11.5, ++token)
        detail = RiskDetail.Area(area.id)
        viewModel.analyzeArea(area)
    }

    fun openPlace(place: PlaceResult) {
        selectedId = null
        pin = MapPin(place.latitude, place.longitude)
        focus = MapFocus(place.latitude, place.longitude, 11.5, ++token)
        detail = RiskDetail.Place(place)
        viewModel.analyzePoint(place.key, place.latitude, place.longitude)
    }

    fun openMyLocation() {
        val loc = userLocation
        if (loc == null) {
            requestLocation()
            return
        }
        selectedId = null
        pin = null
        focus = MapFocus(loc.latitude, loc.longitude, 11.0, ++token)
        detail = RiskDetail.Mine
        viewModel.analyzeUserLocation(loc.latitude, loc.longitude)
    }

    // Cross-screen request (Map / Alert → Risk Areas). Waits for the areas to load, then opens it.
    LaunchedEffect(target, state.isLoading) {
        val t = target ?: return@LaunchedEffect
        if (state.isLoading) return@LaunchedEffect
        val area = t.areaId?.let { id -> state.areas.firstOrNull { it.id == id } }
        when {
            area != null -> {
                filter = null
                stateFilter = null
                view = RiskView.MAP
                openArea(area)
            }
            t.hasCoordinates -> {
                view = RiskView.MAP
                openPlace(PlaceResult(t.label ?: "Alert location", "Location reported with the alert", t.latitude!!, t.longitude!!))
            }
        }
        onTargetConsumed()
    }

    // Frame the filtered set whenever the filter changes.
    LaunchedEffect(filter, stateFilter, state.areas.size) {
        if (state.areas.isNotEmpty() && detail == null && target == null) fitAll = MapFitAll(++token)
    }

    // Keep the carousel in sync with map selection.
    LaunchedEffect(selectedId, visibleAreas) {
        val index = visibleAreas.indexOfFirst { it.id == selectedId }
        if (index >= 0) carouselState.animateScrollToItem(index)
    }

    BackHandler(enabled = searchExpanded) { searchExpanded = false }
    BackHandler(enabled = !searchExpanded && showLayers) { showLayers = false }
    BackHandler(enabled = !searchExpanded && !showLayers && view == RiskView.MAP) { view = RiskView.LIST }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {

        // ── Persistent map (kept alive under the list) ─────────────────────
        LandGuardRiskMap(
            areas = visibleAreas,
            userLocation = userLocation,
            selectedAreaId = selectedId,
            onAreaClick = { openArea(it) },
            onMapBackgroundClick = {
                selectedId = null
                pin = null
                showLayers = false
            },
            onUserClick = { openMyLocation() },
            mapStyle = mapStyle,
            threeD = threeD,
            showZones = showZones,
            pin = pin,
            focus = focus,
            fitAll = fitAll,
            topInset = headerHeight,
            bottomInset = 260.dp,
            modifier = Modifier.fillMaxSize()
        )

        // ── Ranked list overlay ────────────────────────────────────────────
        AnimatedVisibility(
            visible = view == RiskView.LIST,
            enter = fadeIn(tween(240)) + slideInVertically(tween(300)) { it / 8 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(240)) { it / 8 }
        ) {
            RankedList(
                areas = visibleAreas,
                rankOf = { area -> riskRanks[area.id] ?: 0 },
                isLoading = state.isLoading,
                error = state.catalogError,
                onRetry = { viewModel.refresh(force = true) },
                userLocation = userLocation,
                locationStatus = locationState.status,
                placeLabel = locationState.placeLabel,
                userAnalysis = userAnalysis,
                onMyLocationClick = { openMyLocation() },
                topPadding = headerHeight + 8.dp,
                onAreaClick = { openArea(it) }
            )
        }

        // ── Header ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
                .background(
                    Brush.verticalGradient(
                        0f to BgDeep,
                        0.8f to BgDeep.copy(alpha = if (view == RiskView.LIST) 1f else 0.88f),
                        1f to Color.Transparent
                    )
                )
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Risk Areas",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "${state.areas.size} monitored areas · Northeast India",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                ViewToggle(view = view, onChange = { view = it })
            }

            Spacer(Modifier.height(12.dp))

            PlaceSearchBar(
                areas = state.areas,
                expanded = searchExpanded,
                onExpandedChange = { searchExpanded = it },
                onAreaSelected = {
                    filter = null
                    stateFilter = null
                    view = RiskView.MAP
                    openArea(it)
                },
                onPlaceSelected = {
                    view = RiskView.MAP
                    openPlace(it)
                }
            )

            if (!searchExpanded) {
                Spacer(Modifier.height(10.dp))

                SeveritySummary(state = state, selected = filter, onSelect = {
                    filter = if (filter == it) null else it
                    selectedId = null
                })

                if (view == RiskView.LIST && state.statesCovered.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    StateFilter(
                        states = state.statesCovered,
                        selected = stateFilter,
                        onSelect = {
                            stateFilter = if (stateFilter == it) null else it
                            selectedId = null
                        }
                    )
                }

                if (view == RiskView.LIST) {
                    Spacer(Modifier.height(8.dp))
                    SortBar(
                        sort = sort,
                        nearestEnabled = userLocation != null,
                        onSelect = {
                            if (it == AreaSort.NEAREST && userLocation == null) requestLocation()
                            sort = it
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))
                DataStatusChip(state.dataStatus, onRetry = { viewModel.refresh(force = true) })
                if (view == RiskView.LIST) ProvenanceLine(state)
            }
        }

        // ── Map-mode controls & carousel ───────────────────────────────────
        AnimatedVisibility(
            visible = view == RiskView.MAP && !searchExpanded,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = FloatingNavClearance)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    RiskLegend(Modifier.weight(1f, fill = false))
                    Spacer(Modifier.weight(1f))
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedVisibility(visible = showLayers, enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) {
                            LayersPanel(
                                mapStyle = mapStyle,
                                onStyle = { mapStyle = it },
                                threeD = threeD,
                                onThreeD = { threeD = it },
                                showZones = showZones,
                                onShowZones = { showZones = it }
                            )
                        }
                        MapControlButton(
                            icon = Icons.Filled.Layers,
                            contentDescription = "Map layers",
                            active = showLayers,
                            onClick = { showLayers = !showLayers }
                        )
                        MapControlButton(
                            icon = Icons.Filled.ZoomOutMap,
                            contentDescription = "Recenter on all areas",
                            onClick = {
                                selectedId = null
                                pin = null
                                fitAll = MapFitAll(++token)
                            }
                        )
                        MapControlButton(
                            icon = Icons.Filled.MyLocation,
                            contentDescription = "My location",
                            tint = if (userLocation != null) BrandPrimaryLight else TextPrimary,
                            onClick = { openMyLocation() }
                        )
                    }
                }

                LazyRow(
                    state = carouselState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(visibleAreas, key = { _, it -> it.id }) { index, area ->
                        CarouselCard(
                            rank = index + 1,
                            area = area,
                            selected = area.id == selectedId,
                            userLocation = userLocation,
                            onClick = {
                                selectedId = area.id
                                pin = null
                                focus = MapFocus(area.latitude, area.longitude, 11.5, ++token)
                            },
                            onDetails = { openArea(area) },
                            onAnalyze = { onOpenOnMap(MapTarget(area.id, area.latitude, area.longitude, area.name)) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    // ── Details sheet (area, searched place or the user's own area) ───────
    detail?.let { current ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { detail = null },
            sheetState = sheetState,
            containerColor = BgSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 28.dp)
            ) {
                when (current) {
                    is RiskDetail.Area -> {
                        val area = state.areas.firstOrNull { it.id == current.id }
                        if (area == null) {
                            Text("This area is no longer in the monitored list.", color = TextSecondary, fontSize = 13.sp)
                        } else {
                            AreaDetailContent(
                                area = area,
                                analysis = areaAnalysis[area.id],
                                userLocation = userLocation,
                                onClose = { detail = null },
                                onRefresh = { viewModel.analyzeArea(area, force = true) },
                                primaryActionLabel = "Open analysis map",
                                onPrimaryAction = {
                                    detail = null
                                    onOpenOnMap(MapTarget(area.id, area.latitude, area.longitude, area.name))
                                },
                                onOpenAlert = {
                                    detail = null
                                    onOpenAlert(it)
                                },
                                onOpenAlerts = {
                                    detail = null
                                    onOpenAlerts()
                                }
                            )
                        }
                    }

                    is RiskDetail.Place -> {
                        val place = current.place
                        PlaceDetailContent(
                            title = place.title,
                            subtitle = place.subtitle,
                            latitude = place.latitude,
                            longitude = place.longitude,
                            isUserLocation = false,
                            analysis = areaAnalysis[place.key],
                            nearest = nearestArea(state.areas, place.latitude, place.longitude),
                            onClose = { detail = null },
                            onRefresh = { viewModel.analyzePoint(place.key, place.latitude, place.longitude, force = true) },
                            onOpenArea = { openArea(it) }
                        )
                    }

                    RiskDetail.Mine -> {
                        val loc = userLocation
                        if (loc == null) {
                            Text("Your location is not available.", color = TextSecondary, fontSize = 13.sp)
                        } else {
                            PlaceDetailContent(
                                title = locationState.placeLabel ?: "Your GPS position",
                                subtitle = "Device location",
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                isUserLocation = true,
                                analysis = userAnalysis,
                                nearest = nearestArea(state.areas, loc.latitude, loc.longitude),
                                onClose = { detail = null },
                                onRefresh = { viewModel.analyzeUserLocation(loc.latitude, loc.longitude, force = true) },
                                onOpenArea = { openArea(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// HEADER PIECES
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ViewToggle(view: RiskView, onChange: (RiskView) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgElevated)
            .border(1.dp, BgBorder, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        ToggleSegment(Icons.AutoMirrored.Filled.ViewList, "List", view == RiskView.LIST) { onChange(RiskView.LIST) }
        ToggleSegment(Icons.Filled.Map, "Map", view == RiskView.MAP) { onChange(RiskView.MAP) }
    }
}

@Composable
private fun ToggleSegment(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) BrandPrimary else Color.Transparent, tween(220), label = "segBg")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .pressClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Color.White else TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SeveritySummary(
    state: RiskAreasUiState,
    selected: Severity?,
    onSelect: (Severity) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(Severity.CRITICAL, Severity.HIGH, Severity.MODERATE, Severity.LOW).forEach { severity ->
            val isSelected = selected == severity
            val dimmed = selected != null && !isSelected
            val bg by animateColorAsState(
                if (isSelected) severity.container else BgSurface,
                tween(200),
                label = "sevBg"
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(
                        1.dp,
                        if (isSelected) severity.accent.copy(alpha = 0.6f) else BgBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .pressClickable { onSelect(severity) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(severity.accent.copy(alpha = if (dimmed) 0.4f else 1f))
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "${state.countFor(severity)}",
                    color = if (dimmed) TextMuted else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = severity.label,
                    color = if (dimmed) TextMuted else TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SortBar(sort: AreaSort, nearestEnabled: Boolean, onSelect: (AreaSort) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort by", tint = TextMuted, modifier = Modifier.size(16.dp))
        AreaSort.entries.forEach { option ->
            val isSelected = option == sort
            val bg by animateColorAsState(if (isSelected) BrandContainer else Color.Transparent, tween(200), label = "sortBg")
            Text(
                text = option.label + if (option == AreaSort.NEAREST && !nearestEnabled) " (GPS)" else "",
                color = if (isSelected) BrandPrimary else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .border(1.dp, if (isSelected) BrandPrimary.copy(alpha = 0.4f) else BgBorder, RoundedCornerShape(10.dp))
                    .pressClickable { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// RANKED LIST
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun RankedList(
    areas: List<RiskArea>,
    rankOf: (RiskArea) -> Int,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    userLocation: UserLocation?,
    locationStatus: LocationStatus,
    placeLabel: String?,
    userAnalysis: AnalysisState?,
    onMyLocationClick: () -> Unit,
    topPadding: androidx.compose.ui.unit.Dp,
    onAreaClick: (RiskArea) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(top = topPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimaryLight, strokeWidth = 2.dp)
            }

            error != null -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = topPadding / 2)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.CloudOff, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(10.dp))
                Text("DATA UNAVAILABLE", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(error, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                PanelButton("Retry", primary = true, onClick = onRetry, modifier = Modifier.width(140.dp))
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = topPadding,
                    bottom = FloatingNavClearance + 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "my-location") {
                    MyAreaRow(
                        userLocation = userLocation,
                        status = locationStatus,
                        placeLabel = placeLabel,
                        analysis = userAnalysis,
                        onClick = onMyLocationClick
                    )
                }
                if (areas.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Terrain, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No areas match these filters", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Try another severity or state filter.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
                itemsIndexed(areas, key = { _, it -> it.id }) { index, area ->
                    RankedAreaRow(
                        rank = index + 1,
                        riskRank = rankOf(area),
                        area = area,
                        distance = userLocation?.let { formatDistance(distanceKm(it, area.latitude, area.longitude)) },
                        onClick = { onAreaClick(area) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/** The user's own area, always at the top of the list. */
@Composable
private fun MyAreaRow(
    userLocation: UserLocation?,
    status: LocationStatus,
    placeLabel: String?,
    analysis: AnalysisState?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandContainer)
            .border(1.dp, BrandPrimary.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .pressClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MyLocation, null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Your area" + (placeLabel?.let { " · $it" } ?: ""),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    userLocation == null -> when (status) {
                        LocationStatus.DENIED -> "Location off · tap to enable"
                        LocationStatus.UNAVAILABLE -> "No GPS fix · tap to retry"
                        else -> "Finding your location…"
                    }
                    analysis is AnalysisState.Ready -> analysis.analysis.headline()
                    else -> "Fetching satellite, rainfall and terrain data…"
                },
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        when {
            analysis is AnalysisState.Ready -> when (val risk = analysis.analysis.risk) {
                is DataResult.Available -> SeverityPill(risk.value.severity)
                is DataResult.Unavailable -> Text("Risk: DATA\nUNAVAILABLE", color = TextMuted, fontSize = 9.sp, textAlign = TextAlign.End)
            }
            userLocation != null -> CircularProgressIndicator(color = BrandPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RankedAreaRow(
    rank: Int,
    riskRank: Int,
    area: RiskArea,
    distance: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgSurface)
            .border(1.dp, BgBorder, RoundedCornerShape(18.dp))
            .pressClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(area.severity.container)
                .border(1.dp, area.severity.accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                color = area.severity.accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = area.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (area.severity >= Severity.HIGH) {
                    SeverityDot(area.severity, size = 7.dp)
                }
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeverityPill(area.severity)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = area.summaryLine(distance),
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            RiskScoreBar(score = area.score, severity = area.severity)
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text("${area.score}", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (riskRank != rank) "#$riskRank by risk" else "score", color = TextMuted, fontSize = 10.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
}

// ═════════════════════════════════════════════════════════════════════
// MAP CAROUSEL, LAYERS & LEGEND
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun CarouselCard(
    rank: Int,
    area: RiskArea,
    selected: Boolean,
    userLocation: UserLocation?,
    onClick: () -> Unit,
    onDetails: () -> Unit,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border by animateColorAsState(
        if (selected) area.severity.accent else Color.Transparent,
        tween(220),
        label = "carouselBorder"
    )
    GlassSurface(
        modifier = modifier
            .width(292.dp)
            .border(1.5.dp, border, RoundedCornerShape(22.dp))
            .pressClickable(pressedScale = 0.98f, onClick = onClick),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#$rank", color = area.severity.accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(8.dp))
                SeverityPill(area.severity)
                Spacer(Modifier.weight(1f))
                userLocation?.let {
                    Text(
                        formatDistance(distanceKm(it, area.latitude, area.longitude)),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                area.name,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (area.activeAlertCount > 0) Icons.Filled.NotificationsActive else Icons.Filled.Warning,
                    null,
                    tint = if (area.activeAlertCount > 0) area.severity.accent else TextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${area.state} · " + area.summaryLine(),
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            RiskScoreBar(score = area.score, severity = area.severity)

            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "carouselActions"
            ) { isSelected ->
                if (isSelected) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PanelButton("Details", primary = true, onClick = onDetails, modifier = Modifier.weight(1f))
                        PanelButton("Analysis map", primary = false, onClick = onAnalyze, modifier = Modifier.weight(1f))
                    }
                } else {
                    Spacer(Modifier.height(0.dp))
                }
            }
        }
    }
}

@Composable
private fun LayersPanel(
    mapStyle: RiskMapStyle,
    onStyle: (RiskMapStyle) -> Unit,
    threeD: Boolean,
    onThreeD: (Boolean) -> Unit,
    showZones: Boolean,
    onShowZones: (Boolean) -> Unit
) {
    GlassSurface(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp).width(210.dp)) {
            Text("BASE MAP", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RiskMapStyle.entries.forEach { style ->
                    val selected = style == mapStyle
                    Text(
                        text = style.label,
                        color = if (selected) Color.White else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) BrandPrimary else BgElevated)
                            .pressClickable { onStyle(style) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("OVERLAYS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            LayerToggleRow(Icons.Filled.Terrain, "Risk zones", showZones) { onShowZones(!showZones) }
            LayerToggleRow(Icons.Filled.ViewInAr, "3D pillars & tilt", threeD) { onThreeD(!threeD) }
        }
    }
}

@Composable
fun LayerToggleRow(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .pressClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (on) BrandPrimary else TextMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(50))
                .background(if (on) BrandPrimary else BgBorder),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .padding(2.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

/** Score bands used by the LandGuard risk engine. */
@Composable
fun RiskLegend(modifier: Modifier = Modifier) {
    GlassSurface(modifier = modifier, shape = RoundedCornerShape(14.dp), elevation = 6.dp) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("RISK SCORE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            listOf(
                Severity.CRITICAL to "Critical ≥ 75",
                Severity.HIGH to "High 50–74",
                Severity.MODERATE to "Moderate 25–49",
                Severity.LOW to "Low < 25"
            ).forEach { (severity, text) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(severity.accent)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text, color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// REGION FILTER & PROVENANCE
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun StateFilter(
    states: List<Pair<String, Int>>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        states.forEach { (name, count) ->
            val isSelected = selected == name
            val bg by animateColorAsState(if (isSelected) BrandPrimary else BgSurface, tween(200), label = "stateBg")
            Text(
                text = "$name  $count",
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .border(1.dp, if (isSelected) BrandPrimary else BgBorder, RoundedCornerShape(10.dp))
                    .pressClickable { onSelect(name) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ProvenanceLine(state: RiskAreasUiState) {
    val text = buildString {
        if (state.catalogEventCount > 0) {
            append("${state.catalogEventCount} recorded landslides")
            if (state.catalogFirstYear != null && state.catalogLastYear != null) {
                append(" (${state.catalogFirstYear}–${state.catalogLastYear})")
            }
            append(", NASA Global Landslide Catalog · ")
        }
        append(state.dataStatus.detail)
    }
    Text(
        text = text,
        color = TextMuted,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 6.dp)
    )
}
