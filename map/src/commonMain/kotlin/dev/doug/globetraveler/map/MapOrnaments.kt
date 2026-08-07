package dev.doug.globetraveler.map

import androidx.compose.foundation.layout.PaddingValues
import org.maplibre.compose.map.OrnamentOptions

/**
 * Ornament placement for the states map: no scale bar (little value for a visited-states
 * tracker, and its default top-start spot fought the status bar), attribution button
 * bottom-end, MapLibre logo bottom-start, everything inset by [safeAreaPadding] so no
 * ornament draws under the system bars.
 *
 * expect/actual because the per-ornament [OrnamentOptions] fields are only visible in
 * platform source sets; common code sees just the presets.
 */
internal expect fun globeOrnamentOptions(safeAreaPadding: PaddingValues): OrnamentOptions
