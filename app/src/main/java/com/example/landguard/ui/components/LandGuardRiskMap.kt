package com.example.landguard.ui.components

import android.graphics.PointF
import android.view.Gravity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.withFrameMillis
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
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.coroutines.resume
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * LandGuard risk map.
 *
 * A map-first MapLibre view (same engine as the full analysis map in
 * MapScreen) used by Explore and Risk Areas.
 *
 *  • Apple-Maps-like base: soft warm land, clear blue water, white roads,
 *    3D buildings, and shaded world relief at low zoom.
 *  • Cinematic intro (once per app session): the world rolls past, then
 *    the camera flies in and tilts down onto the user like Google Earth.
 *  • Alerted areas are drawn as glowing risk zones: a vibrant hexagonal
 *    boundary with a soft neon glow, radar ripples on the ground, and a
 *    3D risk pillar whose height follows the risk score.
 */

enum class RiskMapStyle(val label: String) {
    STREETS("Map"),
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

/** The cinematic intro plays once per app session, not on every tab visit. */
object MapIntro {
    @Volatile
    var played: Boolean = false
        internal set
}

private const val DEFAULT_LAT = 20.5937
private const val DEFAULT_LNG = 78.9629

private const val ZONE_RADIUS_M = 2400.0
private const val PILLAR_RADIUS_M = 420.0
private const val METERS_PER_SCORE = 32.0
private const val TILT_3D = 55.0
private const val EARTH_METERS_PER_PX_Z0 = 78271.517 // 512px tiles

private const val SOURCE_ZONES = "lg-zones"
private const val SOURCE_PILLARS = "lg-pillars"
private const val SOURCE_POINTS = "lg-points"
private const val SOURCE_USER = "lg-user"

private const val LAYER_ZONE_FILL = "lg-zone-fill"
private const val LAYER_ZONE_GLOW = "lg-zone-glow"
private const val LAYER_ZONE_LINE = "lg-zone-line"
private const val LAYER_SELECTED = "lg-zone-selected"
private const val LAYER_RIPPLE_A = "lg-ripple-a"
private const val LAYER_RIPPLE_B = "lg-ripple-b"
private const val LAYER_PILLAR = "lg-pillar"
private const val LAYER_SCORE = "lg-score"
private const val LAYER_NAME = "lg-name"
private const val LAYER_USER_PULSE = "lg-user-pulse"
private const val LAYER_USER_HALO = "lg-user-halo"
private const val LAYER_USER = "lg-user-dot"

private const val GLYPHS = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"

private val TOPO_STYLE_JSON = """
{
  "version": 8,
  "glyphs": "$GLYPHS",
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
  "glyphs": "$GLYPHS",
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
    { "id": "sat", "type": "raster", "source": "sat", "paint": { "raster-contrast": 0.08, "raster-saturation": 0.2 } }
  ]
}
""".trimIndent()

/** OpenFreeMap "liberty": vector streets, 3D buildings and shaded world relief. */
private const val STREETS_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val FALLBACK_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"

private fun styleBuilder(style: RiskMapStyle): Style.Builder = when (style) {
    RiskMapStyle.STREETS -> Style.Builder().fromUri(STREETS_STYLE_URL)
    RiskMapStyle.TERRAIN -> Style.Builder().fromJson(TOPO_STYLE_JSON)
    RiskMapStyle.SATELLITE -> Style.Builder().fromJson(SATELLITE_STYLE_JSON)
}

/** Vibrant map colours per severity (hot pink-red, amber, sunflower, mint). */
fun severityHex(severity: Severity): String = when (severity) {
    Severity.CRITICAL -> "#FF2D55"
    Severity.HIGH -> "#FF8A00"
    Severity.MODERATE -> "#FFD60A"
    Severity.LOW -> "#30D158"
}

/** Deeper companion colour used for the glow halo. */
private fun severityGlowHex(severity: Severity): String = when (severity) {
    Severity.CRITICAL -> "#BF5AF2"
    Severity.HIGH -> "#FF375F"
    Severity.MODERATE -> "#FF9F0A"
    Severity.LOW -> "#00C7BE"
}

private const val USER_BLUE = "#0A84FF"

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
    bottomInset: Dp = 0.dp,
    threeD: Boolean = true,
    cinematicIntro: Boolean = false,
    onIntroFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentAreas by rememberUpdatedState(areas)
    val currentUser by rememberUpdatedState(userLocation)
    val currentThreeD by rememberUpdatedState(threeD)
    val currentOnAreaClick by rememberUpdatedState(onAreaClick)
    val currentOnBackgroundClick by rememberUpdatedState(onMapBackgroundClick)
    val currentOnIntroFinished by rememberUpdatedState(onIntroFinished)

    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var styleGeneration by remember { mutableIntStateOf(0) }
    var hasPlacedCamera by remember { mutableStateOf(false) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }
    val introPending = cinematicIntro && !MapIntro.played
    var introActive by remember { mutableStateOf(introPending) }
    var userTookOver by remember { mutableStateOf(false) }
    val pillarGrowth = remember { Animatable(0f) }

