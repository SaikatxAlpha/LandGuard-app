package com.example.landguard.ui.startup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.landguard.ui.brand.BrandColors
import com.example.landguard.ui.brand.HazardSpots
import com.example.landguard.ui.brand.HazardTerrain
import com.example.landguard.ui.brand.LandGuardLogo
import com.example.landguard.ui.brand.ScenicTerrain
import com.example.landguard.ui.brand.SystemBarIcons
import com.example.landguard.ui.brand.TopoContours
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path

private const val PAGE_COUNT = 3

@Composable
fun LandGuardOnboarding(onFinished: () -> Unit) {
    SystemBarIcons(darkIcons = false)

    val pagerState = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == PAGE_COUNT - 1

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.ForestNight)
    ) {
        val compact = maxHeight < 700.dp
        val narrow = maxWidth < 360.dp

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val offset = pageOffset(pagerState, page)
            // Each page is clipped so its parallax art can never draw over a neighbour.
            Box(
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                when (page) {
                    0 -> WatchPage(offset, compact, narrow)
                    1 -> PredictPage(offset, compact, narrow)
                    else -> TogetherPage(offset, compact, narrow, isVisible = pagerState.settledPage == 2)
                }
            }
        }

        // ── Top bar: logo + Skip ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LandGuardLogo(size = 34.dp)
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = !isLast, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = "Skip",
                    color = BrandColors.Mist,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onFinished)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        // ── Bottom controls ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, BrandColors.ForestNight.copy(alpha = 0.85f)))
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = if (compact) 16.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageDots(pagerState)
            Spacer(Modifier.height(if (compact) 16.dp else 22.dp))
            BrandButton(
                text = if (isLast) "Get Started" else "Next",
                showArrow = !isLast,
                onClick = {
                    if (isLast) onFinished()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )
        }
    }
}

private fun pageOffset(state: PagerState, page: Int): Float =
    (state.currentPage - page) + state.currentPageOffsetFraction

private const val ART_OVERSCAN = 1.4f
private const val ART_PARALLAX = 0.2f

/**
 * Full-bleed page art that drifts slower than the page while swiping.
 * The art is drawn wider than the page ([ART_OVERSCAN]) so the parallax
 * shift (at most [ART_PARALLAX] of the width) never reveals an empty edge.
 */
@Composable
private fun ParallaxArt(offset: Float, content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val pageWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        Box(
            modifier = Modifier
                .requiredWidth(maxWidth * ART_OVERSCAN)
                .fillMaxHeight()
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = offset.coerceIn(-1f, 1f) * pageWidthPx * ART_PARALLAX
                }
        ) {
            content()
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// PAGES
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun WatchPage(offset: Float, compact: Boolean, narrow: Boolean) {
    Box(Modifier.fillMaxSize()) {
        ParallaxArt(offset) {
            ScenicTerrain(
                modifier = Modifier.fillMaxSize(),
                sunX = 0.30f,
                sunY = 0.52f,
                horizon = 0.56f,
                seed = 1.3f
            )
        }
        SatelliteScan(modifier = Modifier.fillMaxSize())
        PageHeadline(
            offset = offset,
            compact = compact,
            narrow = narrow,
            line1 = "Eyes on",
            line2 = "every slope.",
            accent = "From orbit.",
            body = "ALOS-4 radar and Sentinel-2 imagery watch the ground for movement, soil saturation and vegetation loss."
        )
    }
}

@Composable
private fun PredictPage(offset: Float, compact: Boolean, narrow: Boolean) {
    Box(Modifier.fillMaxSize()) {
        ParallaxArt(offset) {
            HazardTerrain(modifier = Modifier.fillMaxSize())
            // Markers live inside the art layer so they stay pinned to their zones.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth
                val h = maxHeight
                HazardMarker("High Risk", Icons.Filled.Warning, BrandColors.Hazard, HazardSpots.High, w, h)
                HazardMarker("Watch", Icons.Filled.Warning, BrandColors.Watch, HazardSpots.Watch, w, h)
                HazardMarker("Safe", Icons.Filled.LocationOn, BrandColors.Leaf, HazardSpots.Safe, w, h)
            }
        }

        PageHeadline(
            offset = offset,
            compact = compact,
            narrow = narrow,
            line1 = "Predict.",
            line2 = "Protect.",
            accent = "Stay Ahead.",
            body = "Real-time risk insights and alerts for safer communities."
        )
    }
}

