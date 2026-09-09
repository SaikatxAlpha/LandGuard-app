package com.example.landguard.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.landguard.domain.model.GroundDeformationPoint
import com.example.landguard.domain.model.SatelliteLayer
import com.example.landguard.domain.model.SatelliteSource
import com.example.landguard.domain.model.Severity
import kotlin.math.hypot

@Composable
fun Alos4Sentinel2MapViewer(
    source: SatelliteSource,
    layer: SatelliteLayer,
    opacity: Float,
    points: List<GroundDeformationPoint>,
    selectedPoint: GroundDeformationPoint?,
    isSplitView: Boolean,
    onPointSelected: (GroundDeformationPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xFF0F1A17))) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures { tapOffset ->
                        val clicked = findNearestPoint(tapOffset, size.width.toFloat(), size.height.toFloat(), points)
                        if (clicked != null) {
                            onPointSelected(clicked)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw High-Clarity Terrain Elevation & Contour Background (ALOS World 3D DEM)
            drawTerrainContours(width, height)

            // 2. Draw Satellite Data Layer based on selected source & layer
            when (source) {
                SatelliteSource.ALOS4_PALSAR3 -> {
                    drawAlos4SarRadarLayer(width, height, layer, opacity)
                }
                SatelliteSource.SENTINEL2_MSI -> {
                    drawSentinel2OpticalLayer(width, height, layer, opacity)
                }
                SatelliteSource.HYBRID_FUSION -> {
                    drawSentinel2OpticalLayer(width, height, SatelliteLayer.SENTINEL2_TRUE_COLOR, opacity * 0.6f)
                    drawAlos4SarRadarLayer(width, height, SatelliteLayer.ALOS4_INSAR_DISPLACEMENT, opacity * 0.9f)
                }
            }

            // 3. Draw Split View Divider if Split View Mode is enabled
            if (isSplitView) {
                drawSplitViewDivider(width, height)
            }

            // 4. Draw InSAR Deformation Points & Risk Markers
            points.forEach { pt ->
                drawDeformationPointMarker(width, height, pt, pt == selectedPoint)
            }

            // 5. Compass Indicator
            drawCompassOverlay(width)
        }

        // Top Overlay Info Tag
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            color = Color(0xCC11221D),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (source == SatelliteSource.ALOS4_PALSAR3) Icons.Filled.Radar else Icons.Filled.Layers,
                    contentDescription = null,
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = source.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = layer.title + " (${(opacity * 100).toInt()}% Opacity)",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Bottom Map Legend
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            color = Color(0xDD0D1815),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (layer == SatelliteLayer.ALOS4_INSAR_DISPLACEMENT) "ALOS-4 Ground Shift Speed" else "Layer Risk Index",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendItem("Critical (<-25mm/y)", Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    LegendItem("Moderate (-15mm/y)", Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    LegendItem("Stable (-2mm/y)", Color(0xFF10B981))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Color(0xFFCBD5E1), fontSize = 9.sp)
    }
}

