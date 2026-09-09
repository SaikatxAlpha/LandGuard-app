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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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
import com.example.landguard.ui.theme.CoreBackground
import com.example.landguard.ui.theme.CyberBlue
import com.example.landguard.ui.theme.CyberCyan
import com.example.landguard.ui.theme.CyberGreen
import com.example.landguard.ui.theme.CyberOrange
import com.example.landguard.ui.theme.CyberRed
import com.example.landguard.ui.theme.GlassBackground
import com.example.landguard.ui.theme.GlassBorder
import com.example.landguard.ui.theme.SurfaceDark
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

enum class MapStyleType { HYBRID, SATELLITE, TERRAIN, NORMAL }

@Composable
fun MapScreen(
    onOpenZone: (String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMapType by remember { mutableStateOf(MapStyleType.HYBRID) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val currentPoints by rememberUpdatedState(state.deformationPoints)
    val currentViewModel by rememberUpdatedState(viewModel)
    val currentSource by rememberUpdatedState(state.selectedSource)
    val currentOpacity by rememberUpdatedState(state.opacity)

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { maplibreMap ->
                maplibreMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(27.02, 88.38))
                    .zoom(10.5)
                    .build()
                
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
                                LatLng(clickedPoint.latitude, clickedPoint.longitude),
                                12.0
                            ), 1000
                        )
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CoreBackground)) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberCyan)
            }
        } else {
            // 1. MapLibre Native Layer
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Map Style Watcher
            LaunchedEffect(selectedMapType) {
                val styleUrl = when (selectedMapType) {
                    MapStyleType.TERRAIN -> "https://tiles.openfreemap.org/styles/fiord-color"
                    MapStyleType.NORMAL -> "https://tiles.openfreemap.org/styles/positron"
                    else -> "https://tiles.openfreemap.org/styles/liberty" // Liberty serves as our base hybrid/satellite equivalent
                }
                mapView.getMapAsync { maplibreMap ->
                    maplibreMap.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                        setupMapLayers(style, currentPoints, currentSource, currentOpacity)
                    }
                }
            }

            // Data & Source Layer Watcher
            LaunchedEffect(state.deformationPoints, state.opacity, state.selectedSource) {
                mapView.getMapAsync { maplibreMap ->
                    maplibreMap.getStyle { style ->
                        setupMapLayers(style, state.deformationPoints, state.selectedSource, state.opacity)
                    }
                }
            }

            // 2. Top Control Panel
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                MapGlassControlPanel(
                    selectedSource = state.selectedSource,
                    selectedLayer = state.selectedLayer,
                    selectedMapType = selectedMapType,
                    opacity = state.opacity,
                    onSourceSelected = { viewModel.selectSource(it) },
                    onLayerSelected = { viewModel.selectLayer(it) },
                    onMapTypeSelected = { selectedMapType = it },
                    onOpacityChange = { viewModel.setOpacity(it) },
                    onApiClick = { viewModel.setShowApiDialog(true) }
                )
            }

            // 3. Bottom Telemetry Sheet
            AnimatedVisibility(
                visible = state.selectedPoint != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
            ) {
                state.selectedPoint?.let { point ->
                    CyberTelemetryCard(
                        point = point,
                        onClose = { viewModel.selectDeformationPoint(null) },
                        onAnalyze = { onOpenZone(point.id) }
                    )
                }
            }
        }
    }

    if (state.showApiDialog) {
        SatelliteApiGuideDialog(
            currentConfig = state.apiConfig,
            onDismiss = { viewModel.setShowApiDialog(false) },
            onSaveConfig = { viewModel.updateApiConfig(it) }
        )
    }
}

private fun getRiskColorHex(severity: Severity, source: SatelliteSource): String {
    if (source == SatelliteSource.SENTINEL2_MSI) {
        return when (severity) {
            Severity.CRITICAL -> "#D97706"
            Severity.HIGH -> "#FF9100"
            Severity.MODERATE -> "#84CC16"
            Severity.LOW -> "#00E676"
        }
    }
    
    return when (severity) {
        Severity.CRITICAL -> "#FF2A55"
        Severity.HIGH -> "#FF9100"
        Severity.MODERATE -> "#00F0FF"
        Severity.LOW -> "#00E676"
    }
}