@Composable
private fun TogetherPage(offset: Float, compact: Boolean, narrow: Boolean, isVisible: Boolean) {
    Box(Modifier.fillMaxSize()) {
        ParallaxArt(offset) {
            ScenicTerrain(
                modifier = Modifier.fillMaxSize(),
                sunX = 0.36f,
                sunY = 0.50f,
                horizon = 0.52f,
                seed = 3.7f
            )
        }
        PageHeadline(
            offset = offset,
            compact = compact,
            narrow = narrow,
            line1 = "Together",
            line2 = "for ",
            accent = "Safer Lands",
            accentInline = true,
            body = "Communities, technology and nature — working as one."
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp, top = if (compact) 120.dp else 160.dp)
                .graphicsLayer { translationX = offset * size.width * 0.25f },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val cards = listOf(
                Triple(Icons.Filled.Eco, "Safer", "Communities"),
                Triple(Icons.Filled.Insights, "Smarter", "Decisions"),
                Triple(Icons.Filled.Shield, "Stronger", "Tomorrows")
            )
            cards.forEachIndexed { index, (icon, top, bottom) ->
                FeatureCard(
                    icon = icon,
                    title = top,
                    subtitle = bottom,
                    visible = isVisible,
                    delayMillis = 120L * index,
                    width = if (narrow) 138.dp else 158.dp
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// PIECES
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun PageHeadline(
    offset: Float,
    compact: Boolean,
    narrow: Boolean,
    line1: String,
    line2: String,
    accent: String,
    body: String,
    accentInline: Boolean = false
) {
    val titleSize: TextUnit = when {
        narrow -> 30.sp
        compact -> 34.sp
        else -> 40.sp
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(BrandColors.ForestNight.copy(alpha = 0.92f), BrandColors.ForestNight.copy(alpha = 0.55f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(start = 28.dp, end = 28.dp, top = if (compact) 64.dp else 88.dp, bottom = 40.dp)
            .graphicsLayer {
                // Text moves faster than the art and fades out as the page leaves.
                translationX = offset * size.width * 0.15f
                alpha = 1f - abs(offset).coerceIn(0f, 1f)
            }
    ) {
        val headline = buildAnnotatedString {
            withStyle(SpanStyle(color = BrandColors.Mist)) {
                append(line1)
                append("\n")
                append(line2)
            }
            if (!accentInline) append("\n")
            withStyle(SpanStyle(color = BrandColors.Leaf)) { append(accent) }
        }
        Text(
            text = headline,
            fontSize = titleSize,
            lineHeight = titleSize * 1.08f,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.02).em
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = body,
            color = BrandColors.Mist.copy(alpha = 0.82f),
            fontSize = if (compact) 15.sp else 17.sp,
            lineHeight = if (compact) 21.sp else 24.sp,
            modifier = Modifier.widthIn(max = 340.dp)
        )
    }
}

@Composable
private fun HazardMarker(
    label: String,
    icon: ImageVector,
    color: Color,
    spot: Offset,
    width: Dp,
    height: Dp
) {
    Row(
        modifier = Modifier.offset(
            x = width * spot.x - 14.dp,
            y = height * spot.y - 40.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.22f))
                .border(1.5.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = BrandColors.Mist,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(BrandColors.ForestNight.copy(alpha = 0.78f))
                .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    visible: Boolean,
    delayMillis: Long,
    width: Dp
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMillis)
            shown = true
        }
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(420)) + slideInHorizontally(tween(420)) { it / 3 }
    ) {
        Row(
            modifier = Modifier
                .width(width)
                .clip(RoundedCornerShape(16.dp))
                .background(BrandColors.Card)
                .border(1.dp, BrandColors.CardBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = BrandColors.Leaf, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = BrandColors.Mist, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = BrandColors.MistMuted, fontSize = 12.sp)
            }
        }
    }
}

/** A satellite gliding across the sky with a soft scan beam over the terrain. */
@Composable
private fun SatelliteScan(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(9000, easing = LinearEasing)
        ),
        label = "scanT"
    )
    BoxWithConstraints(modifier) {
        val x = maxWidth * (0.08f + 0.84f * t)
        val y = maxHeight * (0.40f - 0.06f * sin(t * PI.toFloat()))
        Canvas(Modifier.fillMaxSize()) {
            val sx = size.width * (0.08f + 0.84f * t)
            val sy = size.height * (0.40f - 0.06f * sin(t * PI.toFloat())) + 12.dp.toPx()
            val groundY = size.height * 0.78f
            val spread = size.width * 0.09f
            val beam = Path().apply {
                moveTo(sx, sy)
                lineTo(sx + spread, groundY)
                lineTo(sx - spread, groundY)
                close()
            }
            drawPath(
                beam,
                Brush.verticalGradient(
                    listOf(BrandColors.Leaf.copy(alpha = 0.16f), BrandColors.Leaf.copy(alpha = 0f)),
                    startY = sy,
                    endY = groundY
                )
            )
            drawLine(
                BrandColors.LeafBright.copy(alpha = 0.45f),
                start = Offset(sx - spread, groundY),
                end = Offset(sx + spread, groundY),
                strokeWidth = 2.dp.toPx()
            )
        }
        TopoContours(
            modifier = Modifier.fillMaxSize(),
            centerX = 0.5f,
            centerY = 0.85f,
            rings = 6,
            color = BrandColors.Leaf.copy(alpha = 0.08f)
        )
        Icon(
            imageVector = Icons.Filled.SatelliteAlt,
            contentDescription = null,
            tint = BrandColors.Mist,
            modifier = Modifier
                .offset(x = x - 13.dp, y = y - 13.dp)
                .size(26.dp)
        )
    }
}

@Composable
private fun PageDots(state: PagerState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(PAGE_COUNT) { index ->
            val selected = state.currentPage == index
            val width by animateDpAsState(
                if (selected) 22.dp else 8.dp,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "dotWidth"
            )
            val color by animateColorAsState(
                if (selected) BrandColors.Leaf else BrandColors.Mist.copy(alpha = 0.35f),
                tween(250),
                label = "dotColor"
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
fun BrandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArrow: Boolean = false,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "buttonScale"
    )
    Box(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF3E9F4E), BrandColors.Leaf)))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = text to showArrow,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
            label = "buttonLabel"
        ) { (label, arrow) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = BrandColors.ForestNight, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (arrow) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = BrandColors.ForestNight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
