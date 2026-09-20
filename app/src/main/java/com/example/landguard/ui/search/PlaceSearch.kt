package com.example.landguard.ui.search

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.landguard.ui.components.GlassSurface
import com.example.landguard.ui.components.SeverityPill
import com.example.landguard.ui.components.pressClickable
import com.example.landguard.ui.risk.RiskArea
import com.example.landguard.ui.theme.BgDivider
import com.example.landguard.ui.theme.BgElevated
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.RiskModerate
import com.example.landguard.ui.theme.TextMuted
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/** A real place returned by the platform geocoder. */
@Immutable
data class PlaceResult(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
) {
    val key: String get() = "%.4f,%.4f".format(Locale.US, latitude, longitude)
}

private sealed interface GeocodeState {
    data object Idle : GeocodeState
    data object Searching : GeocodeState
    data class Results(val places: List<PlaceResult>) : GeocodeState
    data class Failed(val message: String) : GeocodeState
}

/**
 * Place search through the device's platform geocoder (no invented results).
 * Results inside India are preferred; the query is retried worldwide when
 * nothing matches there.
 */
object PlaceGeocoder {
    // India bounding box (lower-left, upper-right).
    private const val IN_S = 6.4
    private const val IN_W = 68.0
    private const val IN_N = 37.6
    private const val IN_E = 97.6

    suspend fun search(context: Context, query: String, max: Int = 6): Result<List<PlaceResult>> =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) {
                return@withContext Result.failure(IllegalStateException("Place search is not available on this device"))
            }
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val inIndia = runCatching { geocoder.getFromLocationName(query, max, IN_S, IN_W, IN_N, IN_E) }
                    .getOrNull().orEmpty()
                @Suppress("DEPRECATION")
                val addresses = inIndia.ifEmpty { geocoder.getFromLocationName(query, max).orEmpty() }
                Result.success(
                    addresses
                        .filter { it.hasLatitude() && it.hasLongitude() }
                        .map { it.toPlace(query) }
                        .distinctBy { it.key }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Result.failure(IllegalStateException("Place search needs an internet connection"))
            } catch (e: Exception) {
                Result.failure(IllegalStateException("Place search failed: ${e.message ?: "unknown error"}"))
            }
        }

    private fun Address.toPlace(query: String): PlaceResult {
        val parts = listOfNotNull(featureName, subLocality, locality, subAdminArea, adminArea, countryName)
            .filter { it.isNotBlank() && it.toDoubleOrNull() == null }
            .distinct()
        val title = parts.firstOrNull() ?: query
        val subtitle = (0..maxAddressLineIndex).mapNotNull { getAddressLine(it) }.joinToString(", ")
            .ifBlank { parts.drop(1).joinToString(", ") }
        return PlaceResult(title, subtitle, latitude, longitude)
    }
}

/**
 * Search bar for monitored areas (matched locally, real LandGuard data) and
 * any place (platform geocoder). Collapsed it is a pill; expanded it shows
 * a text field with live results.
 */
@Composable
fun PlaceSearchBar(
    areas: List<RiskArea>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAreaSelected: (RiskArea) -> Unit,
    onPlaceSelected: (PlaceResult) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search a place or monitored area"
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var geocode by remember { mutableStateOf<GeocodeState>(GeocodeState.Idle) }
    var submitToken by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    val trimmed = query.trim()
    val areaMatches = remember(areas, trimmed) {
        if (trimmed.length < 2) emptyList()
        else {
            val q = trimmed.lowercase()
            areas.filter { it.name.lowercase().contains(q) || it.state.lowercase().contains(q) }
                .sortedWith(compareByDescending<RiskArea> { it.name.lowercase().startsWith(q) }.thenByDescending { it.score })
                .take(5)
        }
    }

    // Debounced geocoding while typing; the keyboard "search" action runs it immediately.
    LaunchedEffect(trimmed, submitToken, expanded) {
        if (!expanded || trimmed.length < 3) {
            geocode = GeocodeState.Idle
            return@LaunchedEffect
        }
        if (submitToken == 0) delay(450)
        geocode = GeocodeState.Searching
        geocode = PlaceGeocoder.search(context, trimmed).fold(
            onSuccess = { GeocodeState.Results(it) },
            onFailure = { GeocodeState.Failed(it.message ?: "Place search failed") }
        )
    }

    LaunchedEffect(expanded) {
        if (expanded) runCatching { focusRequester.requestFocus() }
    }

    fun close() {
        query = ""
        submitToken = 0
        focusManager.clearFocus()
        onExpandedChange(false)
    }

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 8.dp
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "searchExpanded"
        ) { isExpanded ->
            if (!isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressClickable(pressedScale = 0.98f) { onExpandedChange(true) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(placeholder, color = TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close search",
                            tint = TextPrimary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .pressClickable { close() }
                                .padding(10.dp)
                                .size(20.dp)
                        )
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(placeholder, color = TextMuted, fontSize = 15.sp, maxLines = 1)
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = {
                                    query = it
                                    submitToken = 0
                                },
                                singleLine = true,
                                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                                cursorBrush = SolidColor(BrandPrimary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (trimmed.length >= 2) submitToken++
                                    focusManager.clearFocus()
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .pressClickable {
                                        query = ""
                                        submitToken = 0
                                    }
                                    .padding(10.dp)
                                    .size(18.dp)
                            )
                        }
                    }

                    if (trimmed.length >= 2) {
                        HorizontalDivider(color = BgDivider)
                        SearchResults(
                            areaMatches = areaMatches,
                            geocode = geocode,
                            queryTooShort = trimmed.length < 3,
                            onAreaSelected = {
                                close()
                                onAreaSelected(it)
                            },
                            onPlaceSelected = {
                                close()
                                onPlaceSelected(it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    areaMatches: List<RiskArea>,
    geocode: GeocodeState,
    queryTooShort: Boolean,
    onAreaSelected: (RiskArea) -> Unit,
    onPlaceSelected: (PlaceResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 340.dp)
            .padding(vertical = 4.dp)
    ) {
        if (areaMatches.isNotEmpty()) {
            item { SectionLabel("Monitored areas") }
            items(areaMatches, key = { "area:" + it.id }) { area ->
                ResultRow(
                    icon = { Icon(Icons.Filled.Terrain, null, tint = BrandPrimary, modifier = Modifier.size(18.dp)) },
                    title = area.name,
                    subtitle = "${area.state} · risk score ${area.score}",
                    trailing = { SeverityPill(area.severity) },
                    onClick = { onAreaSelected(area) }
                )
            }
        }
        item { SectionLabel("Places") }
        when (geocode) {
            GeocodeState.Idle -> item {
                StatusLine(if (queryTooShort) "Type at least 3 letters to search places" else "Press search to look up this place")
            }
            GeocodeState.Searching -> item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = BrandPrimary, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Searching…", color = TextSecondary, fontSize = 13.sp)
                }
            }
            is GeocodeState.Failed -> item { StatusLine(geocode.message, warning = true) }
            is GeocodeState.Results -> if (geocode.places.isEmpty()) {
                item { StatusLine("No matching place found") }
            } else {
                items(geocode.places, key = { "place:" + it.key }) { place ->
                    ResultRow(
                        icon = { Icon(Icons.Filled.Place, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                        title = place.title,
                        subtitle = place.subtitle,
                        trailing = null,
                        onClick = { onPlaceSelected(place) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun StatusLine(text: String, warning: Boolean = false) {
    Text(
        text = text,
        color = if (warning) RiskModerate else TextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ResultRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(BgElevated),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}