private fun createCirclePolygon(center: LatLng, radiusInMeters: Double): Polygon {
    val points = mutableListOf<Point>()
    val earthRadius = 6378137.0
    val d = radiusInMeters / earthRadius
    val lat1 = Math.toRadians(center.latitude)
    val lng1 = Math.toRadians(center.longitude)
    
    for (i in 0..360 step 10) {
        val tc = Math.toRadians(i.toDouble())
        val lat2 = Math.asin(Math.sin(lat1) * Math.cos(d) + Math.cos(lat1) * Math.sin(d) * Math.cos(tc))
        val lng2 = lng1 + Math.atan2(Math.sin(tc) * Math.sin(d) * Math.cos(lat1), Math.cos(d) - Math.sin(lat1) * Math.sin(lat2))
        points.add(Point.fromLngLat(Math.toDegrees(lng2), Math.toDegrees(lat2)))
    }
    points.add(points.first())
    return Polygon.fromLngLats(listOf(points))
}

private fun setupMapLayers(
    style: Style,
    points: List<GroundDeformationPoint>,
    source: SatelliteSource,
    opacity: Float
) {
    val auraFeatures = points.map { pt ->
        val colorHex = getRiskColorHex(pt.riskSeverity, source)
        val polygon = createCirclePolygon(LatLng(pt.latitude, pt.longitude), 2800.0)
        val feature = Feature.fromGeometry(polygon)
        feature.addStringProperty("color", colorHex)
        feature.addNumberProperty("opacity", opacity * 0.25f)
        feature
    }
    
    val coreFeatures = points.map { pt ->
        val colorHex = getRiskColorHex(pt.riskSeverity, source)
        val polygon = createCirclePolygon(LatLng(pt.latitude, pt.longitude), 1200.0)
        val feature = Feature.fromGeometry(polygon)
        feature.addStringProperty("color", colorHex)
        feature.addNumberProperty("opacity", opacity * 0.75f)
        feature
    }

    var auraSource = style.getSourceAs<GeoJsonSource>("aura-source")
    if (auraSource == null) {
        auraSource = GeoJsonSource("aura-source")
        style.addSource(auraSource)
        style.addLayer(FillLayer("aura-layer", "aura-source").withProperties(
            PropertyFactory.fillColor(Expression.get("color")),
            PropertyFactory.fillOpacity(Expression.get("opacity"))
        ))
    }
    auraSource.setGeoJson(FeatureCollection.fromFeatures(auraFeatures))

    var coreSource = style.getSourceAs<GeoJsonSource>("core-source")
    if (coreSource == null) {
        coreSource = GeoJsonSource("core-source")
        style.addSource(coreSource)
        style.addLayer(FillLayer("core-layer", "core-source").withProperties(
            PropertyFactory.fillColor(Expression.get("color")),
            PropertyFactory.fillOpacity(Expression.get("opacity"))
        ))
        style.addLayer(LineLayer("core-stroke-layer", "core-source").withProperties(
            PropertyFactory.lineColor("rgba(255, 255, 255, 0.9)"),
            PropertyFactory.lineWidth(4f)
        ))
    }
    coreSource.setGeoJson(FeatureCollection.fromFeatures(coreFeatures))
}

