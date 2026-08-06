package dev.doug.globetraveler.data

import dev.doug.globetraveler.domain.CameraDefaults
import dev.doug.globetraveler.domain.MapDescriptor
import dev.doug.globetraveler.domain.MapId
import dev.doug.globetraveler.domain.MapPack
import dev.doug.globetraveler.domain.MapPackRepository
import dev.doug.globetraveler.domain.Region
import dev.doug.globetraveler.domain.RegionCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class CameraDto(val latitude: Double, val longitude: Double, val zoom: Double)

@Serializable
private data class RegionDto(val code: String, val name: String)

@Serializable
private data class DescriptorDto(
    val mapId: String,
    val name: String,
    val geometryAsset: String,
    val camera: CameraDto,
    val regions: List<RegionDto>,
)

class MapPackRepositoryImpl(private val json: Json) : MapPackRepository {

    override suspend fun load(mapId: MapId): MapPack {
        val descriptorBytes = Res.readBytes("files/${mapId.value}.descriptor.json")
        val dto = json.decodeFromString<DescriptorDto>(descriptorBytes.decodeToString())
        val geometry = Res.readBytes("files/${dto.geometryAsset}").decodeToString()
        return MapPack(
            descriptor = MapDescriptor(
                mapId = MapId(dto.mapId),
                name = dto.name,
                geometryAsset = dto.geometryAsset,
                camera = CameraDefaults(dto.camera.latitude, dto.camera.longitude, dto.camera.zoom),
                regions = dto.regions.map { Region(RegionCode(it.code), it.name) },
            ),
            geometryGeoJson = geometry,
        )
    }
}
