package dev.doug.globetraveler.map

import dev.doug.globetraveler.domain.CameraDefaults
import dev.doug.globetraveler.domain.Region
import dev.doug.globetraveler.domain.Visit

data class RegionDetails(val region: Region, val visit: Visit?)

data class MapUiState(
    val loading: Boolean = true,
    val mapName: String = "",
    val cameraDefaults: CameraDefaults? = null,
    val totalCount: Int = 0,
    val visitedCount: Int = 0,
    val visitedGeoJson: String = EMPTY_FEATURE_COLLECTION,
    val unvisitedGeoJson: String = EMPTY_FEATURE_COLLECTION,
    val details: RegionDetails? = null,
)
