package com.example.landguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.landguard.domain.model.Severity
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.BrandContainer
import com.example.landguard.ui.theme.BrandPrimary
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
// Standard LandGuard Card
// ─────────────────────────────────────────────────────────────

@Composable
fun LandGuardCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = BgBorder
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────
// Primary Button
// ─────────────────────────────────────────────────────────────

@Composable
fun LandGuardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandPrimary,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Secondary / Outline Button
// ─────────────────────────────────────────────────────────────

@Composable
fun LandGuardOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BrandPrimary
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = BrandPrimary
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Section Header
// ─────────────────────────────────────────────────────────────

@Composable
fun LandGuardSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.weight(1f))

        if (action != null && onAction != null) {
            androidx.compose.material3.TextButton(
                onClick = onAction
            ) {
                Text(
                    text = action,
                    color = BrandPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.width(2.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Risk Badge
// ─────────────────────────────────────────────────────────────

@Composable
fun RiskBadge(
    severity: Severity,
    modifier: Modifier = Modifier
) {
    val color: Color
    val background: Color

    when (severity) {
        Severity.CRITICAL -> {
            color = RiskCritical
            background = RiskCriticalContainer
        }

        Severity.HIGH -> {
            color = RiskHigh
            background = RiskHighContainer
        }

        Severity.MODERATE -> {
            color = RiskModerate
            background = RiskModerateContainer
        }

        Severity.LOW -> {
            color = RiskLow
            background = RiskLowContainer
        }
    }

    Surface(
        modifier = modifier,
        color = background,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = severity.name,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Risk Filter Chip
// ─────────────────────────────────────────────────────────────

@Composable
fun RiskFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    severity: Severity? = null,
    modifier: Modifier = Modifier
) {
    val accent = when (severity) {
        Severity.CRITICAL -> RiskCritical
        Severity.HIGH -> RiskHigh
        Severity.MODERATE -> RiskModerate
        Severity.LOW -> RiskLow
        null -> BrandPrimary
    }

    Surface(
        modifier = modifier,
        onClick = onClick,
        color = if (selected) accent else BgSurface,
        shape = RoundedCornerShape(22.dp),
        border = if (!selected) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                BgBorder
            )
        } else {
            null
        }
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Medium
            },
            modifier = Modifier.padding(
                horizontal = 15.dp,
                vertical = 9.dp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Stat Card
// ─────────────────────────────────────────────────────────────

@Composable
fun LandGuardStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = BrandPrimary
) {
    LandGuardCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .height(3.dp)
                    .fillMaxWidth(0.38f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Risk Gauge
// ─────────────────────────────────────────────────────────────

@Composable
fun RiskGauge(
    score: Int,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 150.dp
) {
    val safeScore = score.coerceIn(0, 100)

    val riskColor = when {
        safeScore >= 80 -> RiskCritical
        safeScore >= 60 -> RiskHigh
        safeScore >= 35 -> RiskModerate
        else -> RiskLow
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(size)
        ) {
            val strokeWidth = 12.dp.toPx()

            drawArc(
                color = BgDeep,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = riskColor,
                startAngle = 135f,
                sweepAngle = 270f * (safeScore / 100f),
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = safeScore.toString(),
                color = TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "/ 100",
                color = TextMuted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = when {
                    safeScore >= 80 -> "Critical"
                    safeScore >= 60 -> "High Risk"
                    safeScore >= 35 -> "Moderate"
                    else -> "Low Risk"
                },
                color = riskColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Alert Card
// ─────────────────────────────────────────────────────────────

@Composable
fun LandGuardAlertCard(
    title: String,
    location: String,
    timestamp: String,
    severity: Severity,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null
) {
    LandGuardCard(
        modifier = modifier
    ) {
        val clickableModifier = if (onClick != null) {
            Modifier
        } else {
            Modifier
        }

        Row(
            modifier = clickableModifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier
                    } else {
                        Modifier
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when (severity) {
                            Severity.CRITICAL -> RiskCriticalContainer
                            Severity.HIGH -> RiskHighContainer
                            Severity.MODERATE -> RiskModerateContainer
                            Severity.LOW -> RiskLowContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = when (severity) {
                        Severity.CRITICAL -> RiskCritical
                        Severity.HIGH -> RiskHigh
                        Severity.MODERATE -> RiskModerate
                        Severity.LOW -> RiskLow
                    },
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    RiskBadge(severity = severity)
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = location,
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = description,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 2
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = timestamp,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            if (onClick != null) {
                Spacer(Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────

@Composable
fun LandGuardEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(BrandContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = message,
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}