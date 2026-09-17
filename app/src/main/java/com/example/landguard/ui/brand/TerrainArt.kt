package com.example.landguard.ui.brand

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/*
 * Procedural LandGuard terrain illustrations.
 *
 * Everything is vector-drawn from a few deterministic functions, so it
 * stays crisp on any screen size, ships no bitmap assets, and costs a
 * handful of paths per frame. Static layers are cached per size; only
 * the thin contour / glow layers animate.
 */

// ─────────────────────────────────────────────────────────────
// Scenic mountain landscape
// ─────────────────────────────────────────────────────────────

/**
 * Layered ridgelines at golden hour with mist, forest and a river.
 *
 * @param sunX / sunY  sun position as a fraction of the canvas.
 * @param horizon      height of the far ridgeline as a fraction of the canvas.
 */
@Composable
fun ScenicTerrain(
    modifier: Modifier = Modifier,
    sunX: Float = 0.74f,
    sunY: Float = 0.40f,
    horizon: Float = 0.44f,
    seed: Float = 0f,
    showRiver: Boolean = true,
    animateContours: Boolean = true
) {
    Box(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height

            val sky = Brush.verticalGradient(
                0f to BrandColors.ForestNight,
                horizon * 0.55f to Color(0xFF1A2E24),
                horizon to Color(0xFF6B4A2E),
                (horizon + 0.08f).coerceAtMost(1f) to Color(0xFF2A3A2C),
                1f to BrandColors.ForestNight
            )
            val sunCenter = Offset(w * sunX, h * sunY)
            val sunGlow = Brush.radialGradient(
                listOf(BrandColors.Ember.copy(alpha = 0.55f), BrandColors.Ember.copy(alpha = 0.12f), Color.Transparent),
                center = sunCenter,
                radius = w * 0.55f
            )
            val sunDisc = Brush.radialGradient(
                listOf(Color(0xFFFFE2A8), Color(0xFFFFB25C), BrandColors.Ember.copy(alpha = 0f)),
                center = sunCenter,
                radius = w * 0.045f
            )

            data class Layer(val path: Path, val brush: Brush)

            val layers = listOf(
                // far → near
                Layer(
                    ridge(w, h, base = horizon + 0.02f, amp = 0.10f, freq = 2.1f, seed = seed + 0.3f),
                    Brush.verticalGradient(listOf(Color(0xFF3E5446), Color(0xFF2A3E32)), startY = h * (horizon - 0.1f), endY = h)
                ),
                Layer(
                    ridge(w, h, base = horizon + 0.10f, amp = 0.12f, freq = 1.6f, seed = seed + 1.7f),
                    Brush.verticalGradient(listOf(Color(0xFF26402F), Color(0xFF18301F)), startY = h * horizon, endY = h)
                ),
                Layer(
                    ridge(w, h, base = horizon + 0.20f, amp = 0.10f, freq = 2.6f, seed = seed + 3.1f),
                    Brush.verticalGradient(listOf(Color(0xFF1A3524), Color(0xFF10251A)), startY = h * (horizon + 0.1f), endY = h)
                ),
                Layer(
                    ridge(w, h, base = horizon + 0.32f, amp = 0.07f, freq = 3.4f, seed = seed + 4.9f),
                    Brush.verticalGradient(listOf(Color(0xFF123020), BrandColors.ForestNight), startY = h * (horizon + 0.25f), endY = h)
                )
            )

            val mist = listOf(horizon + 0.06f, horizon + 0.16f, horizon + 0.27f).map { y ->
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x1FE8F0EA), Color.Transparent),
                    startY = h * (y - 0.05f),
                    endY = h * (y + 0.05f)
                ) to y
            }

            val trees = treeLine(w, h, base = horizon + 0.32f, amp = 0.07f, freq = 3.4f, seed = seed + 4.9f)
            val river = if (showRiver) riverPath(w, h, horizon) else null
            val riverBrush = Brush.verticalGradient(
                listOf(Color(0xFFD9B98C).copy(alpha = 0.55f), Color(0xFF8FA99A).copy(alpha = 0.45f), Color(0xFF2B4538).copy(alpha = 0.6f)),
                startY = h * (horizon + 0.12f),
                endY = h
            )
            val vignette = Brush.verticalGradient(
                0.0f to Color.Transparent,
                0.82f to Color.Transparent,
                1f to BrandColors.ForestNight
            )

            onDrawBehind {
                drawRect(sky)
                drawRect(sunGlow)
                drawCircle(sunDisc, radius = w * 0.045f, center = sunCenter)
                layers.forEachIndexed { index, layer ->
                    drawPath(layer.path, layer.brush)
                    if (index < mist.size) {
                        val (brush, y) = mist[index]
                        drawRect(brush, topLeft = Offset(0f, h * (y - 0.05f)), size = Size(w, h * 0.10f))
                    }
                }
                river?.let { drawPath(it, riverBrush) }
                drawPath(trees, Color(0xFF0A1D12))
                drawRect(vignette)
            }
        }
    ) {
        if (animateContours) {
            TopoContours(
                modifier = Modifier.fillMaxSize(),
                centerX = 0.3f,
                centerY = 0.9f,
                color = BrandColors.Leaf.copy(alpha = 0.10f)
            )
        }
    }
}

