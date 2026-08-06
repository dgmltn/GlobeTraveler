package dev.doug.globetraveler.map

import dev.doug.globetraveler.domain.CameraDefaults
import dev.doug.globetraveler.domain.Region
import dev.doug.globetraveler.domain.Visit
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

internal const val EMPTY_FEATURE_COLLECTION = """{"type":"FeatureCollection","features":[]}"""

data class RegionDetails(val region: Region, val visit: Visit?)

data class MapUiState(
    val loading: Boolean = true,
    val mapName: String = "",
    val cameraDefaults: CameraDefaults? = null,
    val totalCount: Int = 0,
    val visitedCodes: ImmutableSet<String> = persistentSetOf(),
    val geometryGeoJson: String = EMPTY_FEATURE_COLLECTION,
    val details: RegionDetails? = null,
) {
    val visitedCount: Int get() = visitedCodes.size
}
