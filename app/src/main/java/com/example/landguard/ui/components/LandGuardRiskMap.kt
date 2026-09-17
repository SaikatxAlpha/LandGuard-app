package com.example.landguard.ui.components

import android.graphics.PointF
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.location.UserLocation
import com.example.landguard.ui.risk.RiskArea
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * LandGuard risk map.
 *
 * A focused, map-first MapLibre view (same engine and tile providers as
 * the full analysis map in MapScreen) used by Home and Risk Areas.
 *
 * Each risk area is drawn the same way MapScreen draws it: as real
 * ground coverage — a wide translucent aura ring and a filled core
 * footprint with a white outline — so the affected terrain is visible
 * instead of a small marker.
 */

enum class RiskMapStyle(val label: String) {
    STREETS("Streets"),
    TERRAIN("Terrain"),
    SATELLITE("Satellite")
}

/** A one-shot camera request. Change [token] to re-trigger the same target. */
@Immutable
data class MapFocus(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double = 11.5,
    val token: Int = 0
)

/** Request to fit every area (and the user) into view. */
@Immutable
data class MapFitAll(val token: Int)

private const val DEFAULT_LAT = 20.5937
private const val DEFAULT_LNG = 78.9629

/** Same footprint sizes as MapScreen's overlays. */
private const val AURA_RADIUS_M = 2800.0
private const val CORE_RADIUS_M = 1200.0

private const val SOURCE_AURA = "lg-aura-source"
private const val SOURCE_CORE = "lg-core-source"
private const val SOURCE_USER = "lg-user"
private const val LAYER_PULSE = "lg-pulse"
private const val LAYER_AURA = "lg-aura"
private const val LAYER_CORE = "lg-core"
private const val LAYER_CORE_STROKE = "lg-core-stroke"
private const val LAYER_SELECTED = "lg-selected"
private const val LAYER_USER_ACCURACY = "lg-user-ring"
private const val LAYER_USER = "lg-user-dot"

private val TOPO_STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "topo": {
      "type": "raster",
      "tiles": ["https://tile.opentopomap.org/{z}/{x}/{y}.png"],
      "tileSize": 256, "maxzoom": 17,
      "attribution": "© OpenTopoMap (CC-BY-SA) | © OpenStreetMap contributors"
    }
  },
  "layers": [
    { "id": "bg", "type": "background", "paint": { "background-color": "#f4f1eb" } },
    { "id": "topo", "type": "raster", "source": "topo",
      "paint": { "raster-brightness-min": 0.05, "raster-saturation": 0.1 } }
  ]
}
""".trimIndent()

private val SATELLITE_STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "sat": {
      "type": "raster",
      "tiles": ["https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"],
      "tileSize": 256, "maxzoom": 19,
      "attribution": "© ESRI, Maxar, GeoEye, Earthstar Geographics, CNES/Airbus DS"
    }
  },
  "layers": [
    { "id": "bg", "type": "background", "paint": { "background-color": "#0a0f1a" } },
    { "id": "sat", "type": "raster", "source": "sat", "paint": { "raster-contrast": 0.08, "raster-saturation": 0.15 } }
  ]
}
""".trimIndent()

private const val STREETS_STYLE_URL = "https://tiles.openfreemap.org/styles/bright"
private const val FALLBACK_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"

private fun styleBuilder(style: RiskMapStyle): Style.Builder = when (style) {
    RiskMapStyle.STREETS -> Style.Builder().fromUri(STREETS_STYLE_URL)
    RiskMapStyle.TERRAIN -> Style.Builder().fromJson(TOPO_STYLE_JSON)
    RiskMapStyle.SATELLITE -> Style.Builder().fromJson(SATELLITE_STYLE_JSON)
}

fun severityHex(severity: Severity): String = when (severity) {
    Severity.CRITICAL -> "#DC2626"
    Severity.HIGH -> "#EA580C"
    Severity.MODERATE -> "#D97706"
    Severity.LOW -> "#16A34A"
}

