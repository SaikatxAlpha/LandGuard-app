package com.example.landguard.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.landguard.ui.home.PanelButton
import com.example.landguard.ui.theme.BgBorder
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BgSurface
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.RiskCritical
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary
import java.util.Locale

/** Report an observation — a ground-level landslide sighting sent to the authority. */
@Composable
fun ReportScreen(
    place: ReportPlace,
    placeLabel: String?,
    onClose: () -> Unit,
    onSubmitted: () -> Unit = {},
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.submitSuccess) {
        if (state.submitSuccess) {
            viewModel.consumeSuccess()
            onSubmitted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Report an observation", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Sent to LandGuard authorities as a field report", color = TextSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Where the report is attached
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgSurface)
                .border(1.dp, BgBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val located = place.latitude != null && place.longitude != null
            Icon(
                if (located) Icons.Filled.MyLocation else Icons.Filled.LocationOff,
                null,
                tint = if (located) BrandPrimary else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (located) placeLabel ?: "Your GPS position" else "Location unavailable",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (located) buildString {
                        append("%.5f, %.5f".format(Locale.US, place.latitude, place.longitude))
                        place.zoneName?.let { append(" · nearest area: $it") }
                    } else "The report will be sent without coordinates. Enable location for a geotagged report.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Describe what you're seeing — cracks in the ground, unusual water flow, leaning trees, fallen rocks.",
            color = TextSecondary,
            fontSize = 13.sp
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Description") },
            enabled = !state.isSubmitting,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BgBorder,
                focusedContainerColor = BgSurface,
                unfocusedContainerColor = BgSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = BrandPrimary,
                focusedLabelColor = BrandPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .padding(top = 12.dp)
        )

        state.errorMessage?.let {
            Text(text = it, color = RiskCritical, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(16.dp))

        if (state.isSubmitting) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = BrandPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sending to LandGuard…", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            PanelButton(
                text = "Send report",
                primary = true,
                onClick = { viewModel.submit(place) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