@Composable
private fun MapGlassControlPanel(
    selectedSource: SatelliteSource,
    selectedLayer: SatelliteLayer,
    selectedMapType: MapStyleType,
    opacity: Float,
    onSourceSelected: (SatelliteSource) -> Unit,
    onLayerSelected: (SatelliteLayer) -> Unit,
    onMapTypeSelected: (MapStyleType) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onApiClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Radar,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("SATELLITE TARGETING", color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Earth Observation Array", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onApiClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Api, contentDescription = "API Config", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(14.dp))
                
                // Map Style Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BASE MAP", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    
                    listOf(
                        "HYBRID" to MapStyleType.HYBRID,
                        "NORMAL" to MapStyleType.NORMAL,
                        "TERRAIN" to MapStyleType.TERRAIN
                    ).forEach { (label, type) ->
                        val isSel = selectedMapType == type
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CyberCyan else SurfaceDark)
                                .clickable { onMapTypeSelected(type) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(label, color = if (isSel) CoreBackground else TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Source Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SatelliteSource.entries.forEach { source ->
                        val isSelected = selectedSource == source
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CyberBlue.copy(alpha = 0.25f) else SurfaceDark.copy(alpha = 0.6f))
                                .border(1.dp, if (isSelected) CyberCyan else GlassBorder, RoundedCornerShape(12.dp))
                                .clickable { onSourceSelected(source) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = source.displayName.split(" ")[0],
                                    color = if (isSelected) CyberCyan else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (source == SatelliteSource.ALOS4_PALSAR3) "L-SAR" else if (source == SatelliteSource.SENTINEL2_MSI) "Optical" else "Fusion",
                                    color = if (isSelected) TextPrimary else TextMuted,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Layer Scroller
                val layers = when (selectedSource) {
                    SatelliteSource.ALOS4_PALSAR3 -> listOf(SatelliteLayer.ALOS4_INSAR_DISPLACEMENT, SatelliteLayer.ALOS4_SAR_BACKSCATTER)
                    SatelliteSource.SENTINEL2_MSI -> listOf(SatelliteLayer.SENTINEL2_NDVI, SatelliteLayer.SENTINEL2_MOISTURE, SatelliteLayer.SENTINEL2_TRUE_COLOR)
                    SatelliteSource.HYBRID_FUSION -> listOf(SatelliteLayer.ALOS4_INSAR_DISPLACEMENT, SatelliteLayer.SENTINEL2_NDVI)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    layers.forEach { layer ->
                        val isSelected = selectedLayer == layer
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) CyberCyan else Color.Transparent)
                                .border(1.dp, if (isSelected) CyberCyan else GlassBorder, RoundedCornerShape(14.dp))
                                .clickable { onLayerSelected(layer) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = layer.title,
                                color = if (isSelected) CoreBackground else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opacity Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("OPACITY", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = opacity,
                        onValueChange = onOpacityChange,
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberBlue,
                            inactiveTrackColor = SurfaceDark
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CyberTelemetryCard(
    point: GroundDeformationPoint,
    onClose: () -> Unit,
    onAnalyze: () -> Unit
) {
    val accentColor = when (point.riskSeverity) {
        Severity.CRITICAL -> CyberRed
        Severity.HIGH -> CyberOrange
        Severity.MODERATE -> CyberCyan
        Severity.LOW -> CyberGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Radar, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("TARGET ACQUIRED", color = accentColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text(point.label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("LAT: ${point.latitude} | LON: ${point.longitude}", color = TextSecondary, fontSize = 10.sp)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Grid (Equal width weights, maxLines = 1 to prevent text overflow/wrapping)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryCell("INSAR SHIFT", "${point.displacementRateMmPerYear} mm", Icons.Filled.Speed, CyberRed, modifier = Modifier.weight(1f))
                TelemetryCell("NDVI SCAR", "${point.ndviScore}", Icons.Filled.Landscape, CyberGreen, modifier = Modifier.weight(1f))
                TelemetryCell("SATURATION", "${point.soilMoisturePercentage}%", Icons.Filled.Waves, CyberBlue, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAnalyze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor)
            ) {
                Icon(Icons.Filled.Analytics, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RUN DEEP ANALYSIS", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun TelemetryCell(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark.copy(alpha = 0.6f))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SatelliteApiGuideDialog(
    currentConfig: SatelliteApiConfig,
    onDismiss: () -> Unit,
    onSaveConfig: (SatelliteApiConfig) -> Unit
) {
    var copernicusId by remember { mutableStateOf(currentConfig.copernicusClientId) }
    var copernicusSecret by remember { mutableStateOf(currentConfig.copernicusClientSecret) }
    var sentinelHubKey by remember { mutableStateOf(currentConfig.sentinelHubApiKey) }
    var jaxaKey by remember { mutableStateOf(currentConfig.jaxaGPortalApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Api, null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SATELLITE UPLINK CONFIG", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = 1.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Configure your direct secure lines to ESA Copernicus and JAXA.", color = TextSecondary, fontSize = 12.sp)

                CyberTextField(value = copernicusId, onValueChange = { copernicusId = it }, label = "Backend Endpoint URL")
                CyberTextField(value = copernicusSecret, onValueChange = { copernicusSecret = it }, label = "Gateway Target")
                CyberTextField(value = sentinelHubKey, onValueChange = { sentinelHubKey = it }, label = "GeoJSON Fetch Route")
                CyberTextField(value = jaxaKey, onValueChange = { jaxaKey = it }, label = "Auth Profile Token")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConfig(
                        SatelliteApiConfig(
                            copernicusClientId = copernicusId,
                            copernicusClientSecret = copernicusSecret,
                            sentinelHubApiKey = sentinelHubKey,
                            jaxaGPortalApiKey = jaxaKey,
                            isConfigured = true
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("ESTABLISH LINK", color = CoreBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ABORT", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun CyberTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMuted) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberCyan,
            unfocusedBorderColor = GlassBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = CyberCyan
        )
    )
}
