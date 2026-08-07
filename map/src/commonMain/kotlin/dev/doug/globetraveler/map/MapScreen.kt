package dev.doug.globetraveler.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.doug.globetraveler.design.GlobeTheme
import dev.doug.globetraveler.design.MapPalette
import dev.doug.globetraveler.domain.CameraDefaults
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.contains
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.not
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Position

private const val LIGHT_STYLE_URI = "https://tiles.openfreemap.org/styles/positron"
private const val DARK_STYLE_URI = "https://tiles.openfreemap.org/styles/dark"

@Composable
fun MapScreen(viewModel: MapViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MapContent(
        state = state,
        onRegionTapped = viewModel::onRegionTapped,
        onRegionLongPressed = viewModel::onRegionLongPressed,
        onDetailsSave = viewModel::onDetailsSave,
        onDetailsRemoveVisit = viewModel::onDetailsRemoveVisit,
        onDetailsDismissed = viewModel::onDetailsDismissed,
    )
}

@Composable
fun MapContent(
    state: MapUiState,
    onRegionTapped: (String) -> Unit,
    onRegionLongPressed: (String) -> Unit,
    onDetailsSave: (LocalDate?, String?) -> Unit,
    onDetailsRemoveVisit: () -> Unit,
    onDetailsDismissed: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        val cameraDefaults = state.cameraDefaults
        if (cameraDefaults != null) {
            StatesMap(
                cameraDefaults = cameraDefaults,
                geometryGeoJson = state.geometryGeoJson,
                visitedCodes = state.visitedCodes,
                onRegionTapped = onRegionTapped,
                onRegionLongPressed = onRegionLongPressed,
            )
        }
        if (state.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            CounterChip(
                visitedCount = state.visitedCount,
                totalCount = state.totalCount,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
            )
        }
        val details = state.details
        if (details != null) {
            VisitDetailsSheet(
                details = details,
                onSave = onDetailsSave,
                onRemoveVisit = onDetailsRemoveVisit,
                onDismiss = onDetailsDismissed,
            )
        }
    }
}

@Composable
private fun StatesMap(
    cameraDefaults: CameraDefaults,
    geometryGeoJson: String,
    visitedCodes: ImmutableSet<String>,
    onRegionTapped: (String) -> Unit,
    onRegionLongPressed: (String) -> Unit,
) {
    var loadFailed by remember { mutableStateOf(false) }
    var retryToken by remember { mutableStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        key(retryToken) {
            LoadableMap(
                cameraDefaults = cameraDefaults,
                geometryGeoJson = geometryGeoJson,
                visitedCodes = visitedCodes,
                onRegionTapped = onRegionTapped,
                onRegionLongPressed = onRegionLongPressed,
                onLoadFailed = { loadFailed = true },
            )
        }
        if (loadFailed) {
            MapLoadErrorBanner(
                onRetry = {
                    loadFailed = false
                    retryToken++
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            )
        }
    }
}

@Composable
private fun LoadableMap(
    cameraDefaults: CameraDefaults,
    geometryGeoJson: String,
    visitedCodes: ImmutableSet<String>,
    onRegionTapped: (String) -> Unit,
    onRegionLongPressed: (String) -> Unit,
    onLoadFailed: () -> Unit,
) {
    val camera = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = cameraDefaults.latitude, longitude = cameraDefaults.longitude),
            zoom = cameraDefaults.zoom,
        ),
    )
    // Basemap follows the phone's color scheme; the system bar icons do the same
    // (dark icons in light mode, light in dark), so they stay legible over the tiles.
    val darkTheme = isSystemInDarkTheme()
    val palette = if (darkTheme) MapPalette.Dark else MapPalette.Light
    MaplibreMap(
        baseStyle = BaseStyle.Uri(if (darkTheme) DARK_STYLE_URI else LIGHT_STYLE_URI),
        cameraState = camera,
        onMapLoadFailed = { onLoadFailed() },
        modifier = Modifier.fillMaxSize(),
    ) {
        // The geometry is loaded into a single static source exactly once; visited-ness
        // is a layer filter, so toggling never re-parses or re-tessellates the polygons.
        val states = rememberGeoJsonSource(GeoJsonData.JsonString(geometryGeoJson))
        val isVisited = const(visitedCodes.toList()).contains(feature.get("code").asString())

        FillLayer(
            id = "unvisited-fill",
            source = states,
            filter = !isVisited,
            color = const(palette.unvisitedFill),
            onClick = tapHandler(onRegionTapped),
            onLongClick = tapHandler(onRegionLongPressed),
        )
        LineLayer(
            id = "state-borders",
            source = states,
            color = const(palette.border),
            width = const(1.dp),
        )
        FillLayer(
            id = "visited-fill",
            source = states,
            filter = isVisited,
            color = const(palette.visitedFill),
            opacity = const(0.6f),
            onClick = tapHandler(onRegionTapped),
            onLongClick = tapHandler(onRegionLongPressed),
        )
        LineLayer(
            id = "visited-borders",
            source = states,
            filter = isVisited,
            color = const(palette.visitedOutline),
            width = const(1.5.dp),
        )
    }
}

private fun tapHandler(onRegion: (String) -> Unit): (List<Feature<*, *>>) -> ClickResult = { features ->
    val code = features.firstNotNullOfOrNull { it.regionCode() }
    if (code != null) {
        onRegion(code)
        ClickResult.Consume
    } else {
        ClickResult.Pass
    }
}

internal fun Feature<*, *>.regionCode(): String? =
    ((properties as? JsonObject)?.get("code") as? JsonPrimitive)?.contentOrNull

@Composable
fun MapLoadErrorBanner(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Map failed to load",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Preview
@Composable
private fun Preview_MapLoadErrorBanner() {
    GlobeTheme {
        MapLoadErrorBanner(onRetry = {})
    }
}

@Composable
fun CounterChip(visitedCount: Int, totalCount: Int, modifier: Modifier = Modifier) {
    // Deliberately theme-independent: the black scrim + white text stays legible over
    // both the light and dark basemaps, so the chip doesn't track the color scheme.
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White,
    ) {
        Text(
            text = "$visitedCount / $totalCount visited",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview
@Composable
private fun Preview_CounterChip() {
    GlobeTheme {
        CounterChip(visitedCount = 12, totalCount = 50)
    }
}

@Preview
@Composable
private fun Preview_CounterChip_None() {
    GlobeTheme {
        CounterChip(visitedCount = 0, totalCount = 50)
    }
}

@Preview
@Composable
private fun Preview_MapContent_Loading() {
    GlobeTheme {
        MapContent(
            state = MapUiState(loading = true),
            onRegionTapped = {},
            onRegionLongPressed = {},
            onDetailsSave = { _, _ -> },
            onDetailsRemoveVisit = {},
            onDetailsDismissed = {},
        )
    }
}
