package com.example.landguard.ui.risk

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.components.GlassSurface
import com.example.landguard.ui.components.LandGuardRiskMap
import com.example.landguard.ui.components.MapControlButton
import com.example.landguard.ui.components.MapFitAll
import com.example.landguard.ui.components.MapFocus
import com.example.landguard.ui.components.SeverityDot
import com.example.landguard.ui.components.SeverityPill
import com.example.landguard.ui.components.accent
import com.example.landguard.ui.components.container
import com.example.landguard.ui.components.label
import com.example.landguard.ui.components.pressClickable
import com.example.landguard.ui.home.FloatingNavClearance
import com.example.landguard.ui.home.PanelButton
import com.example.landguard.ui.home.RiskScoreBar
import com.example.landguard.ui.monitor.formatTime
import com.example.landguard.ui.monitor.summaryLine
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.ui.text.style.TextAlign
import com.example.landguard.ui.location.LocalRequestUserLocation
import com.example.landguard.ui.location.LocalUserLocation
import com.example.landguard.ui.location.UserLocation
import com.example.landguard.ui.location.distanceKm
import com.example.landguard.ui.location.formatDistance
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.BrandPrimaryLight
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary

private enum class RiskView { MAP, LIST }

@Composable
fun RiskAreasScreen(
    onOpenFullMap: () -> Unit,
    onOpenAlerts: () -> Unit,
    viewModel: RiskAreasViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val userLocation = LocalUserLocation.current.location
    val requestLocation = LocalRequestUserLocation.current

    var view by remember { mutableStateOf(RiskView.LIST) }
    var filter by remember { mutableStateOf<Severity?>(null) }
    var stateFilter by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var focus by remember { mutableStateOf<MapFocus?>(null) }
    var fitAll by remember { mutableStateOf<MapFitAll?>(null) }
    var token by remember { mutableIntStateOf(0) }

    val visibleAreas = remember(state.areas, filter, stateFilter) {
        state.areas
            .filter { filter == null || it.severity == filter }
            .filter { stateFilter == null || it.state == stateFilter }
    }
    val carouselState = rememberLazyListState()

    fun focusOn(area: RiskArea) {
        selectedId = area.id
        focus = MapFocus(area.latitude, area.longitude, 11.5, ++token)
    }

    // Frame the filtered set whenever the filter changes.
    LaunchedEffect(filter, stateFilter, state.areas.size) {
        if (state.areas.isNotEmpty()) fitAll = MapFitAll(++token)
    }

    // Keep the carousel in sync with map selection.
    LaunchedEffect(selectedId, visibleAreas) {
        val index = visibleAreas.indexOfFirst { it.id == selectedId }
        if (index >= 0) carouselState.animateScrollToItem(index)
    }

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
            onAreaClick = { focusOn(it) },
            onMapBackgroundClick = { selectedId = null },
            focus = focus,
            fitAll = fitAll,
            topInset = 262.dp,
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
                isLoading = state.isLoading,
                error = state.catalogError,
                onRetry = { viewModel.refresh(force = true) },
                userLocation = userLocation,
                onAreaClick = { area ->
                    view = RiskView.MAP
                    focusOn(area)
                }
            )
        }

        // ── Header ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to BgDeep,
                        0.75f to BgDeep.copy(alpha = if (view == RiskView.LIST) 1f else 0.85f),
                        1f to Color.Transparent
                    )
                )
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp)
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
                        text = "Northeast India · ranked by landslide record, live rainfall and slope",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                ViewToggle(view = view, onChange = { view = it })
            }

            Spacer(Modifier.height(14.dp))

            SeveritySummary(state = state, selected = filter, onSelect = {
                filter = if (filter == it) null else it
                selectedId = null
            })

            if (state.statesCovered.isNotEmpty()) {
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

            ProvenanceLine(state)
        }

        // ── Map-mode controls & carousel ───────────────────────────────────
        AnimatedVisibility(
            visible = view == RiskView.MAP,
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
                MapControlButton(
                    icon = Icons.Filled.MyLocation,
                    contentDescription = "My location",
                    tint = if (userLocation != null) BrandPrimaryLight else TextPrimary,
                    onClick = {
                        val loc = userLocation
                        if (loc != null) {
                            selectedId = null
                            focus = MapFocus(loc.latitude, loc.longitude, 10.5, ++token)
                        } else {
                            requestLocation()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 16.dp, bottom = 12.dp)
                )

                LazyRow(
                    state = carouselState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(visibleAreas, key = { _, it -> it.id }) { index, area ->
                        CarouselCard(
                            rank = state.areas.indexOf(area) + 1,
                            area = area,
                            selected = area.id == selectedId,
                            userLocation = userLocation,
                            onClick = { focusOn(area) },
                            onAnalyze = onOpenFullMap,
                            onAlerts = onOpenAlerts,
                            modifier = Modifier.animateItem()
                        )
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

// ═════════════════════════════════════════════════════════════════════
// RANKED LIST
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun RankedList(
    areas: List<RiskArea>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    userLocation: UserLocation?,
    onAreaClick: (RiskArea) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimaryLight, strokeWidth = 2.dp)
            }

            error != null -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.CloudOff, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(10.dp))
                Text("Data unavailable", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(error, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                PanelButton("Retry", primary = true, onClick = onRetry, modifier = Modifier.width(140.dp))
            }

            areas.isEmpty() -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Terrain, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(10.dp))
                Text("No areas at this level", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Try another severity filter.", color = TextSecondary, fontSize = 12.sp)
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 262.dp,
                    bottom = FloatingNavClearance + 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(areas, key = { _, it -> it.id }) { index, area ->
                    RankedAreaRow(
                        rank = index + 1,
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

@Composable
private fun RankedAreaRow(
    rank: Int,
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
            Text("score", color = TextMuted, fontSize = 10.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
}

// ═════════════════════════════════════════════════════════════════════
// MAP CAROUSEL
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun CarouselCard(
    rank: Int,
    area: RiskArea,
    selected: Boolean,
    userLocation: UserLocation?,
    onClick: () -> Unit,
    onAnalyze: () -> Unit,
    onAlerts: () -> Unit,
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
                        PanelButton("Analyze", primary = true, onClick = onAnalyze, modifier = Modifier.weight(1f))
                        PanelButton("Alerts", primary = false, onClick = onAlerts, modifier = Modifier.weight(1f))
                    }
                } else {
                    Spacer(Modifier.height(0.dp))
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
    if (state.catalogEventCount == 0) return
    val text = buildString {
        append("${state.catalogEventCount} recorded landslides")
        if (state.catalogFirstYear != null && state.catalogLastYear != null) {
            append(" (${state.catalogFirstYear}–${state.catalogLastYear})")
        }
        append(", NASA Global Landslide Catalog")
        if (state.catalogFromCache) state.catalogFetchedAtMillis?.let { append(" · cached ${formatTime(it)}") }
        append(" · rainfall: ")
        append(state.conditionsUpdatedAtMillis?.let { "Open-Meteo, updated ${formatTime(it)}" } ?: "data unavailable")
    }
    Text(
        text = text,
        color = TextMuted,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}