@Composable
fun LandGuardRiskMap(
    areas: List<RiskArea>,
    userLocation: UserLocation?,
    selectedAreaId: String?,
    onAreaClick: (RiskArea) -> Unit,
    onMapBackgroundClick: () -> Unit,
    modifier: Modifier = Modifier,
    mapStyle: RiskMapStyle = RiskMapStyle.STREETS,
    focus: MapFocus? = null,
    fitAll: MapFitAll? = null,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentAreas by rememberUpdatedState(areas)
    val currentOnAreaClick by rememberUpdatedState(onAreaClick)
    val currentOnBackgroundClick by rememberUpdatedState(onMapBackgroundClick)

    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var styleGeneration by remember { mutableIntStateOf(0) }
    var hasPlacedCamera by remember { mutableStateOf(false) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }

    val tapSlopPx = with(density) { 28.dp.toPx() }

    val mapView = remember {
        MapLibre.getInstance(context)
        val options = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true)
            .camera(
                CameraPosition.Builder()
                    .target(LatLng(DEFAULT_LAT, DEFAULT_LNG))
                    .zoom(4.0)
                    .build()
            )
        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.uiSettings.apply {
                    isRotateGesturesEnabled = false
                    isTiltGesturesEnabled = false
                    isCompassEnabled = false
                    isLogoEnabled = false
                    attributionGravity = Gravity.TOP or Gravity.START
                    setAttributionTintColor(android.graphics.Color.parseColor("#4A6558"))
                }
                map.addOnMapClickListener { latLng ->
                    val area = findTappedArea(map, map.projection.toScreenLocation(latLng), currentAreas, tapSlopPx)
                    if (area != null) {
                        currentOnAreaClick(area)
                    } else {
                        currentOnBackgroundClick()
                    }
                    true
                }
            }
            var fallbackAttempted = false
            addOnDidFailLoadingMapListener {
                // Graceful fallback (once) if the primary vector style is unreachable.
                if (fallbackAttempted) return@addOnDidFailLoadingMapListener
                fallbackAttempted = true
                getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri(FALLBACK_STYLE_URL)) { style ->
                        installLayers(style)
                        loadedStyle = style
                        styleGeneration++
                    }
                }
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
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

    // ── Style ────────────────────────────────────────────────────────────
    LaunchedEffect(mapStyle) {
        mapView.getMapAsync { map ->
            map.setStyle(styleBuilder(mapStyle)) { style ->
                installLayers(style)
                loadedStyle = style
                styleGeneration++
            }
        }
    }

    // ── Insets for attribution ───────────────────────────────────────────
    LaunchedEffect(topInset) {
        val topPx = with(density) { (topInset + 4.dp).roundToPx() }
        val sidePx = with(density) { 10.dp.roundToPx() }
        mapView.getMapAsync { map ->
            map.uiSettings.setAttributionMargins(sidePx, topPx, sidePx, 0)
        }
    }

    // ── Data ─────────────────────────────────────────────────────────────
    LaunchedEffect(areas, selectedAreaId, userLocation, styleGeneration) {
        val style = loadedStyle ?: return@LaunchedEffect
        if (!style.isFullyLoaded) return@LaunchedEffect
        updateAreas(style, areas, selectedAreaId)
        updateUser(style, userLocation)
    }

    // ── Initial camera: the user's position is the default centre ────────
    LaunchedEffect(userLocation) {
        val user = userLocation ?: return@LaunchedEffect
        if (hasCenteredOnUser) return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(user.latitude, user.longitude), 10.5),
                1200
            )
        }
        hasCenteredOnUser = true
        hasPlacedCamera = true
    }

    // Until a fix arrives, frame the monitored areas instead of a blank globe.
    LaunchedEffect(areas.isNotEmpty()) {
        if (hasPlacedCamera || userLocation != null || areas.isEmpty()) return@LaunchedEffect
        mapView.getMapAsync { map ->
            if (!hasPlacedCamera) {
                fitCamera(map, areas, null, density.run { topInset.roundToPx() }, density.run { bottomInset.roundToPx() }, animate = false)
                hasPlacedCamera = true
            }
        }
    }

    // ── Explicit focus / fit requests ────────────────────────────────────
    LaunchedEffect(focus) {
        val target = focus ?: return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), target.zoom),
                900
            )
            hasPlacedCamera = true
        }
    }

    LaunchedEffect(fitAll) {
        if (fitAll == null) return@LaunchedEffect
        mapView.getMapAsync { map ->
            fitCamera(map, areas, userLocation, density.run { topInset.roundToPx() }, density.run { bottomInset.roundToPx() }, animate = true)
            hasPlacedCamera = true
        }
    }

    // ── Gentle breathing on critical / high coverage (only while resumed) ─
    val hasUrgentAreas = areas.any { it.severity >= Severity.HIGH }
    LaunchedEffect(styleGeneration, hasUrgentAreas, lifecycleOwner) {
        val style = loadedStyle ?: return@LaunchedEffect
        if (!hasUrgentAreas) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val phase = (System.currentTimeMillis() % 2600L) / 2600f
                val wave = (sin(phase * 2 * PI).toFloat() + 1f) / 2f
                runCatching {
                    style.getLayer(LAYER_PULSE)?.setProperties(
                        PropertyFactory.fillOpacity(0.06f + 0.14f * wave)
                    )
                }
                delay(50)
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

// ─────────────────────────────────────────────────────────────
// Hit testing
// ─────────────────────────────────────────────────────────────

private fun findTappedArea(
    map: MapLibreMap,
    screenPoint: PointF,
    areas: List<RiskArea>,
    slopPx: Float
): RiskArea? {
    // 1) Tap inside a drawn footprint (smallest / core first).
    val hitId = map.queryRenderedFeatures(screenPoint, LAYER_CORE, LAYER_AURA)
        .firstNotNullOfOrNull { it.getStringProperty("id") }
    hitId?.let { id -> areas.firstOrNull { it.id == id }?.let { return it } }

    // 2) When zoomed out the footprints are tiny: pick the nearest centre within a finger's reach.
    return areas
        .map { area ->
            val p = map.projection.toScreenLocation(LatLng(area.latitude, area.longitude))
            area to hypot((p.x - screenPoint.x).toDouble(), (p.y - screenPoint.y).toDouble())
        }
        .filter { it.second <= slopPx }
        .minByOrNull { it.second }
        ?.first
}

// ─────────────────────────────────────────────────────────────
// Layers
// ─────────────────────────────────────────────────────────────

private fun installLayers(style: Style) {
    if (style.getSource(SOURCE_AURA) == null) style.addSource(GeoJsonSource(SOURCE_AURA))
    if (style.getSource(SOURCE_CORE) == null) style.addSource(GeoJsonSource(SOURCE_CORE))
    if (style.getSource(SOURCE_USER) == null) style.addSource(GeoJsonSource(SOURCE_USER))

    // Outer translucent aura ring (wide halo of the affected terrain)
    if (style.getLayer(LAYER_AURA) == null) {
        style.addLayer(
            FillLayer(LAYER_AURA, SOURCE_AURA).withProperties(
                PropertyFactory.fillColor(Expression.get("color")),
                PropertyFactory.fillOpacity(0.16f)
            )
        )
    }
    // Breathing overlay on critical / high auras
    if (style.getLayer(LAYER_PULSE) == null) {
        style.addLayer(
            FillLayer(LAYER_PULSE, SOURCE_AURA).withProperties(
                PropertyFactory.fillColor(Expression.get("color")),
                PropertyFactory.fillOpacity(0f)
            ).withFilter(Expression.gte(Expression.get("rank"), Expression.literal(Severity.HIGH.ordinal)))
        )
    }
    // Core footprint
    if (style.getLayer(LAYER_CORE) == null) {
        style.addLayer(
            FillLayer(LAYER_CORE, SOURCE_CORE).withProperties(
                PropertyFactory.fillColor(Expression.get("color")),
                PropertyFactory.fillOpacity(0.58f)
            )
        )
    }
    // White outline, as in MapScreen
    if (style.getLayer(LAYER_CORE_STROKE) == null) {
        style.addLayer(
            LineLayer(LAYER_CORE_STROKE, SOURCE_CORE).withProperties(
                PropertyFactory.lineColor("rgba(255, 255, 255, 0.9)"),
                PropertyFactory.lineWidth(2.5f)
            )
        )
    }
    // Selected area: dark outline around the whole aura
    if (style.getLayer(LAYER_SELECTED) == null) {
        style.addLayer(
            LineLayer(LAYER_SELECTED, SOURCE_AURA).withProperties(
                PropertyFactory.lineColor("#0D1812"),
                PropertyFactory.lineWidth(2.5f),
                PropertyFactory.lineDasharray(arrayOf(2f, 1.5f))
            ).withFilter(Expression.eq(Expression.get("selected"), Expression.literal(true)))
        )
    }
    // User position
    if (style.getLayer(LAYER_USER_ACCURACY) == null) {
        style.addLayer(
            CircleLayer(LAYER_USER_ACCURACY, SOURCE_USER).withProperties(
                PropertyFactory.circleColor("#2563EB"),
                PropertyFactory.circleRadius(18f),
                PropertyFactory.circleOpacity(0.15f)
            )
        )
    }
    if (style.getLayer(LAYER_USER) == null) {
        style.addLayer(
            CircleLayer(LAYER_USER, SOURCE_USER).withProperties(
                PropertyFactory.circleColor("#2563EB"),
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
                PropertyFactory.circleStrokeWidth(3f)
            )
        )
    }
}

private fun updateAreas(style: Style, areas: List<RiskArea>, selectedId: String?) {
    fun feature(area: RiskArea, radius: Double) =
        Feature.fromGeometry(circlePolygon(area.latitude, area.longitude, radius)).apply {
            addStringProperty("id", area.id)
            addStringProperty("color", severityHex(area.severity))
            addNumberProperty("rank", area.severity.ordinal)
            addBooleanProperty("selected", area.id == selectedId)
        }

    style.getSourceAs<GeoJsonSource>(SOURCE_AURA)
        ?.setGeoJson(FeatureCollection.fromFeatures(areas.map { feature(it, AURA_RADIUS_M) }))
    style.getSourceAs<GeoJsonSource>(SOURCE_CORE)
        ?.setGeoJson(FeatureCollection.fromFeatures(areas.map { feature(it, CORE_RADIUS_M) }))
}

private fun updateUser(style: Style, user: UserLocation?) {
    val features = listOfNotNull(
        user?.let { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) }
    )
    style.getSourceAs<GeoJsonSource>(SOURCE_USER)
        ?.setGeoJson(FeatureCollection.fromFeatures(features))
}

