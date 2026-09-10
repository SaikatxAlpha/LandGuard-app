// app/src/main/java/com/example/landguard/ui/map/MapScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.landguard.ui.map

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.domain.model.GroundDeformationPoint
import com.example.landguard.domain.model.SatelliteApiConfig
import com.example.landguard.domain.model.SatelliteLayer
import com.example.landguard.domain.model.SatelliteSource
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.theme.Alos4Brand
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.CyanContainer
import com.example.landguard.ui.theme.CyanDim
import com.example.landguard.ui.theme.CyanPrimary
import com.example.landguard.ui.theme.EmeraldContainer
import com.example.landguard.ui.theme.EmeraldPrimary
import com.example.landguard.ui.theme.FusionBrand
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
import com.example.landguard.ui.theme.SentinelBrand
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

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

// ─── Root Screen ──────────────────────────────────────────────────────────────

@Composable
fun MapScreen(
    onOpenZone: (String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val state            by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMapType  by remember { mutableStateOf(MapStyleType.BRIGHT) }
    val context           = LocalContext.current
    val lifecycleOwner    = LocalLifecycleOwner.current
    val currentPoints    by rememberUpdatedState(state.deformationPoints)
    val currentViewModel by rememberUpdatedState(viewModel)
    val currentSource    by rememberUpdatedState(state.selectedSource)
    val currentOpacity   by rememberUpdatedState(state.opacity)

    // ── MapLibre Map View (created once, reused across recompositions) ────────
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { maplibreMap ->
                // Start centred on the Darjeeling–Kalimpong corridor
                maplibreMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(27.02, 88.38))
                    .zoom(10.5)
                    .build()

                // Tap-to-select nearest deformation point
                maplibreMap.addOnMapClickListener { latLng ->
                    var clickedPoint: GroundDeformationPoint? = null
                    var minDistance = Float.MAX_VALUE
                    val results = FloatArray(1)
                    for (point in currentPoints) {
                        android.location.Location.distanceBetween(
                            latLng.latitude, latLng.longitude,
                            point.latitude, point.longitude,
                            results
                        )
                        if (results[0] < 1200f && results[0] < minDistance) {
                            minDistance = results[0]
                            clickedPoint = point
                        }
                    }
                    if (clickedPoint != null) {
                        currentViewModel.selectDeformationPoint(clickedPoint)
                        maplibreMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(clickedPoint.latitude, clickedPoint.longitude), 12.0
                            ), 1000
                        )
                        true
                    } else false
                }
            }
        }
    }

    // ── Lifecycle wiring ──────────────────────────────────────────────────────
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyanPrimary, trackColor = BgBorder, strokeWidth = 3.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading satellite layers…", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {

            // ── Real MapLibre map ─────────────────────────────────────────
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            // ── Reload map style when user switches tile source ───────────
            LaunchedEffect(selectedMapType) {
                val source = resolveStyleSource(selectedMapType)
                mapView.getMapAsync { maplibreMap ->
                    val builder = when (source) {
                        is MapStyleSource.FromUrl  -> Style.Builder().fromUri(source.url)
                        is MapStyleSource.FromJson -> Style.Builder().fromJson(source.json)
                    }
                    maplibreMap.setStyle(builder) { style ->
                        setupMapLayers(style, currentPoints, currentSource, currentOpacity)
                    }
                }
            }

            // ── Refresh risk-zone overlays when data or opacity changes ───
            LaunchedEffect(state.deformationPoints, state.opacity, state.selectedSource) {
                mapView.getMapAsync { maplibreMap ->
                    maplibreMap.getStyle { style ->
                        setupMapLayers(style, state.deformationPoints, state.selectedSource, state.opacity)
                    }
                }
            }

            // ── Floating Control Panel (top) ──────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                MapControlPanel(
                    selectedSource    = state.selectedSource,
                    selectedLayer     = state.selectedLayer,
                    selectedMapType   = selectedMapType,
                    opacity           = state.opacity,
                    onSourceSelected  = { viewModel.selectSource(it) },
                    onLayerSelected   = { viewModel.selectLayer(it) },
                    onMapTypeSelected = { selectedMapType = it },
                    onOpacityChange   = { viewModel.setOpacity(it) },
                    onApiClick        = { viewModel.setShowApiDialog(true) }
                )
            }

            // ── Map Style Legend (bottom-left corner) ─────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 14.dp, bottom = 104.dp)
            ) {
                MapLegend(selectedMapType = selectedMapType)
            }

            // ── Telemetry Card (slides up from bottom when point selected) ─
            AnimatedVisibility(
                visible  = state.selectedPoint != null,
                enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 100.dp)
            ) {
                state.selectedPoint?.let { point ->
                    TelemetryCard(
                        point     = point,
                        onClose   = { viewModel.selectDeformationPoint(null) },
                        onAnalyze = { onOpenZone(point.id) }
                    )
                }
            }
        }
    }

    // ── API Config Dialog ─────────────────────────────────────────────────────
    if (state.showApiDialog) {
        ApiConfigDialog(
            currentConfig = state.apiConfig,
            onDismiss     = { viewModel.setShowApiDialog(false) },
            onSaveConfig  = { viewModel.updateApiConfig(it) }
        )
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

private fun setupMapLayers(
    style:   Style,
    points:  List<GroundDeformationPoint>,
    source:  SatelliteSource,
    opacity: Float
) {
    // Outer translucent aura ring (wide halo effect)
    val auraFeatures = points.map { pt ->
        val colorHex = getRiskColorHex(pt.riskSeverity, source)
        Feature.fromGeometry(createCirclePolygon(LatLng(pt.latitude, pt.longitude), 2800.0)).apply {
            addStringProperty("color", colorHex)
            addNumberProperty("opacity", opacity * 0.18f)
        }
    }

    // Core filled circle (tight footprint)
    val coreFeatures = points.map { pt ->
        val colorHex = getRiskColorHex(pt.riskSeverity, source)
        Feature.fromGeometry(createCirclePolygon(LatLng(pt.latitude, pt.longitude), 1200.0)).apply {
            addStringProperty("color", colorHex)
            addNumberProperty("opacity", opacity * 0.68f)
        }
    }

    // Aura layer
    var auraSource = style.getSourceAs<GeoJsonSource>("aura-source")
    if (auraSource == null) {
        auraSource = GeoJsonSource("aura-source")
        style.addSource(auraSource)
        style.addLayer(
            FillLayer("aura-layer", "aura-source").withProperties(
                PropertyFactory.fillColor(Expression.get("color")),
                PropertyFactory.fillOpacity(Expression.get("opacity"))
            )
        )
    }
    auraSource.setGeoJson(FeatureCollection.fromFeatures(auraFeatures))

    // Core fill + white stroke
    var coreSource = style.getSourceAs<GeoJsonSource>("core-source")
    if (coreSource == null) {
        coreSource = GeoJsonSource("core-source")
        style.addSource(coreSource)
        style.addLayer(
            FillLayer("core-layer", "core-source").withProperties(
                PropertyFactory.fillColor(Expression.get("color")),
                PropertyFactory.fillOpacity(Expression.get("opacity"))
            )
        )
        style.addLayer(
            LineLayer("core-stroke-layer", "core-source").withProperties(
                PropertyFactory.lineColor("rgba(255, 255, 255, 0.9)"),
                PropertyFactory.lineWidth(2.5f)
            )
        )
    }
    coreSource.setGeoJson(FeatureCollection.fromFeatures(coreFeatures))
}

// ─── Map Style Legend ─────────────────────────────────────────────────────────

@Composable
private fun MapLegend(selectedMapType: MapStyleType) {
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
            LegendRow(color = RiskCritical,  label = "Critical  (>75%)")
            LegendRow(color = RiskHigh,      label = "High      (50–74%)")
            LegendRow(color = RiskModerate,  label = "Moderate  (25–49%)")
            LegendRow(color = RiskLow,       label = "Low       (<25%)")
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

// ─── Map Control Panel ────────────────────────────────────────────────────────

@Composable
private fun MapControlPanel(
    selectedSource:    SatelliteSource,
    selectedLayer:     SatelliteLayer,
    selectedMapType:   MapStyleType,
    opacity:           Float,
    onSourceSelected:  (SatelliteSource) -> Unit,
    onLayerSelected:   (SatelliteLayer) -> Unit,
    onMapTypeSelected: (MapStyleType) -> Unit,
    onOpacityChange:   (Float) -> Unit,
    onApiClick:        () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(BgSurface)
            .border(1.dp, BgBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

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
                    Text("Satellite Insights & Parcel Risk Layers", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onApiClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Api, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Filled.Close else Icons.Filled.KeyboardArrowDown,
                        null, tint = TextPrimary, modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(14.dp))

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

                // ── Data Source Selector ──────────────────────────────────
                Text("DATA SOURCE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
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

                Spacer(Modifier.height(14.dp))

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
}

// ─── Bottom Telemetry Card ────────────────────────────────────────────────────

@Composable
private fun TelemetryCard(
    point:     GroundDeformationPoint,
    onClose:   () -> Unit,
    onAnalyze: () -> Unit
) {
    val (accent, bg) = when (point.riskSeverity) {
        Severity.CRITICAL -> Pair(RiskCritical,  RiskCriticalContainer)
        Severity.HIGH     -> Pair(RiskHigh,      RiskHighContainer)
        Severity.MODERATE -> Pair(RiskModerate,  RiskModerateContainer)
        Severity.LOW      -> Pair(RiskLow,       RiskLowContainer)
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(8.dp, RoundedCornerShape(22.dp)),
        colors    = CardDefaults.cardColors(containerColor = BgSurface),
        shape     = RoundedCornerShape(22.dp),
        border    = androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

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
                        "${"%.4f".format(point.latitude)}°N,  ${"%.4f".format(point.longitude)}°E  •  ${point.slopeAngleDegrees}° slope",
                        color = TextMuted, fontSize = 10.sp
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Telemetry Grid ────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelCell("INSAR SHIFT", "${point.displacementRateMmPerYear} mm/y", Icons.Filled.Speed,     RiskCritical,  RiskCriticalContainer, Modifier.weight(1f))
                TelCell("NDVI",        "${point.ndviScore}",                       Icons.Filled.Landscape, RiskLow,       RiskLowContainer,       Modifier.weight(1f))
                TelCell("SOIL SAT.",   "${point.soilMoisturePercentage}%",         Icons.Filled.Waves,     SentinelBrand, CyanContainer,          Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))

            // ── Last scan info ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgElevated)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Last scan:", color = TextMuted, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                Text(point.lastScanDate, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("SAR backscatter: ${point.radarBackscatterDb} dB", color = TextMuted, fontSize = 10.sp)
            }

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
                    Text("RUN DEEP ANALYSIS", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.2.sp)
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
            Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(title, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.3.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─── API Config Dialog ────────────────────────────────────────────────────────

@Composable
private fun ApiConfigDialog(
    currentConfig: SatelliteApiConfig,
    onDismiss:     () -> Unit,
    onSaveConfig:  (SatelliteApiConfig) -> Unit
) {
    var copernicusId     by remember { mutableStateOf(currentConfig.copernicusClientId) }
    var copernicusSecret by remember { mutableStateOf(currentConfig.copernicusClientSecret) }
    var sentinelHubKey   by remember { mutableStateOf(currentConfig.sentinelHubApiKey) }
    var jaxaKey          by remember { mutableStateOf(currentConfig.jaxaGPortalApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = BgSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyanContainer)
                        .border(1.dp, CyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Api, null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("BACKEND UPLINK", color = CyanPrimary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                    Text("API Gateway Configuration", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Configure secure backend gateways to ESA Copernicus Data Space and JAXA G-Portal.", color = TextSecondary, fontSize = 12.sp)
                StyledTextField(copernicusId,     { copernicusId = it },     "Backend Endpoint URL")
                StyledTextField(copernicusSecret, { copernicusSecret = it }, "Gateway Target / Client ID")
                StyledTextField(sentinelHubKey,   { sentinelHubKey = it },   "GeoJSON Fetch Route")
                StyledTextField(jaxaKey,          { jaxaKey = it },          "Auth Profile Token")
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyanPrimary)
                    .clickable {
                        onSaveConfig(
                            SatelliteApiConfig(
                                copernicusClientId     = copernicusId,
                                copernicusClientSecret = copernicusSecret,
                                sentinelHubApiKey      = sentinelHubKey,
                                jaxaGPortalApiKey      = jaxaKey,
                                isConfigured           = true
                            )
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("ESTABLISH LINK", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ABORT", color = TextMuted)
            }
        }
    )
}

@Composable
private fun StyledTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = { Text(label, fontSize = 12.sp) },
        modifier        = Modifier.fillMaxWidth(),
        singleLine      = true,
        shape           = RoundedCornerShape(12.dp),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = CyanPrimary,
            unfocusedBorderColor    = BgBorder,
            focusedTextColor        = TextPrimary,
            unfocusedTextColor      = TextPrimary,
            cursorColor             = CyanPrimary,
            focusedContainerColor   = BgElevated,
            unfocusedContainerColor = BgSurface,
            focusedLabelColor       = CyanPrimary,
            unfocusedLabelColor     = TextMuted
        )
    )
}