    val tapSlopPx = with(density) { 28.dp.toPx() }

    val mapView = remember {
        MapLibre.getInstance(context)
        val options = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true)
            .camera(
                CameraPosition.Builder()
                    .target(LatLng(if (introPending) 18.0 else DEFAULT_LAT, DEFAULT_LNG))
                    .zoom(if (introPending) 1.2 else 4.0)
                    .build()
            )
        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.uiSettings.apply {
                    isRotateGesturesEnabled = true
                    isTiltGesturesEnabled = true
                    // Floating controls own the top-right corner; 2D mode resets north.
                    isCompassEnabled = false
                    isLogoEnabled = false
                    attributionGravity = Gravity.TOP or Gravity.START
                    setAttributionTintColor(android.graphics.Color.parseColor("#6B7280"))
                }
                map.addOnMapClickListener { latLng ->
                    val area = findTappedArea(map, map.projection.toScreenLocation(latLng), currentAreas, tapSlopPx)
                    if (area != null) currentOnAreaClick(area) else currentOnBackgroundClick()
                    true
                }
                // Any manual pan ends the intro immediately — the user is in control.
                map.addOnMoveListener(object : MapLibreMap.OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {
                        userTookOver = true
                    }
                    override fun onMove(detector: MoveGestureDetector) = Unit
                    override fun onMoveEnd(detector: MoveGestureDetector) = Unit
                })
            }
            var fallbackAttempted = false
            addOnDidFailLoadingMapListener {
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
                if (mapStyle == RiskMapStyle.STREETS) applyAppleLook(style)
                installLayers(style)
                loadedStyle = style
                styleGeneration++
            }
        }
    }

    // ── Insets for attribution ─────────────────────────────────────────
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

    // ── Cinematic intro: roll the world, then fly down onto the user ─────
    LaunchedEffect(introPending) {
        if (!introPending) return@LaunchedEffect
        val map = mapView.awaitMap()

        val minRollMs = 2400L
        val maxWaitMs = 7000L
        val startLng = (currentUser?.longitude ?: DEFAULT_LNG) - 150.0
        var startTime = -1L
        var lastFrame = -1L
        var lng = startLng

        while (!userTookOver) {
            val now = withFrameMillis { it }
            if (startTime < 0) {
                startTime = now
                lastFrame = now
            }
            val elapsed = now - startTime
            val dtSec = (now - lastFrame).coerceIn(0L, 100L) / 1000.0
            lastFrame = now
            // Ease in to a steady spin, like a globe gaining momentum.
            val speedDegPerSec = 70.0 * (elapsed / 700.0).coerceAtMost(1.0)
            lng += speedDegPerSec * dtSec
            map.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(18.0 + 6.0 * sin(elapsed / 1800.0), wrapLng(lng)))
                        .zoom(1.2)
                        .tilt(0.0)
                        .bearing(0.0)
                        .build()
                )
            )
            val located = currentUser != null
            if (elapsed >= minRollMs && (located || elapsed >= maxWaitMs)) break
        }

        if (!userTookOver) {
            val user = currentUser
            val target: CameraPosition? = when {
                user != null -> CameraPosition.Builder()
                    .target(LatLng(user.latitude, user.longitude))
                    .zoom(12.2)
                    .tilt(if (currentThreeD) TILT_3D else 0.0)
                    .bearing(-18.0)
                    .build()
                currentAreas.isNotEmpty() -> {
                    val c = centroid(currentAreas)
                    CameraPosition.Builder()
                        .target(c)
                        .zoom(9.2)
                        .tilt(if (currentThreeD) 45.0 else 0.0)
                        .bearing(-12.0)
                        .build()
                }
                else -> null
            }
            if (target != null) {
                // animateCamera performs a flyTo: zoom out, travel, then dive in.
                map.awaitCameraAnimation(CameraUpdateFactory.newCameraPosition(target), 4200)
            }
        }

        MapIntro.played = true
        introActive = false
        hasPlacedCamera = true
        hasCenteredOnUser = currentUser != null
        pillarGrowth.snapTo(0f)
        pillarGrowth.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
        currentOnIntroFinished()
    }

    // ── Default camera when there is no intro ────────────────────────────
    LaunchedEffect(userLocation, introActive) {
        val user = userLocation ?: return@LaunchedEffect
        if (introActive || hasCenteredOnUser) return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(user.latitude, user.longitude))
                        .zoom(11.0)
                        .tilt(if (currentThreeD) 45.0 else 0.0)
                        .bearing(map.cameraPosition.bearing)
                        .build()
                ),
                1600
            )
        }
        hasCenteredOnUser = true
        hasPlacedCamera = true
    }

    LaunchedEffect(areas.isNotEmpty(), introActive) {
        if (introActive || hasPlacedCamera || userLocation != null || areas.isEmpty()) return@LaunchedEffect
        mapView.getMapAsync { map ->
            if (!hasPlacedCamera) {
                fitCamera(map, areas, null, density.run { topInset.roundToPx() }, density.run { bottomInset.roundToPx() }, animate = false)
                hasPlacedCamera = true
            }
        }
    }

    // Pillars rise whenever a map (re)loads outside the intro.
    LaunchedEffect(styleGeneration) {
        if (styleGeneration == 0 || introActive) return@LaunchedEffect
        pillarGrowth.snapTo(0f)
        pillarGrowth.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }

    // ── Explicit focus / fit requests (cinematic fly-to) ─────────────────
    LaunchedEffect(focus) {
        val target = focus ?: return@LaunchedEffect
        userTookOver = true
        mapView.getMapAsync { map ->
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(target.latitude, target.longitude))
                        .zoom(target.zoom + if (currentThreeD) 0.4 else 0.0)
                        .tilt(if (currentThreeD) TILT_3D else 0.0)
                        .bearing(map.cameraPosition.bearing - 12.0)
                        .build()
                ),
                1800
            )
            hasPlacedCamera = true
        }
    }

    LaunchedEffect(fitAll) {
        if (fitAll == null) return@LaunchedEffect
        userTookOver = true
        mapView.getMapAsync { map ->
            fitCamera(map, areas, userLocation, density.run { topInset.roundToPx() }, density.run { bottomInset.roundToPx() }, animate = true)
            hasPlacedCamera = true
        }
    }

    // ── 2D / 3D toggle ───────────────────────────────────────────────────
    LaunchedEffect(threeD) {
        if (introActive) return@LaunchedEffect
        mapView.getMapAsync { map ->
            if (!hasPlacedCamera) return@getMapAsync
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder(map.cameraPosition)
                        .tilt(if (threeD) TILT_3D else 0.0)
                        .bearing(if (threeD) map.cameraPosition.bearing else 0.0)
                        .build()
                ),
                900
            )
        }
    }

    // ── Pillar growth ────────────────────────────────────────────────────
    LaunchedEffect(styleGeneration) {
        val style = loadedStyle ?: return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow { pillarGrowth.value }.collect { g ->
            runCatching {
                style.getLayer(LAYER_PILLAR)?.setProperties(
                    PropertyFactory.fillExtrusionHeight(
                        Expression.product(Expression.get("height"), Expression.literal(g))
                    )
                )
            }
        }
    }

    // ── Living layers: radar ripples, neon breathing, user pulse ─────────
    LaunchedEffect(styleGeneration, lifecycleOwner) {
        val style = loadedStyle ?: return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val now = System.currentTimeMillis()
                val p = (now % 2600L) / 2600f
                val q = (p + 0.5f) % 1f
                val breathe = (sin(now / 700.0).toFloat() + 1f) / 2f
                val userP = (now % 2000L) / 2000f
                runCatching {
                    style.getLayer(LAYER_RIPPLE_A)?.setProperties(
                        PropertyFactory.circleRadius(metersRadius(700.0 + 3200.0 * p)),
                        PropertyFactory.circleStrokeOpacity(0.9f * (1f - p))
                    )
                    style.getLayer(LAYER_RIPPLE_B)?.setProperties(
                        PropertyFactory.circleRadius(metersRadius(700.0 + 3200.0 * q)),
                        PropertyFactory.circleStrokeOpacity(0.9f * (1f - q))
                    )
                    style.getLayer(LAYER_ZONE_GLOW)?.setProperties(
                        PropertyFactory.lineOpacity(0.35f + 0.4f * breathe)
                    )
                    style.getLayer(LAYER_USER_PULSE)?.setProperties(
                        PropertyFactory.circleRadius(9f + 26f * userP),
                        PropertyFactory.circleOpacity(0.35f * (1f - userP))
                    )
                }
                delay(33)
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

// ─────────────────────────────────────────────────────────────
// Camera helpers
// ─────────────────────────────────────────────────────────────

private suspend fun MapView.awaitMap(): MapLibreMap = suspendCancellableCoroutine { cont ->
    getMapAsync { if (cont.isActive) cont.resume(it) }
}

private suspend fun MapLibreMap.awaitCameraAnimation(
    update: org.maplibre.android.camera.CameraUpdate,
    durationMs: Int
) = suspendCancellableCoroutine { cont ->
    animateCamera(update, durationMs, object : MapLibreMap.CancelableCallback {
        override fun onCancel() { if (cont.isActive) cont.resume(Unit) }
        override fun onFinish() { if (cont.isActive) cont.resume(Unit) }
    })
    cont.invokeOnCancellation { runCatching { cancelTransitions() } }
}

private fun wrapLng(lng: Double): Double {
    var v = (lng + 180.0) % 360.0
    if (v < 0) v += 360.0
    return v - 180.0
}

private fun centroid(areas: List<RiskArea>): LatLng =
    LatLng(areas.map { it.latitude }.average(), areas.map { it.longitude }.average())

/**
 * Circle radius in screen pixels that represents [meters] on the ground at every zoom.
 * Uses each feature's `px0` (pixels per metre at zoom 0, latitude-corrected).
 */
private fun metersRadius(meters: Double): Expression =
    Expression.interpolate(
        Expression.exponential(2f),
        Expression.zoom(),
        Expression.stop(0, Expression.product(Expression.get("px0"), Expression.literal(meters))),
        Expression.stop(24, Expression.product(Expression.get("px0"), Expression.literal(meters * 16777216.0)))
    )

// ─────────────────────────────────────────────────────────────
// Hit testing
// ─────────────────────────────────────────────────────────────

private fun findTappedArea(
    map: MapLibreMap,
    screenPoint: PointF,
    areas: List<RiskArea>,
    slopPx: Float
): RiskArea? {
    val hitId = map.queryRenderedFeatures(screenPoint, LAYER_PILLAR, LAYER_ZONE_FILL)
        .firstNotNullOfOrNull { it.getStringProperty("id") }
    hitId?.let { id -> areas.firstOrNull { it.id == id }?.let { return it } }

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
// Apple-Maps-like base styling (runtime tweaks to OpenFreeMap liberty)
// ─────────────────────────────────────────────────────────────

private fun applyAppleLook(style: Style) {
    val land = "#F2EFE9"
    val water = "#9FD3F5"
    val park = "#CDE8BD"
    val roadWhite = "#FFFFFF"
    val casing = "#DAD6CF"
    val highway = "#FFCF5C"
    val building = "#E6E2DB"

    style.layers.forEach { layer ->
        val id = layer.id
        runCatching {
            when {
                layer is BackgroundLayer -> layer.setProperties(PropertyFactory.backgroundColor(land))
                id == "water" && layer is FillLayer -> layer.setProperties(PropertyFactory.fillColor(water))
                id.startsWith("waterway") && layer is LineLayer -> layer.setProperties(PropertyFactory.lineColor(water))
                id == "park" && layer is FillLayer -> layer.setProperties(PropertyFactory.fillColor(park))
                id == "park_outline" && layer is LineLayer -> layer.setProperties(PropertyFactory.lineOpacity(0f))
                id == "landcover_grass" && layer is FillLayer -> layer.setProperties(PropertyFactory.fillColor(park))
                id == "landcover_wood" && layer is FillLayer -> layer.setProperties(
                    PropertyFactory.fillColor("#B9DEA3"),
                    PropertyFactory.fillOpacity(0.55f)
                )
                id == "building" && layer is FillLayer -> layer.setProperties(PropertyFactory.fillColor(building))
                layer is FillExtrusionLayer && id == "building-3d" -> layer.setProperties(
                    PropertyFactory.fillExtrusionColor("#EDEAE4"),
                    PropertyFactory.fillExtrusionOpacity(0.9f)
                )
                layer is LineLayer && (id.startsWith("road_") || id.startsWith("bridge_") || id.startsWith("tunnel_")) -> when {
                    id.endsWith("_casing") -> layer.setProperties(PropertyFactory.lineColor(casing))
                    "rail" in id -> Unit
                    "motorway" in id -> layer.setProperties(PropertyFactory.lineColor(highway))
                    "path" in id -> Unit
                    else -> layer.setProperties(PropertyFactory.lineColor(roadWhite))
                }
                id.startsWith("boundary") && layer is LineLayer -> layer.setProperties(PropertyFactory.lineColor("#B8B2C8"))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Risk layers
// ─────────────────────────────────────────────────────────────

private fun installLayers(style: Style) {
    listOf(SOURCE_ZONES, SOURCE_PILLARS, SOURCE_POINTS, SOURCE_USER).forEach { id ->
        if (style.getSource(id) == null) style.addSource(GeoJsonSource(id))
    }

    fun add(layer: org.maplibre.android.style.layers.Layer) {
        if (style.getLayer(layer.id) == null) style.addLayer(layer)
    }

    // Soft tinted zone on the ground
    add(
        FillLayer(LAYER_ZONE_FILL, SOURCE_ZONES).withProperties(
            PropertyFactory.fillColor(Expression.get("color")),
            PropertyFactory.fillOpacity(0.20f)
        )
    )
    // Radar ripples lying flat on the terrain
    listOf(LAYER_RIPPLE_A, LAYER_RIPPLE_B).forEach { id ->
        add(
            CircleLayer(id, SOURCE_POINTS).withProperties(
                PropertyFactory.circleColor("rgba(0,0,0,0)"),
                PropertyFactory.circleRadius(metersRadius(700.0)),
                PropertyFactory.circleStrokeColor(Expression.get("color")),
                PropertyFactory.circleStrokeWidth(2.5f),
                PropertyFactory.circleStrokeOpacity(0f),
                PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP),
                PropertyFactory.circlePitchScale(Property.CIRCLE_PITCH_SCALE_MAP)
            ).withFilter(Expression.gte(Expression.get("rank"), Expression.literal(Severity.MODERATE.ordinal)))
        )
    }
    // Neon glow around the boundary
    add(
        LineLayer(LAYER_ZONE_GLOW, SOURCE_ZONES).withProperties(
            PropertyFactory.lineColor(Expression.get("glow")),
            PropertyFactory.lineWidth(12f),
            PropertyFactory.lineBlur(9f),
            PropertyFactory.lineOpacity(0.55f),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    // Crisp vibrant boundary
    add(
        LineLayer(LAYER_ZONE_LINE, SOURCE_ZONES).withProperties(
            PropertyFactory.lineColor(Expression.get("color")),
            PropertyFactory.lineWidth(2.8f),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    // Selected zone: bright white double outline
    add(
        LineLayer(LAYER_SELECTED, SOURCE_ZONES).withProperties(
            PropertyFactory.lineColor("#FFFFFF"),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineGapWidth(3f),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        ).withFilter(Expression.eq(Expression.get("selected"), Expression.literal(true)))
    )
    // 3D risk pillars (height follows the risk score)
    add(
        FillExtrusionLayer(LAYER_PILLAR, SOURCE_PILLARS).withProperties(
            PropertyFactory.fillExtrusionColor(Expression.get("color")),
            PropertyFactory.fillExtrusionHeight(Expression.literal(0f)),
            PropertyFactory.fillExtrusionBase(0f),
            PropertyFactory.fillExtrusionOpacity(0.92f),
            PropertyFactory.fillExtrusionVerticalGradient(true)
        )
    )
    // Score badge
    add(
        SymbolLayer(LAYER_SCORE, SOURCE_POINTS).withProperties(
            PropertyFactory.textField(Expression.get("score")),
            PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
            PropertyFactory.textSize(13f),
            PropertyFactory.textColor("#FFFFFF"),
            PropertyFactory.textHaloColor(Expression.get("color")),
            PropertyFactory.textHaloWidth(3f),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
            PropertyFactory.textOffset(arrayOf(0f, -0.6f)),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true)
        )
    )
    // Area name
    add(
        SymbolLayer(LAYER_NAME, SOURCE_POINTS).withProperties(
            PropertyFactory.textField(Expression.get("name")),
            PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
            PropertyFactory.textSize(11.5f),
            PropertyFactory.textColor("#1C1C1E"),
            PropertyFactory.textHaloColor("#FFFFFF"),
            PropertyFactory.textHaloWidth(1.8f),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
            PropertyFactory.textOffset(arrayOf(0f, 1.1f)),
            PropertyFactory.textMaxWidth(9f),
            PropertyFactory.textOptional(true)
        ).apply { minZoom = 8.5f }
    )
    // User position — Apple-style blue dot with a breathing pulse
    add(
        CircleLayer(LAYER_USER_PULSE, SOURCE_USER).withProperties(
            PropertyFactory.circleColor(USER_BLUE),
            PropertyFactory.circleRadius(9f),
            PropertyFactory.circleOpacity(0.3f),
            PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP)
        )
    )
    add(
        CircleLayer(LAYER_USER_HALO, SOURCE_USER).withProperties(
            PropertyFactory.circleColor("#FFFFFF"),
            PropertyFactory.circleRadius(11f),
            PropertyFactory.circleBlur(0.35f),
            PropertyFactory.circleOpacity(0.95f)
        )
    )
    add(
        CircleLayer(LAYER_USER, SOURCE_USER).withProperties(
            PropertyFactory.circleColor(USER_BLUE),
            PropertyFactory.circleRadius(7.5f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2.5f)
        )
    )
}

private fun updateAreas(style: Style, areas: List<RiskArea>, selectedId: String?) {
    fun props(feature: Feature, area: RiskArea) = feature.apply {
        addStringProperty("id", area.id)
        addStringProperty("color", severityHex(area.severity))
        addStringProperty("glow", severityGlowHex(area.severity))
        addNumberProperty("rank", area.severity.ordinal)
        addBooleanProperty("selected", area.id == selectedId)
    }

    val zones = areas.map { area ->
        props(Feature.fromGeometry(ringPolygon(area.latitude, area.longitude, ZONE_RADIUS_M, sides = 6, rotationDeg = 30.0)), area)
    }
    val pillars = areas.map { area ->
        props(Feature.fromGeometry(ringPolygon(area.latitude, area.longitude, PILLAR_RADIUS_M, sides = 6, rotationDeg = 30.0)), area).apply {
            val boost = if (area.id == selectedId) 1.25 else 1.0
            addNumberProperty("height", area.score * METERS_PER_SCORE * boost)
        }
    }
    val points = areas.map { area ->
        props(Feature.fromGeometry(Point.fromLngLat(area.longitude, area.latitude)), area).apply {
            val metersPerPxZ0 = EARTH_METERS_PER_PX_Z0 * cos(Math.toRadians(area.latitude))
            addNumberProperty("px0", 1.0 / metersPerPxZ0)
            addStringProperty("score", area.score.toString())
            addStringProperty("name", area.name)
        }
    }

    style.getSourceAs<GeoJsonSource>(SOURCE_ZONES)?.setGeoJson(FeatureCollection.fromFeatures(zones))
    style.getSourceAs<GeoJsonSource>(SOURCE_PILLARS)?.setGeoJson(FeatureCollection.fromFeatures(pillars))
    style.getSourceAs<GeoJsonSource>(SOURCE_POINTS)?.setGeoJson(FeatureCollection.fromFeatures(points))
}

private fun updateUser(style: Style, user: UserLocation?) {
    val features = listOfNotNull(
        user?.let { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) }
    )
    style.getSourceAs<GeoJsonSource>(SOURCE_USER)
        ?.setGeoJson(FeatureCollection.fromFeatures(features))
}

/** Geodesic regular polygon (hexagon for zones / pillars) around a centre. */
private fun ringPolygon(lat: Double, lng: Double, radiusMeters: Double, sides: Int, rotationDeg: Double): Polygon {
    val earthRadius = 6378137.0
    val d = radiusMeters / earthRadius
    val lat1 = Math.toRadians(lat)
    val lng1 = Math.toRadians(lng)
    val points = (0 until sides).map { i ->
        val tc = Math.toRadians(rotationDeg + 360.0 * i / sides)
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
            if (animate) map.animateCamera(update, 1400) else map.moveCamera(update)
        }
        else -> {
            val bounds = LatLngBounds.Builder().includes(points).build()
            val side = 120
            val update = CameraUpdateFactory.newLatLngBounds(bounds, side, topPx + 80, side, bottomPx + 80)
            runCatching {
                if (animate) map.animateCamera(update, 1400) else map.moveCamera(update)
            }
        }
    }
}