/** Ridgeline silhouette filled down to the bottom edge. */
private fun ridge(w: Float, h: Float, base: Float, amp: Float, freq: Float, seed: Float): Path {
    val path = Path()
    val steps = 64
    path.moveTo(0f, h)
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        path.lineTo(w * t, h * ridgeY(t, base, amp, freq, seed))
    }
    path.lineTo(w, h)
    path.close()
    return path
}

private fun ridgeY(t: Float, base: Float, amp: Float, freq: Float, seed: Float): Float {
    val x = t * PI.toFloat() * 2f
    val n = 0.55f * sin(x * freq + seed) +
            0.30f * sin(x * freq * 2.3f + seed * 1.9f) +
            0.15f * sin(x * freq * 5.1f + seed * 0.7f)
    // A sharper peak gives the range a mountain character.
    val peakCenter = ((seed * 0.37f) % 1f + 1f) % 1f
    val peak = exp(-((t - peakCenter) * (t - peakCenter)) / 0.025f)
    return base - amp * (0.5f * n + 0.65f * peak)
}

/** Small conifer silhouettes along the nearest ridge. */
private fun treeLine(w: Float, h: Float, base: Float, amp: Float, freq: Float, seed: Float): Path {
    val path = Path()
    val count = 46
    for (i in 0 until count) {
        val t = (i + 0.5f) / count
        val jitter = sin(i * 12.9898f + seed) * 0.5f + 0.5f
        val x = w * t
        val y = h * ridgeY(t, base, amp, freq, seed) + h * 0.006f
        val treeH = h * (0.018f + 0.022f * jitter)
        val treeW = treeH * 0.42f
        path.moveTo(x, y - treeH)
        path.lineTo(x + treeW, y + 2f)
        path.lineTo(x - treeW, y + 2f)
        path.close()
    }
    return path
}

/** A river winding from the foreground towards the horizon, tapering with distance. */
private fun riverPath(w: Float, h: Float, horizon: Float): Path {
    val steps = 40
    val left = ArrayList<Offset>(steps + 1)
    val right = ArrayList<Offset>(steps + 1)
    for (i in 0..steps) {
        val t = i / steps.toFloat() // 0 = near, 1 = far
        val y = h * (1.02f - t * (1.02f - (horizon + 0.2f)))
        val cx = w * (0.42f + 0.22f * sin(t * PI.toFloat() * 2.2f) * (1f - t * 0.5f) + 0.12f * t)
        val half = w * (0.16f * (1f - t) * (1f - t) + 0.006f)
        left.add(Offset(cx - half, y))
        right.add(Offset(cx + half, y))
    }
    return Path().apply {
        moveTo(left.first().x, left.first().y)
        left.drop(1).forEach { lineTo(it.x, it.y) }
        right.asReversed().forEach { lineTo(it.x, it.y) }
        close()
    }
}

// ─────────────────────────────────────────────────────────────
// Animated topographic contours
// ─────────────────────────────────────────────────────────────

@Composable
fun TopoContours(
    modifier: Modifier = Modifier,
    centerX: Float = 0.5f,
    centerY: Float = 0.5f,
    rings: Int = 9,
    color: Color = BrandColors.Leaf.copy(alpha = 0.12f),
    periodMillis: Int = 14000
) {
    val transition = rememberInfiniteTransition(label = "contours")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
        label = "contourPhase"
    )
    Canvas(modifier) {
        drawContours(phase, centerX, centerY, rings, color)
    }
}

private fun DrawScope.drawContours(phase: Float, cxFrac: Float, cyFrac: Float, rings: Int, color: Color) {
    val center = Offset(size.width * cxFrac, size.height * cyFrac)
    val base = size.minDimension * 0.10f
    val stroke = Stroke(width = 1.1f * density)
    val points = 56
    for (ring in 1..rings) {
        val r0 = base * ring
        val path = Path()
        for (i in 0..points) {
            val a = i / points.toFloat() * 2f * PI.toFloat()
            val wobble = 1f +
                    0.10f * sin(3f * a + phase + ring * 0.6f) +
                    0.05f * sin(5f * a - phase * 0.7f + ring)
            val x = center.x + cos(a) * r0 * wobble * 1.35f
            val y = center.y + sin(a) * r0 * wobble * 0.75f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color.copy(alpha = color.alpha * (1f - ring / (rings + 2f))), style = stroke)
    }
}

// ─────────────────────────────────────────────────────────────
// Hazard terrain (wireframe relief with glowing risk zones)
// ─────────────────────────────────────────────────────────────

/** Fractional positions of the hotspots, shared with marker overlays. */
object HazardSpots {
    val High = Offset(0.30f, 0.46f)
    val Watch = Offset(0.52f, 0.62f)
    val Safe = Offset(0.66f, 0.82f)
}

