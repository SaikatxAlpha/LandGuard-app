// app/src/main/java/com/example/landguard/ui/map/MapScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.landguard.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.Alert
import com.example.landguard.domain.model.GroundDeformationPoint
import com.example.landguard.domain.model.SatelliteLayer
import com.example.landguard.domain.model.SatelliteSource
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.components.GlassSurface
import com.example.landguard.ui.components.MapControlButton
import com.example.landguard.ui.components.severityHex
import com.example.landguard.ui.home.FloatingNavClearance
import com.example.landguard.ui.location.LocalRequestUserLocation
import com.example.landguard.ui.location.LocalUserLocation
import com.example.landguard.ui.location.LocationStatus
import com.example.landguard.ui.location.UserLocation
import com.example.landguard.ui.monitor.LiveReadings
import com.example.landguard.ui.monitor.RiskIndexSummary
import com.example.landguard.ui.navigation.MapTarget
import com.example.landguard.ui.risk.AnalysisState
import com.example.landguard.ui.risk.DataStatusChip
import com.example.landguard.ui.risk.DataStatusDetail
import com.example.landguard.ui.risk.DataStatusKind
import com.example.landguard.ui.risk.LayerToggleRow
import com.example.landguard.ui.risk.PlaceDetailContent
import com.example.landguard.ui.risk.RiskAreasViewModel
import com.example.landguard.ui.risk.nearestArea
import com.example.landguard.ui.search.PlaceResult
import com.example.landguard.ui.search.PlaceSearchBar
import com.example.landguard.ui.theme.Alos4Brand
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.BrandPrimaryLight
import com.example.landguard.ui.theme.CyanContainer
import com.example.landguard.ui.theme.CyanDim
import com.example.landguard.ui.theme.CyanPrimary
import com.example.landguard.ui.theme.EmeraldPrimary
import com.example.landguard.ui.theme.FusionBrand
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.RiskCriticalContainer
import com.example.landguard.ui.theme.RiskHigh
import com.example.landguard.ui.theme.RiskHighContainer
import com.example.landguard.ui.theme.RiskLow
import com.example.landguard.ui.theme.RiskLowContainer
import com.example.landguard.ui.theme.RiskModerate
import com.example.landguard.ui.theme.RiskModerateContainer
import com.example.landguard.ui.theme.SentinelBrand
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import java.util.Locale
import kotlin.math.hypot

// ─── Map Style Options ────────────────────────────────────────────────────────
//
//  BRIGHT    — OpenFreeMap "bright" vector tiles. Full street detail,
//              building footprints, landuse polygons, terrain shading.
//              Clean professional look — the best default for a GIS platform.
//
//  TOPO      — OpenTopoMap raster tiles built on OSM + SRTM elevation data.
//              Topographic contour lines, hillshade, mountain/ridge labels.
//              The most "scientific" / intellectual style in this set.
//
//  SATELLITE — ESRI World Imagery (public, no API key required).
//              High-resolution aerial and satellite photography.
//              Perfect for viewing actual land cover alongside risk overlays.
//
//  CLEAN     — OpenFreeMap "positron" vector tiles.
//              Minimalist greyscale base — makes coloured risk overlays
//              pop with maximum visual contrast.

enum class MapStyleType(
    val label:    String,
    val icon:     String,      // emoji shorthand used in the selector chip
    val subtitle: String
) {
    BRIGHT   ("BRIGHT",    "🗺",  "OSM + Buildings"),
    TOPO     ("TOPO",      "⛰",  "Contours + Relief"),
    SATELLITE("SATELLITE", "🛰",  "ESRI Imagery"),
    CLEAN    ("CLEAN",     "◻",  "Minimal Overlay"),
}

// ─── Raster Tile Style JSON Definitions ──────────────────────────────────────
//
//  MapLibre accepts either a hosted style URL (for vector tiles) or
//  an inline JSON style document (for raster tile sources). The JSON
//  approach unlocks any XYZ tile service without needing a hosted style.

private val TOPO_STYLE_JSON = """
{
  "version": 8,
  "name": "OpenTopoMap",
  "sources": {
    "topo": {
      "type": "raster",
      "tiles": [
        "https://tile.opentopomap.org/{z}/{x}/{y}.png"
      ],
      "tileSize": 256,
      "minzoom": 0,
      "maxzoom": 17,
      "attribution": "© OpenTopoMap (CC-BY-SA) | © OpenStreetMap contributors"
    }
  },
  "layers": [
    {
      "id": "topo-background",
      "type": "background",
      "paint": { "background-color": "#f4f1eb" }
    },
    {
      "id": "topo-layer",
      "type": "raster",
      "source": "topo",
      "paint": {
        "raster-opacity": 1.0,
        "raster-brightness-min": 0.05,
        "raster-saturation": 0.1
      }
    }
  ]
}
""".trimIndent()

