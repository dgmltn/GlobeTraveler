package dev.doug.globetraveler.data

import dev.doug.globetraveler.domain.MapId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MapPackRepositoryImplTest {

    private val repository = MapPackRepositoryImpl(Json { ignoreUnknownKeys = true })

    @Test
    fun `loads us-states pack with 50 matching regions`() = runTest {
        val pack = repository.load(MapId("us-states"))

        assertEquals("us-states", pack.descriptor.mapId.value)
        assertEquals(50, pack.descriptor.regions.size)

        val features = Json.parseToJsonElement(pack.geometryGeoJson)
            .jsonObject.getValue("features").jsonArray
        assertEquals(50, features.size)

        val featureCodes = features.map {
            it.jsonObject.getValue("properties").jsonObject.getValue("code").jsonPrimitive.content
        }.toSet()
        assertEquals(pack.descriptor.regions.map { it.code.value }.toSet(), featureCodes)
    }

    @Test
    fun `descriptor camera has usable defaults`() = runTest {
        val camera = repository.load(MapId("us-states")).descriptor.camera
        assertEquals(3.0, camera.zoom)
    }
}
