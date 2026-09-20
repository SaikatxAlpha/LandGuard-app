package com.example.landguard.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.landguard.ui.components.pressClickable
import com.example.landguard.ui.monitor.formatTime
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.BrandPrimaryLight
import com.example.landguard.ui.theme.GlassBg
import com.example.landguard.ui.theme.GlassBorder
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.RiskModerate
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary

enum class DataStatusKind { LOADING, UPDATED, CACHED, OFFLINE, UNAVAILABLE }

/** Honest freshness of the monitored-area data: where it came from and when. */
@Immutable
data class DataStatus(
    val kind: DataStatusKind,
    val label: String,
    val detail: String
)

/**
 * @param fromCache       the catalog was read from the on-device copy.
 * @param backendReachable null before the first LandGuard API attempt.
 */
fun monitoringStatus(
    isLoading: Boolean,
    catalogError: String?,
    fromCache: Boolean,
    fetchedAtMillis: Long?,
    online: Boolean,
    backendReachable: Boolean?,
    conditionsAtMillis: Long?,
    conditionsSource: String?
): DataStatus {
    val conditions = if (conditionsAtMillis != null && conditionsSource != null)
        "rainfall: $conditionsSource, ${formatTime(conditionsAtMillis)}"
    else "rainfall: DATA UNAVAILABLE"
    val api = when (backendReachable) {
        true -> "LandGuard API connected"
        false -> "LandGuard API unreachable — public sources used"
        null -> "LandGuard API: connecting"
    }
    return when {
        isLoading -> DataStatus(DataStatusKind.LOADING, "Loading monitored areas…", api)
        catalogError != null -> DataStatus(
            DataStatusKind.UNAVAILABLE,
            if (online) "DATA UNAVAILABLE" else "OFFLINE · DATA UNAVAILABLE",
            catalogError
        )
        !online -> DataStatus(
            DataStatusKind.OFFLINE,
            "OFFLINE · CACHED" + (fetchedAtMillis?.let { " ${formatTime(it)}" } ?: ""),
            "No connection. Showing the last downloaded data · $conditions"
        )
        fromCache -> DataStatus(
            DataStatusKind.CACHED,
            "CACHED" + (fetchedAtMillis?.let { " ${formatTime(it)}" } ?: ""),
            "Landslide catalog from this device · $conditions · $api"
        )
        else -> DataStatus(
            DataStatusKind.UPDATED,
            "Updated " + (fetchedAtMillis?.let { formatTime(it) } ?: "now"),
            "$conditions · $api"
        )
    }
}

@Composable
fun DataStatusChip(
    status: DataStatus,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val accent: Color = when (status.kind) {
        DataStatusKind.LOADING, DataStatusKind.UPDATED -> BrandPrimaryLight
        DataStatusKind.CACHED -> RiskModerate
        DataStatusKind.OFFLINE -> TextSecondary
        DataStatusKind.UNAVAILABLE -> RiskCritical
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(50))
            .padding(start = 10.dp, end = if (onRetry != null) 4.dp else 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status.kind) {
            DataStatusKind.LOADING -> CircularProgressIndicator(
                color = BrandPrimary,
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(11.dp)
            )
            else -> Icon(
                imageVector = when (status.kind) {
                    DataStatusKind.UPDATED -> Icons.Filled.CloudDone
                    DataStatusKind.CACHED -> Icons.Filled.History
                    DataStatusKind.OFFLINE -> Icons.Filled.CloudOff
                    else -> Icons.Filled.WarningAmber
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = status.label,
            color = if (status.kind == DataStatusKind.UPDATED) TextPrimary else accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onRetry != null && (status.kind == DataStatusKind.UNAVAILABLE || status.kind == DataStatusKind.OFFLINE)) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "Retry",
                tint = TextPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .pressClickable(onClick = onRetry)
                    .padding(4.dp)
                    .size(14.dp)
            )
        }
    }
}

/** Second line under a status chip (source + reason). */
@Composable
fun DataStatusDetail(status: DataStatus, modifier: Modifier = Modifier) {
    Text(
        text = status.detail,
        color = TextMuted,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