private val SATELLITE_STYLE_JSON = """
{
  "version": 8,
  "name": "ESRI World Imagery",
  "sources": {
    "satellite": {
      "type": "raster",
      "tiles": [
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
      ],
      "tileSize": 256,
      "minzoom": 0,
      "maxzoom": 19,
      "attribution": "© ESRI, Maxar, GeoEye, Earthstar Geographics, CNES/Airbus DS"
    }
  },
  "layers": [
    {
      "id": "satellite-background",
      "type": "background",
      "paint": { "background-color": "#0a0f1a" }
    },
    {
      "id": "satellite-layer",
      "type": "raster",
      "source": "satellite",
      "paint": {
        "raster-opacity": 1.0,
        "raster-contrast": 0.08,
        "raster-saturation": 0.15
      }
    }
  ]
}
""".trimIndent()

// ─── Style Source Sealed Class ────────────────────────────────────────────────

private sealed class MapStyleSource {
    data class FromUrl(val url: String)   : MapStyleSource()
    data class FromJson(val json: String) : MapStyleSource()
}

private fun resolveStyleSource(type: MapStyleType): MapStyleSource = when (type) {
    MapStyleType.BRIGHT    -> MapStyleSource.FromUrl("https://tiles.openfreemap.org/styles/bright")
    MapStyleType.CLEAN     -> MapStyleSource.FromUrl("https://tiles.openfreemap.org/styles/positron")
    MapStyleType.TOPO      -> MapStyleSource.FromJson(TOPO_STYLE_JSON)
    MapStyleType.SATELLITE -> MapStyleSource.FromJson(SATELLITE_STYLE_JSON)
}

private fun styleBuilder(type: MapStyleType): Style.Builder = when (val source = resolveStyleSource(type)) {
    is MapStyleSource.FromUrl  -> Style.Builder().fromUri(source.url)
    is MapStyleSource.FromJson -> Style.Builder().fromJson(source.json)
}

// Whole-India view until the GPS fix / monitored areas are known.
private const val INDIA_LAT = 22.5
private const val INDIA_LNG = 82.0
private const val INDIA_ZOOM = 3.8

private const val LAYER_AURA = "aura-layer"
private const val LAYER_CORE = "core-layer"
private const val LAYER_CORE_STROKE = "core-stroke-layer"
private const val LAYER_SELECTED = "selected-ring-layer"
private const val LAYER_SELECTED_INNER = "selected-ring-inner-layer"
private const val LAYER_ALERTS = "alert-layer"
private const val LAYER_USER_HALO = "user-halo-layer"
private const val LAYER_USER = "user-layer"
private const val LAYER_PLACE = "place-layer"

/** A one-shot camera move; [token] distinguishes repeated requests for the same place. */
private data class CameraRequest(val lat: Double, val lng: Double, val zoom: Double?, val token: Int)

// ─── Root Screen ──────────────────────────────────────────────────────────────

/**
 * Full analysis map (MapLibre). Monitored areas are drawn from the real
 * NASA-catalog clusters with the LandGuard risk index; tapping any area opens
 * its details immediately — the tap handler reads the latest data through
 * [rememberUpdatedState], so it works on the very first map load.
 */