@Composable
fun HazardTerrain(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hazard")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "hazardPulse"
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "hazardDrift"
    )

    Box(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val bg = Brush.verticalGradient(
                0f to BrandColors.ForestNight,
                0.35f to Color(0xFF12301F),
                1f to BrandColors.ForestNight
            )
            // Distant ridges for depth.
            val farRidge = ridge(w, h, base = 0.36f, amp = 0.10f, freq = 1.8f, seed = 2.2f)
            val farBrush = Brush.verticalGradient(listOf(Color(0xFF1C3A28), Color(0xFF0E2418)), startY = h * 0.2f, endY = h * 0.7f)
            val grid = wireframe(w, h)
            onDrawBehind {
                drawRect(bg)
                drawPath(farRidge, farBrush)
                grid.forEachIndexed { index, path ->
                    val depth = index / grid.size.toFloat()
                    drawPath(
                        path,
                        BrandColors.Leaf.copy(alpha = 0.10f + 0.28f * depth),
                        style = Stroke(width = (0.8f + 1.0f * depth) * density)
                    )
                }
            }
        }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawHotspot(Offset(w * HazardSpots.High.x, h * HazardSpots.High.y), BrandColors.Hazard, pulse, w * 0.26f, drift)
            drawHotspot(Offset(w * HazardSpots.Watch.x, h * HazardSpots.Watch.y), BrandColors.Watch, (pulse + 0.35f) % 1f, w * 0.20f, drift + 1f)
            drawHotspot(Offset(w * HazardSpots.Safe.x, h * HazardSpots.Safe.y), BrandColors.Leaf, (pulse + 0.7f) % 1f, w * 0.14f, drift + 2f)

            // Flow line connecting risk zones downhill.
            val flow = Path().apply {
                moveTo(w * HazardSpots.High.x, h * HazardSpots.High.y)
                cubicTo(w * 0.44f, h * 0.50f, w * 0.40f, h * 0.60f, w * HazardSpots.Watch.x, h * HazardSpots.Watch.y)
                cubicTo(w * 0.62f, h * 0.66f, w * 0.56f, h * 0.78f, w * HazardSpots.Safe.x, h * HazardSpots.Safe.y)
            }
            drawPath(
                flow,
                Brush.verticalGradient(
                    listOf(BrandColors.Hazard.copy(alpha = 0.8f), BrandColors.Watch.copy(alpha = 0.7f), BrandColors.Leaf.copy(alpha = 0.6f)),
                    startY = h * HazardSpots.High.y,
                    endY = h * HazardSpots.Safe.y
                ),
                style = Stroke(width = 2f * density, cap = StrokeCap.Round)
            )
        }
    }
}

/** Perspective wireframe of a relief surface (rows get denser towards the horizon). */
private fun wireframe(w: Float, h: Float): List<Path> {
    val rows = 26
    val cols = 48
    return (0 until rows).map { r ->
        val depth = r / (rows - 1f)                 // 0 far → 1 near
        val yBase = h * (0.38f + 0.62f * depth * depth * 0.55f + 0.28f * depth)
        val spread = 0.55f + 0.75f * depth          // near rows wider than the screen
        Path().apply {
            for (c in 0..cols) {
                val u = c / cols.toFloat()
                val x = w * (0.5f + (u - 0.5f) * spread * 1.6f)
                val relief =
                    0.075f * exp(-((u - 0.30f) * (u - 0.30f) + (depth - 0.35f) * (depth - 0.35f)) / 0.02f) +
                    0.055f * exp(-((u - 0.55f) * (u - 0.55f) + (depth - 0.6f) * (depth - 0.6f)) / 0.025f) +
                    0.02f * sin(u * 14f + depth * 6f) * (0.4f + depth)
                val y = yBase - h * relief * (0.6f + depth)
                if (c == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
    }
}

private fun DrawScope.drawHotspot(center: Offset, color: Color, pulse: Float, radius: Float, drift: Float) {
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.12f), Color.Transparent), center, radius),
        radius = radius,
        center = center
    )
    // Glowing contour rings squeezed into perspective.
    val stroke = Stroke(width = 1.4f * density)
    for (ring in 1..4) {
        val r = radius * 0.22f * ring
        val path = Path()
        val points = 40
        for (i in 0..points) {
            val a = i / points.toFloat() * 2f * PI.toFloat()
            val wobble = 1f + 0.12f * sin(3f * a + drift + ring)
            val x = center.x + cos(a) * r * wobble
            val y = center.y + sin(a) * r * wobble * 0.5f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color.copy(alpha = 0.55f - ring * 0.1f), style = stroke)
    }
    // Expanding pulse ring.
    drawCircle(
        color = color.copy(alpha = 0.5f * (1f - pulse)),
        radius = radius * (0.2f + 0.8f * pulse),
        center = center,
        style = Stroke(width = 2f * density)
    )
}
