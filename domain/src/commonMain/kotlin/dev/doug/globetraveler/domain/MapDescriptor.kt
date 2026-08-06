package dev.doug.globetraveler.domain

data class CameraDefaults(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
)

data class MapDescriptor(
    val mapId: MapId,
    val name: String,
    val geometryAsset: String,
    val camera: CameraDefaults,
    val regions: List<Region>,
)