@Composable
fun MapScreen(
    onOpenZone: (String) -> Unit = {},
    onOpenAlert: (String) -> Unit = {},
    target: MapTarget? = null,
    onTargetConsumed: () -> Unit = {},
    openSearch: Boolean = false,
    onSearchOpened: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
    riskViewModel: RiskAreasViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val riskState by riskViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationState = LocalUserLocation.current
    val userLocation = locationState.location
    val requestLocation = LocalRequestUserLocation.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    var selectedMapType by rememberSaveable { mutableStateOf(MapStyleType.BRIGHT) }
    var showZones by rememberSaveable { mutableStateOf(true) }
    var showAlerts by rememberSaveable { mutableStateOf(true) }
    var showUser by rememberSaveable { mutableStateOf(true) }
    var layersOpen by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var baseMapFailed by remember { mutableStateOf(false) }

    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var styleGeneration by remember { mutableIntStateOf(0) }
    var cameraPlaced by remember { mutableStateOf(false) }
    var cameraRequest by remember { mutableStateOf<CameraRequest?>(null) }
    var cameraToken by remember { mutableIntStateOf(0) }

    fun moveCamera(lat: Double, lng: Double, zoom: Double? = null) {
        cameraPlaced = true
        cameraRequest = CameraRequest(lat, lng, zoom, ++cameraToken)
    }

    fun selectPoint(point: GroundDeformationPoint) {
        searchExpanded = false
        layersOpen = false
        viewModel.selectDeformationPoint(point)
        moveCamera(point.latitude, point.longitude, 11.5)
    }

    fun showPlace(place: PlaceResult) {
        searchExpanded = false
        layersOpen = false
        viewModel.showPlace(place)
        moveCamera(place.latitude, place.longitude, 12.0)
    }

    fun showMyLocation() {
        val loc = userLocation
        if (loc == null) {
            requestLocation()
            return
        }
        showPlace(PlaceResult(locationState.placeLabel ?: "Your location", "Device location", loc.latitude, loc.longitude))
    }

    // Latest values for the (once-registered) MapLibre tap listener.
    val currentPoints by rememberUpdatedState(state.deformationPoints)
    val currentUser by rememberUpdatedState(userLocation)
    val currentShowZones by rememberUpdatedState(showZones)
    val currentShowAlerts by rememberUpdatedState(showAlerts)
    val currentOnPointTap by rememberUpdatedState<(GroundDeformationPoint) -> Unit>({ selectPoint(it) })
    val currentOnUserTap by rememberUpdatedState<() -> Unit>({ showMyLocation() })
    val currentOnAlertTap by rememberUpdatedState(onOpenAlert)
    val currentOnBackgroundTap by rememberUpdatedState<() -> Unit>({
        layersOpen = false
        viewModel.selectDeformationPoint(null)
        viewModel.showPlace(null)
    })
    val tapSlopPx = with(density) { 32.dp.toPx() }
    val userSlopPx = with(density) { 20.dp.toPx() }

    // ── MapLibre Map View (created once, reused across recompositions) ────────
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { maplibreMap ->
                maplibreMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(INDIA_LAT, INDIA_LNG))
                    .zoom(INDIA_ZOOM)
                    .build()
                maplibreMap.uiSettings.isCompassEnabled = false
                maplibreMap.addOnMapClickListener { latLng ->
                    val screen = maplibreMap.projection.toScreenLocation(latLng)

                    if (currentShowAlerts) {
                        maplibreMap.queryRenderedFeatures(screen, LAYER_ALERTS)
                            .firstNotNullOfOrNull { it.getStringProperty("alertId") }
                            ?.let { currentOnAlertTap(it); return@addOnMapClickListener true }
                    }

                    val user = currentUser
                    if (user != null) {
                        val p = maplibreMap.projection.toScreenLocation(LatLng(user.latitude, user.longitude))
                        if (hypot((p.x - screen.x).toDouble(), (p.y - screen.y).toDouble()) <= userSlopPx) {
                            currentOnUserTap()
                            return@addOnMapClickListener true
                        }
                    }

                    if (currentShowZones) {
                        val points = currentPoints
                        val hitId = maplibreMap.queryRenderedFeatures(screen, LAYER_CORE)
                            .firstNotNullOfOrNull { it.getStringProperty("id") }
                            ?: maplibreMap.queryRenderedFeatures(screen, LAYER_AURA)
                                .firstNotNullOfOrNull { it.getStringProperty("id") }
                        val tapped = hitId?.let { id -> points.firstOrNull { it.id == id } }
                            ?: points
                                .map { point ->
                                    val p = maplibreMap.projection.toScreenLocation(LatLng(point.latitude, point.longitude))
                                    point to hypot((p.x - screen.x).toDouble(), (p.y - screen.y).toDouble())
                                }
                                .filter { it.second <= tapSlopPx }
                                .minByOrNull { it.second }
                                ?.first
                        if (tapped != null) {
                            currentOnPointTap(tapped)
                            return@addOnMapClickListener true
                        }
                    }
                    currentOnBackgroundTap()
                    true
                }
            }
            var fallbackAttempted = false
            addOnDidFailLoadingMapListener {
                baseMapFailed = true
                if (fallbackAttempted) return@addOnDidFailLoadingMapListener
                fallbackAttempted = true
                getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/positron")) { style ->
                        loadedStyle = style
                        styleGeneration++
                    }
                }
            }
            addOnDidFinishLoadingStyleListener { baseMapFailed = false }
        }
    }

    // ── Lifecycle wiring (the map is disposed with this screen) ──────────────
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    // ── Base map style ───────────────────────────────────────────────────────
    LaunchedEffect(selectedMapType) {
        mapView.getMapAsync { map ->
            map.setStyle(styleBuilder(selectedMapType)) { style ->
                loadedStyle = style
                styleGeneration++
            }
        }
    }

    // ── Overlays: re-applied whenever data, selection, layers or the style change ─
    LaunchedEffect(
        styleGeneration, state.deformationPoints, state.opacity, state.selectedSource,
        state.selectedPoint?.id, state.place, state.alerts, userLocation, showZones, showAlerts, showUser
    ) {
        val style = loadedStyle ?: return@LaunchedEffect
        if (!style.isFullyLoaded) return@LaunchedEffect
        runCatching {
            setupMapLayers(style, state.deformationPoints, state.selectedSource, state.opacity, state.selectedPoint)
            setupOverlayLayers(style, userLocation, state.place, state.alerts)
            setVisibility(style, listOf(LAYER_AURA, LAYER_CORE, LAYER_CORE_STROKE, LAYER_SELECTED, LAYER_SELECTED_INNER), showZones)
            setVisibility(style, listOf(LAYER_ALERTS), showAlerts)
            setVisibility(style, listOf(LAYER_USER_HALO, LAYER_USER), showUser)
        }
    }

    // ── Camera ───────────────────────────────────────────────────────────────
    LaunchedEffect(cameraRequest) {
        val request = cameraRequest ?: return@LaunchedEffect
        mapView.getMapAsync { map ->
            val zoom = request.zoom?.let { maxOf(it, map.cameraPosition.zoom) } ?: map.cameraPosition.zoom
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(request.lat, request.lng), zoom), 1200)
        }
    }

    // First placement: the user's GPS fix; without one, frame every monitored area.
    LaunchedEffect(userLocation, state.deformationPoints.isNotEmpty(), locationState.status) {
        if (cameraPlaced || target != null) return@LaunchedEffect
        val loc = userLocation
        when {
            loc != null -> moveCamera(loc.latitude, loc.longitude, 9.5)
            state.deformationPoints.isNotEmpty() &&
                    (locationState.status == LocationStatus.DENIED || locationState.status == LocationStatus.UNAVAILABLE) -> {
                cameraPlaced = true
                mapView.getMapAsync { map -> fitPoints(map, state.deformationPoints, null, density.run { 140.dp.roundToPx() }) }
            }
        }
    }

    // Cross-screen request (Home / Risk Areas / Alerts → this map).
    LaunchedEffect(target, state.isLoading, state.deformationPoints.isNotEmpty()) {
        val t = target ?: return@LaunchedEffect
        val catalogFailed = state.dataStatus.kind == DataStatusKind.UNAVAILABLE
        // Wait for the monitored areas (unless they cannot be loaded at all).
        if (state.isLoading || (state.deformationPoints.isEmpty() && !catalogFailed)) return@LaunchedEffect
        val point = t.areaId?.let { viewModel.selectPointById(it) }
        when {
            point != null -> moveCamera(point.latitude, point.longitude, 11.5)
            t.hasCoordinates -> showPlace(PlaceResult(t.label ?: "Selected location", "", t.latitude!!, t.longitude!!))
        }
        onTargetConsumed()
    }

    LaunchedEffect(openSearch) {
        if (openSearch) {
            searchExpanded = true
            onSearchOpened()
        }
    }

    BackHandler(enabled = searchExpanded) { searchExpanded = false }
    BackHandler(enabled = !searchExpanded && layersOpen) { layersOpen = false }
    BackHandler(enabled = !searchExpanded && !layersOpen && (state.selectedPoint != null || state.place != null)) {
        viewModel.selectDeformationPoint(null)
        viewModel.showPlace(null)
    }

    val detailOpen = state.selectedPoint != null || state.place != null

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {

        // ── Real MapLibre map (always attached; loading / offline shown as overlays) ─
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // ── Top: search, data status, layers ──────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            PlaceSearchBar(
                areas = riskState.areas,
                expanded = searchExpanded,
                onExpandedChange = {
                    searchExpanded = it
                    if (it) layersOpen = false
                },
                onAreaSelected = { area ->
                    viewModel.selectPointById(area.id)?.let { moveCamera(it.latitude, it.longitude, 11.5) }
                },
                onPlaceSelected = { showPlace(it) },
                placeholder = "Search a place or area on the map"
            )
            if (!searchExpanded) {
                Spacer(Modifier.height(8.dp))
                val status = state.dataStatus
                DataStatusChip(status, onRetry = { viewModel.refresh() })
                if (status.kind == DataStatusKind.UNAVAILABLE || status.kind == DataStatusKind.OFFLINE || baseMapFailed) {
                    GlassSurface(
                        modifier = Modifier.padding(top = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = 4.dp
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            if (status.kind != DataStatusKind.UPDATED && status.kind != DataStatusKind.LOADING) {
                                DataStatusDetail(status)
                            }
                            if (baseMapFailed) {
                                Text(
                                    "Base map tiles could not be loaded (offline?). Risk overlays still show real data.",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = layersOpen,
                    enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { -it / 6 },
                    exit = fadeOut(tween(120))
                ) {
                    MapControlPanel(
                        selectedSource    = state.selectedSource,
                        selectedLayer     = state.selectedLayer,
                        selectedMapType   = selectedMapType,
                        opacity           = state.opacity,
                        showZones         = showZones,
                        showAlerts        = showAlerts,
                        showUser          = showUser,
                        alertCount        = state.alerts.size,
                        onSourceSelected  = { viewModel.selectSource(it) },
                        onLayerSelected   = { viewModel.selectLayer(it) },
                        onMapTypeSelected = { selectedMapType = it },
                        onOpacityChange   = { viewModel.setOpacity(it) },
                        onShowZones       = { showZones = it },
                        onShowAlerts      = { showAlerts = it },
                        onShowUser        = { showUser = it },
                        onClose           = { layersOpen = false },
                        modifier          = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // ── Right-hand map controls ───────────────────────────────────────────
        if (!searchExpanded) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MapControlButton(
                    icon = Icons.Filled.Layers,
                    contentDescription = "Map layers",
                    active = layersOpen,
                    onClick = { layersOpen = !layersOpen }
                )
                MapControlButton(
                    icon = Icons.Filled.ZoomOutMap,
                    contentDescription = "Recenter on monitored areas",
                    onClick = {
                        cameraPlaced = true
                        mapView.getMapAsync { map ->
                            fitPoints(map, state.deformationPoints, userLocation, density.run { 140.dp.roundToPx() })
                        }
                    }
                )
                MapControlButton(
                    icon = Icons.Filled.MyLocation,
                    contentDescription = "My location",
                    tint = if (userLocation != null) BrandPrimaryLight else TextPrimary,
                    onClick = {
                        val loc = userLocation
                        if (loc != null) moveCamera(loc.latitude, loc.longitude, 11.0) else requestLocation()
                    }
                )
            }
        }

        // ── Map legend (bottom-left) ──────────────────────────────────────────
        AnimatedVisibility(
            visible = !detailOpen && !searchExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 14.dp, bottom = FloatingNavClearance + 8.dp)
        ) {
            MapLegend(selectedMapType = selectedMapType, alertsShown = showAlerts && state.alerts.isNotEmpty())
        }

        // ── Details card (slides up from bottom when an area / place is selected) ─
        AnimatedVisibility(
            visible  = detailOpen && !searchExpanded,
            enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = FloatingNavClearance)
        ) {
            val maxHeight = screenHeight * 0.58f
            val point = state.selectedPoint
            val place = state.place
            when {
                point != null -> TelemetryCard(
                    point     = point,
                    analysis  = state.selectedAnalysis,
                    maxHeight = maxHeight,
                    onClose   = { viewModel.selectDeformationPoint(null) },
                    onRefresh = { viewModel.refreshSelected() },
                    onAnalyze = { onOpenZone(point.id) }
                )
                place != null -> Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .shadow(8.dp, RoundedCornerShape(22.dp)),
                    colors = CardDefaults.cardColors(containerColor = BgSurface),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    val isUser = userLocation != null && place.key == PlaceResult("", "", userLocation.latitude, userLocation.longitude).key
                    PlaceDetailContent(
                        title = place.title,
                        subtitle = place.subtitle,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        isUserLocation = isUser,
                        analysis = state.placeAnalysis,
                        nearest = nearestArea(riskState.areas, place.latitude, place.longitude),
                        onClose = { viewModel.showPlace(null) },
                        onRefresh = { viewModel.refreshSelected() },
                        onOpenArea = { area ->
                            viewModel.selectPointById(area.id)?.let { moveCamera(it.latitude, it.longitude, 11.5) }
                        },
                        modifier = Modifier
                            .heightIn(max = maxHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

// ─── Camera helpers ───────────────────────────────────────────────────────────

private fun fitPoints(map: MapLibreMap, points: List<GroundDeformationPoint>, user: UserLocation?, paddingPx: Int) {
    val all = points.map { LatLng(it.latitude, it.longitude) } + listOfNotNull(user?.let { LatLng(it.latitude, it.longitude) })
    when {
        all.isEmpty() -> return
        all.size == 1 -> map.animateCamera(CameraUpdateFactory.newLatLngZoom(all.first(), 10.0), 1200)
        else -> runCatching {
            val bounds = LatLngBounds.Builder().includes(all).build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx), 1200)
        }
    }
}

// ─── Map Layer Setup ──────────────────────────────────────────────────────────

private fun getRiskColorHex(severity: Severity, source: SatelliteSource): String {
    if (source == SatelliteSource.SENTINEL2_MSI) {
        return when (severity) {
            Severity.CRITICAL -> "#C04800"
            Severity.HIGH     -> "#A07000"
            Severity.MODERATE -> "#008A5C"
            Severity.LOW      -> "#33B380"
        }
    }
    return when (severity) {
        Severity.CRITICAL -> "#C0184A"
        Severity.HIGH     -> "#C04800"
        Severity.MODERATE -> "#A07000"
        Severity.LOW      -> "#008A5C"
    }
}

private fun createCirclePolygon(center: LatLng, radiusInMeters: Double): Polygon {
    val points      = mutableListOf<Point>()
    val earthRadius = 6378137.0
    val d           = radiusInMeters / earthRadius
    val lat1        = Math.toRadians(center.latitude)
    val lng1        = Math.toRadians(center.longitude)
    for (i in 0..360 step 10) {
        val tc   = Math.toRadians(i.toDouble())
        val lat2 = Math.asin(
            Math.sin(lat1) * Math.cos(d) +
                    Math.cos(lat1) * Math.sin(d) * Math.cos(tc)
        )
        val lng2 = lng1 + Math.atan2(
            Math.sin(tc) * Math.sin(d) * Math.cos(lat1),
            Math.cos(d) - Math.sin(lat1) * Math.sin(lat2)
        )
        points.add(Point.fromLngLat(Math.toDegrees(lng2), Math.toDegrees(lat2)))
    }
    points.add(points.first())
    return Polygon.fromLngLats(listOf(points))
}

private fun Style.geoJsonSource(id: String): GeoJsonSource =
    getSourceAs(id) ?: GeoJsonSource(id).also { addSource(it) }

private fun Style.addLayerOnce(layer: org.maplibre.android.style.layers.Layer) {
    if (getLayer(layer.id) == null) addLayer(layer)
}

private fun setVisibility(style: Style, layers: List<String>, visible: Boolean) {
    val value = PropertyFactory.visibility(if (visible) Property.VISIBLE else Property.NONE)
    layers.forEach { style.getLayer(it)?.setProperties(value) }
}

private fun setupMapLayers(
    style:    Style,
    points:   List<GroundDeformationPoint>,
    source:   SatelliteSource,
    opacity:  Float,
    selected: GroundDeformationPoint?
) {
    // Outer translucent aura ring (wide halo effect)
    val auraFeatures = points.map { pt ->
        Feature.fromGeometry(createCirclePolygon(LatLng(pt.latitude, pt.longitude), 2800.0)).apply {
            addStringProperty("id", pt.id)
            addStringProperty("color", getRiskColorHex(pt.riskSeverity, source))
            addNumberProperty("opacity", opacity * 0.18f)
        }
    }

    // Core filled circle (tight footprint)
    val coreFeatures = points.map { pt ->
        Feature.fromGeometry(createCirclePolygon(LatLng(pt.latitude, pt.longitude), 1200.0)).apply {
            addStringProperty("id", pt.id)
            addStringProperty("color", getRiskColorHex(pt.riskSeverity, source))
            addNumberProperty("opacity", opacity * 0.68f)
        }
    }

    style.geoJsonSource("aura-source").setGeoJson(FeatureCollection.fromFeatures(auraFeatures))
    style.addLayerOnce(
        FillLayer(LAYER_AURA, "aura-source").withProperties(
            PropertyFactory.fillColor(Expression.get("color")),
            PropertyFactory.fillOpacity(Expression.get("opacity"))
        )
    )

    style.geoJsonSource("core-source").setGeoJson(FeatureCollection.fromFeatures(coreFeatures))
    style.addLayerOnce(
        FillLayer(LAYER_CORE, "core-source").withProperties(
            PropertyFactory.fillColor(Expression.get("color")),
            PropertyFactory.fillOpacity(Expression.get("opacity"))
        )
    )
    style.addLayerOnce(
        LineLayer(LAYER_CORE_STROKE, "core-source").withProperties(
            PropertyFactory.lineColor("rgba(255, 255, 255, 0.9)"),
            PropertyFactory.lineWidth(2.5f)
        )
    )

    // Selected area: bold white ring + severity-coloured inner ring
    val selectedFeatures = listOfNotNull(selected?.let { pt ->
        Feature.fromGeometry(createCirclePolygon(LatLng(pt.latitude, pt.longitude), 1900.0)).apply {
            addStringProperty("color", severityHex(pt.riskSeverity))
        }
    })
    style.geoJsonSource("selected-source").setGeoJson(FeatureCollection.fromFeatures(selectedFeatures))
    style.addLayerOnce(
        LineLayer(LAYER_SELECTED, "selected-source").withProperties(
            PropertyFactory.lineColor("#FFFFFF"),
            PropertyFactory.lineWidth(7f),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    style.addLayerOnce(
        LineLayer(LAYER_SELECTED_INNER, "selected-source").withProperties(
            PropertyFactory.lineColor(Expression.get("color")),
            PropertyFactory.lineWidth(3.5f),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
}

/** Active alerts with coordinates, the user's GPS position and a searched place. */
private fun setupOverlayLayers(style: Style, user: UserLocation?, place: PlaceResult?, alerts: List<Alert>) {
    val alertFeatures = alerts.mapNotNull { alert ->
        val lat = alert.latitude ?: return@mapNotNull null
        val lng = alert.longitude ?: return@mapNotNull null
        Feature.fromGeometry(Point.fromLngLat(lng, lat)).apply {
            addStringProperty("alertId", alert.id)
            addStringProperty("color", severityHex(alert.severity))
        }
    }
    style.geoJsonSource("alert-source").setGeoJson(FeatureCollection.fromFeatures(alertFeatures))
    style.addLayerOnce(
        CircleLayer(LAYER_ALERTS, "alert-source").withProperties(
            PropertyFactory.circleColor(Expression.get("color")),
            PropertyFactory.circleRadius(9f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(3f)
        )
    )

    val userFeatures = listOfNotNull(user?.let { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) })
    style.geoJsonSource("user-source").setGeoJson(FeatureCollection.fromFeatures(userFeatures))
    style.addLayerOnce(
        CircleLayer(LAYER_USER_HALO, "user-source").withProperties(
            PropertyFactory.circleColor("#0A84FF"),
            PropertyFactory.circleRadius(18f),
            PropertyFactory.circleOpacity(0.18f)
        )
    )
    style.addLayerOnce(
        CircleLayer(LAYER_USER, "user-source").withProperties(
            PropertyFactory.circleColor("#0A84FF"),
            PropertyFactory.circleRadius(7.5f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2.5f)
        )
    )

    val placeFeatures = listOfNotNull(place?.let { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) })
    style.geoJsonSource("place-source").setGeoJson(FeatureCollection.fromFeatures(placeFeatures))
    style.addLayerOnce(
        CircleLayer(LAYER_PLACE, "place-source").withProperties(
            PropertyFactory.circleColor("#1C1C1E"),
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(3f)
        )
    )
}

// ─── Map Style Legend ─────────────────────────────────────────────────────────

@Composable
private fun MapLegend(selectedMapType: MapStyleType, alertsShown: Boolean) {
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(BgSurface.copy(alpha = 0.95f))
            .border(1.dp, BgBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = when (selectedMapType) {
                    MapStyleType.BRIGHT    -> "OSM Bright — OpenFreeMap"
                    MapStyleType.TOPO      -> "OpenTopoMap — SRTM Contours"
                    MapStyleType.SATELLITE -> "ESRI World Imagery"
                    MapStyleType.CLEAN     -> "Positron — OpenFreeMap"
                },
                color    = TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text("LANDGUARD RISK SCORE", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            LegendRow(color = RiskCritical,  label = "Critical  (≥ 75)")
            LegendRow(color = RiskHigh,      label = "High      (50–74)")
            LegendRow(color = RiskModerate,  label = "Moderate  (25–49)")
            LegendRow(color = RiskLow,       label = "Low       (< 25)")
            if (alertsShown) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = RiskCritical, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Active alert (tap)", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Map Control Panel (layers) ───────────────────────────────────────────────

@Composable
private fun MapControlPanel(
    selectedSource:    SatelliteSource,
    selectedLayer:     SatelliteLayer,
    selectedMapType:   MapStyleType,
    opacity:           Float,
    showZones:         Boolean,
    showAlerts:        Boolean,
    showUser:          Boolean,
    alertCount:        Int,
    onSourceSelected:  (SatelliteSource) -> Unit,
    onLayerSelected:   (SatelliteLayer) -> Unit,
    onMapTypeSelected: (MapStyleType) -> Unit,
    onOpacityChange:   (Float) -> Unit,
    onShowZones:       (Boolean) -> Unit,
    onShowAlerts:      (Boolean) -> Unit,
    onShowUser:        (Boolean) -> Unit,
    onClose:           () -> Unit,
    modifier:          Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(BgSurface)
            .border(1.dp, BgBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyanContainer)
                        .border(1.dp, CyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Radar, null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("LAND RISK MAP", color = CyanPrimary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                    Text("Layers & base map", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, "Close layers", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Overlays ──────────────────────────────────────────────────
            Text("OVERLAYS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            LayerToggleRow(Icons.Filled.Terrain, "Monitored risk areas", showZones) { onShowZones(!showZones) }
            LayerToggleRow(Icons.Filled.NotificationsActive, "Active alerts ($alertCount with location)", showAlerts) { onShowAlerts(!showAlerts) }
            LayerToggleRow(Icons.Filled.MyLocation, "My location", showUser) { onShowUser(!showUser) }

            Spacer(Modifier.height(12.dp))

            // ── Base Map Style Selector ───────────────────────────────
            Text("BASE MAP STYLE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                MapStyleType.entries.forEach { type ->
                    val isSelected = selectedMapType == type
                    val icon: ImageVector = when (type) {
                        MapStyleType.BRIGHT    -> Icons.Filled.Layers
                        MapStyleType.TOPO      -> Icons.Filled.Terrain
                        MapStyleType.SATELLITE -> Icons.Filled.Satellite
                        MapStyleType.CLEAN     -> Icons.Filled.Landscape
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CyanContainer else BgElevated)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CyanPrimary else BgBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onMapTypeSelected(type) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected) CyanPrimary else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                type.label,
                                color = if (isSelected) CyanPrimary else TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                type.subtitle,
                                color    = if (isSelected) CyanDim else TextMuted,
                                fontSize = 7.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Data Source Selector (overlay colour scheme) ──────────
            Text("OVERLAY COLOURS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SatelliteSource.entries.forEach { source ->
                    val isSelected = selectedSource == source
                    val accent = when (source) {
                        SatelliteSource.ALOS4_PALSAR3 -> Alos4Brand
                        SatelliteSource.SENTINEL2_MSI -> SentinelBrand
                        SatelliteSource.HYBRID_FUSION -> FusionBrand
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accent.copy(alpha = 0.12f) else BgElevated)
                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) accent else BgBorder, RoundedCornerShape(12.dp))
                            .clickable { onSourceSelected(source) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(source.displayName.split(" ")[0], color = if (isSelected) accent else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                when (source) {
                                    SatelliteSource.ALOS4_PALSAR3 -> "L-SAR"
                                    SatelliteSource.SENTINEL2_MSI -> "Optical"
                                    SatelliteSource.HYBRID_FUSION -> "Fusion"
                                },
                                color = if (isSelected) accent.copy(alpha = 0.75f) else TextMuted, fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Layer Chip Scroller ───────────────────────────────────
            Text("ACTIVE LAYER", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            val layers = when (selectedSource) {
                SatelliteSource.ALOS4_PALSAR3 -> listOf(SatelliteLayer.ALOS4_INSAR_DISPLACEMENT, SatelliteLayer.ALOS4_SAR_BACKSCATTER)
                SatelliteSource.SENTINEL2_MSI -> listOf(SatelliteLayer.SENTINEL2_NDVI, SatelliteLayer.SENTINEL2_MOISTURE, SatelliteLayer.SENTINEL2_TRUE_COLOR)
                SatelliteSource.HYBRID_FUSION -> listOf(SatelliteLayer.ALOS4_INSAR_DISPLACEMENT, SatelliteLayer.SENTINEL2_NDVI)
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                layers.forEach { layer ->
                    val isSelected = selectedLayer == layer
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CyanPrimary else BgElevated)
                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) CyanPrimary else BgBorder, RoundedCornerShape(12.dp))
                            .clickable { onLayerSelected(layer) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(layer.title, color = if (isSelected) Color.White else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (selectedLayer.name.startsWith("ALOS4"))
                    "ALOS-4 PALSAR-3: DATA UNAVAILABLE (no public data service). Area colours show the LandGuard risk index."
                else
                    "Sentinel-2 values are fetched per area when you tap it. Area colours show the LandGuard risk index.",
                color = TextMuted,
                fontSize = 9.sp,
                lineHeight = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            // ── Opacity Slider ────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("OVERLAY", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Spacer(Modifier.width(10.dp))
                Slider(
                    value         = opacity,
                    onValueChange = onOpacityChange,
                    valueRange    = 0.1f..1.0f,
                    modifier      = Modifier.weight(1f),
                    colors        = SliderDefaults.colors(
                        thumbColor         = CyanPrimary,
                        activeTrackColor   = CyanPrimary,
                        inactiveTrackColor = BgBorder
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text("${(opacity * 100).toInt()}%", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Bottom Telemetry Card ────────────────────────────────────────────────────

private const val UNAVAILABLE = "Unavailable"

private fun Double.orUnavailable(format: String): String = if (isNaN()) UNAVAILABLE else format.format(Locale.US, this)

@Composable
private fun TelemetryCard(
    point:     GroundDeformationPoint,
    analysis:  AnalysisState?,
    maxHeight: androidx.compose.ui.unit.Dp,
    onClose:   () -> Unit,
    onRefresh: () -> Unit,
    onAnalyze: () -> Unit
) {
    val (accent, bg) = when (point.riskSeverity) {
        Severity.CRITICAL -> Pair(RiskCritical,  RiskCriticalContainer)
        Severity.HIGH     -> Pair(RiskHigh,      RiskHighContainer)
        Severity.MODERATE -> Pair(RiskModerate,  RiskModerateContainer)
        Severity.LOW      -> Pair(RiskLow,       RiskLowContainer)
    }
    val loading = analysis == null || analysis == AnalysisState.Loading
    val pending = if (loading) "…" else UNAVAILABLE

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(8.dp, RoundedCornerShape(22.dp)),
        colors    = CardDefaults.cardColors(containerColor = BgSurface),
        shape     = RoundedCornerShape(22.dp),
        border    = androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(bg)
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Radar, null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            point.riskSeverity.name + "  RISK",
                            color = accent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(point.label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${"%.4f".format(Locale.US, point.latitude)}°N,  ${"%.4f".format(Locale.US, point.longitude)}°E  •  " +
                                if (point.slopeAngleDegrees.isNaN()) "slope $pending" else "${point.slopeAngleDegrees}° slope",
                        color = TextMuted, fontSize = 10.sp
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Telemetry Grid (real values or explicit "Unavailable") ────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelCell("ALOS-4 INSAR", point.displacementRateMmPerYear.orUnavailable("%.1f mm/y"), Icons.Filled.Speed, RiskCritical, RiskCriticalContainer, Modifier.weight(1f))
                TelCell("NDVI", if (point.ndviScore.isNaN()) pending else "%.2f".format(Locale.US, point.ndviScore), Icons.Filled.Landscape, RiskLow, RiskLowContainer, Modifier.weight(1f))
                TelCell("SOIL MOIST.", if (point.soilMoisturePercentage < 0) UNAVAILABLE else "${point.soilMoisturePercentage}%", Icons.Filled.Waves, SentinelBrand, CyanContainer, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))

            // ── Last scan info ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgElevated)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(point.lastScanDate, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Sentinel-1 SAR backscatter: " +
                            if (point.radarBackscatterDb.isNaN()) pending else "${point.radarBackscatterDb} dB",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Full analysis ─────────────────────────────────────────────
            (analysis as? AnalysisState.Ready)?.let {
                RiskIndexSummary(it.analysis.risk)
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Latest satellite & live readings", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (analysis is AnalysisState.Ready) {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.Refresh, "Refresh readings", tint = CyanPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            LiveReadings(state = analysis)

            Spacer(Modifier.height(12.dp))

            // ── CTA ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(CyanPrimary, EmeraldPrimary)))
                    .clickable(onClick = onAnalyze),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Analytics, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("OPEN FULL AREA DETAILS", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.2.sp)
                }
            }
        }
    }
}

// ─── Telemetry Cell ───────────────────────────────────────────────────────────

@Composable
private fun TelCell(
    title:    String,
    value:    String,
    icon:     ImageVector,
    accent:   Color,
    bg:       Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                color = if (value == UNAVAILABLE) TextMuted else TextPrimary,
                fontSize = if (value == UNAVAILABLE) 10.sp else 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(title, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.3.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
