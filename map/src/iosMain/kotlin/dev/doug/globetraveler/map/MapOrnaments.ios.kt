package dev.doug.globetraveler.map

import androidx.compose.foundation.layout.PaddingValues
import org.maplibre.compose.map.OrnamentOptions

internal actual fun globeOrnamentOptions(safeAreaPadding: PaddingValues): OrnamentOptions =
    OrnamentOptions.AllEnabled.copy(
        padding = safeAreaPadding,
        isScaleBarEnabled = false,
    )
