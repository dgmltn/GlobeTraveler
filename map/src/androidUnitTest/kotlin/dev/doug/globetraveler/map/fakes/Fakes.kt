package dev.doug.globetraveler.map.fakes

import dev.doug.globetraveler.domain.ApproximateLocation
import dev.doug.globetraveler.domain.CameraDefaults
import dev.doug.globetraveler.domain.DeviceLocationRepository
import dev.doug.globetraveler.domain.MapDescriptor
import dev.doug.globetraveler.domain.MapId
import dev.doug.globetraveler.domain.MapPack
import dev.doug.globetraveler.domain.MapPackRepository
import dev.doug.globetraveler.domain.Region
import dev.doug.globetraveler.domain.RegionCode
import dev.doug.globetraveler.domain.RegionId
import dev.doug.globetraveler.domain.Visit
import dev.doug.globetraveler.domain.VisitRepository
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

val TEST_GEOMETRY = """
{"type":"FeatureCollection","features":[
 {"type":"Feature","properties":{"code":"CA","name":"California"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},
 {"type":"Feature","properties":{"code":"NV","name":"Nevada"},"geometry":{"type":"Polygon","coordinates":[[[2,2],[3,2],[3,3],[2,2]]]}},
 {"type":"Feature","properties":{"code":"OR","name":"Oregon"},"geometry":{"type":"Polygon","coordinates":[[[4,4],[5,4],[5,5],[4,4]]]}}
]}
""".trimIndent()

val TEST_PACK = MapPack(
    descriptor = MapDescriptor(
        mapId = MapId("us-states"),
        name = "Test States",
        geometryAsset = "test.geojson",
        camera = CameraDefaults(latitude = 39.5, longitude = -98.35, zoom = 3.0),
        regions = listOf(
            Region(RegionCode("CA"), "California"),
            Region(RegionCode("NV"), "Nevada"),
            Region(RegionCode("OR"), "Oregon"),
        ),
    ),
    geometryGeoJson = TEST_GEOMETRY,
)

class FakeMapPackRepository(private val pack: MapPack = TEST_PACK) : MapPackRepository {
    override suspend fun load(mapId: MapId): MapPack = pack
}

class FakeVisitRepository : VisitRepository {
    private val visits = MutableStateFlow<Map<RegionId, Visit>>(emptyMap())

    override fun observeVisits(mapId: MapId): Flow<List<Visit>> =
        visits.map { all -> all.values.filter { it.regionId.mapId == mapId } }

    override suspend fun toggle(regionId: RegionId) {
        visits.value = visits.value.let { current ->
            if (regionId in current) current - regionId
            else current + (regionId to Visit(regionId, null, null, Instant.fromEpochMilliseconds(1_000)))
        }
    }

    override suspend fun updateDetails(visit: Visit) {
        val existing = visits.value[visit.regionId]
        visits.value += visit.regionId to visit.copy(markedAt = existing?.markedAt ?: visit.markedAt)
    }
}

class FakeDeviceLocationRepository : DeviceLocationRepository {
    private val locations = MutableSharedFlow<ApproximateLocation>(replay = 1)
    override fun observeLocation(): Flow<ApproximateLocation> = locations
    suspend fun emit(location: ApproximateLocation) = locations.emit(location)
}