/** Geodesic circle polygon around a centre (same construction as MapScreen). */
private fun circlePolygon(lat: Double, lng: Double, radiusMeters: Double): Polygon {
    val earthRadius = 6378137.0
    val d = radiusMeters / earthRadius
    val lat1 = Math.toRadians(lat)
    val lng1 = Math.toRadians(lng)
    val points = (0..360 step 10).map { deg ->
        val tc = Math.toRadians(deg.toDouble())
        val lat2 = asin(sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(tc))
        val lng2 = lng1 + atan2(sin(tc) * sin(d) * cos(lat1), cos(d) - sin(lat1) * sin(lat2))
        Point.fromLngLat(Math.toDegrees(lng2), Math.toDegrees(lat2))
    }.toMutableList()
    points.add(points.first())
    return Polygon.fromLngLats(listOf(points))
}

private fun fitCamera(
    map: MapLibreMap,
    areas: List<RiskArea>,
    user: UserLocation?,
    topPx: Int,
    bottomPx: Int,
    animate: Boolean
) {
    val points = areas.map { LatLng(it.latitude, it.longitude) } +
            listOfNotNull(user?.let { LatLng(it.latitude, it.longitude) })
    when {
        points.isEmpty() -> return
        points.size == 1 -> {
            val update = CameraUpdateFactory.newLatLngZoom(points.first(), 11.0)
            if (animate) map.animateCamera(update, 900) else map.moveCamera(update)
        }
        else -> {
            val bounds = LatLngBounds.Builder().includes(points).build()
            val side = 120
            val update = CameraUpdateFactory.newLatLngBounds(bounds, side, topPx + 80, side, bottomPx + 80)
            runCatching {
                if (animate) map.animateCamera(update, 900) else map.moveCamera(update)
            }
        }
    }
}
