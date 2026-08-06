package dev.doug.globetraveler.domain

data class MapPack(
    val descriptor: MapDescriptor,
    val geometryGeoJson: String,
)
