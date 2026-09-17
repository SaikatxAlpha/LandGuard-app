package com.example.landguard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.BrandPrimaryLight
import com.example.landguard.ui.theme.EarthOchre
import com.example.landguard.ui.theme.GlassBg
import com.example.landguard.ui.theme.GlassBorder
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.RiskCriticalContainer
import com.example.landguard.ui.theme.RiskHigh
import com.example.landguard.ui.theme.RiskHighContainer
import com.example.landguard.ui.theme.RiskLow
import com.example.landguard.ui.theme.RiskLowContainer
import com.example.landguard.ui.theme.RiskModerate
import com.example.landguard.ui.theme.RiskModerateContainer
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────
// Severity helpers
// ─────────────────────────────────────────────────────────────

val Severity.accent: Color
    get() = when (this) {
        Severity.CRITICAL -> RiskCritical
        Severity.HIGH -> RiskHigh
        Severity.MODERATE -> RiskModerate
        Severity.LOW -> RiskLow
    }

val Severity.container: Color
    get() = when (this) {
        Severity.CRITICAL -> RiskCriticalContainer
        Severity.HIGH -> RiskHighContainer
        Severity.MODERATE -> RiskModerateContainer
        Severity.LOW -> RiskLowContainer
    }

val Severity.label: String
    get() = when (this) {
        Severity.CRITICAL -> "Critical"
        Severity.HIGH -> "High"
        Severity.MODERATE -> "Moderate"
        Severity.LOW -> "Low"
    }

// ─────────────────────────────────────────────────────────────
// Press feedback
// ─────────────────────────────────────────────────────────────

/** Subtle spring scale-down while pressed, plus click handling. */
fun Modifier.pressClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

// ─────────────────────────────────────────────────────────────
// Glass surface
// ─────────────────────────────────────────────────────────────

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(GlassBg)
            .border(1.dp, GlassBorder, shape),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────
// Floating map control
// ─────────────────────────────────────────────────────────────

@Composable
fun MapControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color = TextPrimary
) {
    val background by animateColorAsState(
        targetValue = if (active) BrandPrimary else GlassBg,
        animationSpec = tween(220),
        label = "controlBg"
    )
    Box(
        modifier = modifier
            .size(46.dp)
            .shadow(8.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, GlassBorder, CircleShape)
            .pressClickable(pressedScale = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) Color.White else tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Map style switcher (compact segmented control)
// ─────────────────────────────────────────────────────────────

@Composable
fun MapStyleSwitcher(
    current: RiskMapStyle,
    onSelect: (RiskMapStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RiskMapStyle.entries.forEach { style ->
                val selected = style == current
                val bg by animateColorAsState(
                    if (selected) BrandPrimary else Color.Transparent,
                    tween(200),
                    label = "styleBg"
                )
                Text(
                    text = style.label,
                    color = if (selected) Color.White else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(bg)
                        .pressClickable { onSelect(style) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Severity dot with optional live pulse
// ─────────────────────────────────────────────────────────────

@Composable
fun SeverityDot(
    severity: Severity,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    pulsing: Boolean = severity >= Severity.HIGH
) {
    Box(modifier = modifier.size(size * 2), contentAlignment = Alignment.Center) {
        if (pulsing) {
            val transition = rememberInfiniteTransition(label = "dotPulse")
            val pulse by transition.animateFloat(
                initialValue = 1f,
                targetValue = 2f,
                animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
                label = "dotPulseScale"
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulse)
                    .graphicsLayer { alpha = (2f - pulse) * 0.45f }
                    .clip(CircleShape)
                    .background(severity.accent)
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(severity.accent)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Severity pill
// ─────────────────────────────────────────────────────────────

@Composable
fun SeverityPill(severity: Severity, modifier: Modifier = Modifier) {
    Text(
        text = severity.label.uppercase(),
        color = severity.accent,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(severity.container)
            .border(1.dp, severity.accent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// ─────────────────────────────────────────────────────────────
// Brand mark
// ─────────────────────────────────────────────────────────────

@Composable
fun LandGuardWordmark(modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(if (compact) 28.dp else 34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(BrandPrimaryLight, BrandPrimary, EarthOchre))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (compact) 16.dp else 19.dp)
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = "LandGuard",
            color = TextPrimary,
            fontSize = if (compact) 16.sp else 19.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Metric tile
// ─────────────────────────────────────────────────────────────

@Composable
fun MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgElevated)
            .border(1.dp, BgBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            color = accent,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(
            text = label,
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
