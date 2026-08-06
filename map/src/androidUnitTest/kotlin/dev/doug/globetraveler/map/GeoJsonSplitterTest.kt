package dev.doug.globetraveler.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val THREE_STATES = """
{"type":"FeatureCollection","features":[
 {"type":"Feature","properties":{"code":"CA","name":"California"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},
 {"type":"Feature","properties":{"code":"NV","name":"Nevada"},"geometry":{"type":"Polygon","coordinates":[[[2,2],[3,2],[3,3],[2,2]]]}},
 {"type":"Feature","properties":{"code":"OR","name":"Oregon"},"geometry":{"type":"Polygon","coordinates":[[[4,4],[5,4],[5,5],[4,4]]]}}
]}
""".trimIndent()

class GeoJsonSplitterTest {

    private fun codesOf(featureCollection: String): List<String> =
        Json.parseToJsonElement(featureCollection).jsonObject.getValue("features").jsonArray
            .map { it.jsonObject.getValue("properties").jsonObject.getValue("code").jsonPrimitive.content }

    @Test
    fun `split partitions features by visited codes`() {
        val parsed = GeoJsonSplitter.parse(THREE_STATES)
        val split = GeoJsonSplitter.split(parsed, setOf("CA", "OR"))

        assertEquals(listOf("CA", "OR"), codesOf(split.visitedGeoJson))
        assertEquals(listOf("NV"), codesOf(split.unvisitedGeoJson))
    }

    @Test
    fun `unknown visited codes are ignored`() {
        val parsed = GeoJsonSplitter.parse(THREE_STATES)
        val split = GeoJsonSplitter.split(parsed, setOf("ZZ"))

        assertEquals(emptyList(), codesOf(split.visitedGeoJson))
        assertEquals(listOf("CA", "NV", "OR"), codesOf(split.unvisitedGeoJson))
    }

    @Test
    fun `features survive the round trip unchanged`() {
        val parsed = GeoJsonSplitter.parse(THREE_STATES)
        val split = GeoJsonSplitter.split(parsed, emptySet())

        val original = Json.parseToJsonElement(THREE_STATES).jsonObject.getValue("features")
        val roundTripped = Json.parseToJsonElement(split.unvisitedGeoJson).jsonObject.getValue("features")
        assertEquals(original, roundTripped)
    }
}