private fun DrawScope.drawTerrainContours(width: Float, height: Float) {
    val contourColor = Color(0xFF1E3A34)
    val gridColor = Color(0xFF132924)

    val cols = 10
    val rows = 8
    for (i in 0..cols) {
        val x = i * (width / cols)
        drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
    }
    for (j in 0..rows) {
        val y = j * (height / rows)
        drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
    }

    val ridgePath1 = Path().apply {
        moveTo(0f, height * 0.8f)
        cubicTo(width * 0.2f, height * 0.3f, width * 0.5f, height * 0.9f, width, height * 0.2f)
    }
    val ridgePath2 = Path().apply {
        moveTo(0f, height * 0.6f)
        cubicTo(width * 0.3f, height * 0.1f, width * 0.7f, height * 0.75f, width, height * 0.4f)
    }
    val ridgePath3 = Path().apply {
        moveTo(0f, height * 0.95f)
        cubicTo(width * 0.4f, height * 0.5f, width * 0.8f, height * 0.95f, width, height * 0.65f)
    }

    drawPath(ridgePath1, contourColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
    drawPath(ridgePath2, contourColor.copy(alpha = 0.7f), style = Stroke(width = 2f))
    drawPath(ridgePath3, contourColor.copy(alpha = 0.5f), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawAlos4SarRadarLayer(width: Float, height: Float, layer: SatelliteLayer, opacity: Float) {
    if (layer == SatelliteLayer.ALOS4_INSAR_DISPLACEMENT || layer == SatelliteLayer.ALOS4_SAR_BACKSCATTER) {
        val gradientRed = Brush.radialGradient(
            colors = listOf(Color(0xFFEF4444).copy(alpha = opacity * 0.75f), Color.Transparent),
            center = Offset(width * 0.68f, height * 0.32f),
            radius = width * 0.35f
        )
        drawCircle(
            brush = gradientRed,
            radius = width * 0.35f,
            center = Offset(width * 0.68f, height * 0.32f)
        )

        val gradientOrange = Brush.radialGradient(
            colors = listOf(Color(0xFFF59E0B).copy(alpha = opacity * 0.65f), Color.Transparent),
            center = Offset(width * 0.38f, height * 0.58f),
            radius = width * 0.28f
        )
        drawCircle(
            brush = gradientOrange,
            radius = width * 0.28f,
            center = Offset(width * 0.38f, height * 0.58f)
        )

        val gradientCritical = Brush.radialGradient(
            colors = listOf(Color(0xFFDC2626).copy(alpha = opacity * 0.85f), Color.Transparent),
            center = Offset(width * 0.76f, height * 0.68f),
            radius = width * 0.25f
        )
        drawCircle(
            brush = gradientCritical,
            radius = width * 0.25f,
            center = Offset(width * 0.76f, height * 0.68f)
        )

        for (i in 0..15) {
            val startX = (i * width / 15f)
            drawLine(
                color = Color(0xFF34D399).copy(alpha = opacity * 0.12f),
                start = Offset(startX, 0f),
                end = Offset(startX + width * 0.15f, height),
                strokeWidth = 2f
            )
        }
    }
}

private fun DrawScope.drawSentinel2OpticalLayer(width: Float, height: Float, layer: SatelliteLayer, opacity: Float) {
    if (layer == SatelliteLayer.SENTINEL2_NDVI) {
        val ndviGreen = Brush.radialGradient(
            colors = listOf(Color(0xFF10B981).copy(alpha = opacity * 0.6f), Color.Transparent),
            center = Offset(width * 0.25f, height * 0.25f),
            radius = width * 0.4f
        )
        drawCircle(brush = ndviGreen, radius = width * 0.4f, center = Offset(width * 0.25f, height * 0.25f))

        val ndviScar = Brush.radialGradient(
            colors = listOf(Color(0xFFB91C1C).copy(alpha = opacity * 0.7f), Color.Transparent),
            center = Offset(width * 0.68f, height * 0.32f),
            radius = width * 0.25f
        )
        drawCircle(brush = ndviScar, radius = width * 0.25f, center = Offset(width * 0.68f, height * 0.32f))
    } else {
        val opticalTint = Brush.linearGradient(
            colors = listOf(Color(0xFF1A382B).copy(alpha = opacity * 0.5f), Color(0xFF234237).copy(alpha = opacity * 0.5f)),
            start = Offset.Zero,
            end = Offset(width, height)
        )
        drawRect(brush = opticalTint, size = size)
    }
}

private fun DrawScope.drawSplitViewDivider(width: Float, height: Float) {
    val splitX = width * 0.5f
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(splitX, 0f),
        end = Offset(splitX, height),
        strokeWidth = 4f
    )
    drawCircle(color = Color.White, radius = 12f, center = Offset(splitX, height * 0.5f))
    drawCircle(color = Color(0xFF0284C7), radius = 6f, center = Offset(splitX, height * 0.5f))
}

private fun DrawScope.drawDeformationPointMarker(
    width: Float,
    height: Float,
    point: GroundDeformationPoint,
    isSelected: Boolean
) {
    val screenPos = getScreenCoordinate(point, width, height)
    val color = when (point.riskSeverity) {
        Severity.CRITICAL -> Color(0xFFDC2626)
        Severity.HIGH -> Color(0xFFEF4444)
        Severity.MODERATE -> Color(0xFFF59E0B)
        Severity.LOW -> Color(0xFF10B981)
    }

    if (isSelected) {
        drawCircle(
            color = color.copy(alpha = 0.35f),
            radius = 28f,
            center = screenPos
        )
        drawCircle(
            color = color,
            radius = 18f,
            center = screenPos
        )
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = screenPos
        )
    } else {
        drawCircle(
            color = color,
            radius = 14f,
            center = screenPos
        )
        drawCircle(
            color = Color.White,
            radius = 5f,
            center = screenPos
        )
    }
}

private fun DrawScope.drawCompassOverlay(width: Float) {
    val center = Offset(width - 32.dp.toPx(), 32.dp.toPx())
    drawCircle(color = Color(0xBB11221D), radius = 20.dp.toPx(), center = center)
    val pathN = Path().apply {
        moveTo(center.x, center.y - 14.dp.toPx())
        lineTo(center.x - 5.dp.toPx(), center.y)
        lineTo(center.x + 5.dp.toPx(), center.y)
        close()
    }
    drawPath(pathN, color = Color(0xFFEF4444))
}

private fun getScreenCoordinate(point: GroundDeformationPoint, width: Float, height: Float): Offset {
    return when (point.id) {
        "def_01" -> Offset(width * 0.68f, height * 0.32f)
        "def_02" -> Offset(width * 0.38f, height * 0.58f)
        "def_03" -> Offset(width * 0.76f, height * 0.68f)
        else -> Offset(width * 0.28f, height * 0.82f)
    }
}

private fun findNearestPoint(
    tap: Offset,
    width: Float,
    height: Float,
    points: List<GroundDeformationPoint>
): GroundDeformationPoint? {
    var closest: GroundDeformationPoint? = null
    var minDistance = Float.MAX_VALUE
    points.forEach { pt ->
        val pos = getScreenCoordinate(pt, width, height)
        val dist = hypot(tap.x - pos.x, tap.y - pos.y)
        if (dist < 80f && dist < minDistance) {
            minDistance = dist
            closest = pt
        }
    }
    return closest
}
