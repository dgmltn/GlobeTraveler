package dev.doug.globetraveler.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal const val EMPTY_FEATURE_COLLECTION = """{"type":"FeatureCollection","features":[]}"""

/** Partitions a FeatureCollection into visited/unvisited collections by the `code` property. */
object GeoJsonSplitter {

    class ParsedFeatures internal constructor(
        internal val features: List<Pair<String, JsonElement>>,
    )

    data class Split(val visitedGeoJson: String, val unvisitedGeoJson: String)

    fun parse(featureCollectionJson: String): ParsedFeatures {
        val features = Json.parseToJsonElement(featureCollectionJson)
            .jsonObject.getValue("features") as JsonArray
        return ParsedFeatures(
            features.map { feature ->
                val code = feature.jsonObject.getValue("properties")
                    .jsonObject.getValue("code").jsonPrimitive.content
                code to feature
            },
        )
    }

    fun split(features: ParsedFeatures, visitedCodes: Set<String>): Split {
        val (visited, unvisited) = features.features.partition { (code, _) -> code in visitedCodes }
        return Split(
            visitedGeoJson = featureCollection(visited.map { it.second }),
            unvisitedGeoJson = featureCollection(unvisited.map { it.second }),
        )
    }

    private fun featureCollection(features: List<JsonElement>): String =
        Json.encodeToString(
            JsonObject.serializer(),
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("FeatureCollection"),
                    "features" to JsonArray(features),
                ),
            ),
        )
}
