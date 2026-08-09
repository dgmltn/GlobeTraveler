package dev.doug.globetraveler.map

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Guards the output of scripts/strip-basemap-labels.py: the bundled basemap styles must show
 * state names and no other place names.
 */
class BundledBasemapStyleTest {

    @Test
    fun bundled_styles_have_no_place_labels_except_states() {
        for (name in listOf("basemap-light.json", "basemap-dark.json")) {
            val file = File("src/commonMain/composeResources/files/$name")
            assertTrue(file.exists(), "$name missing — run scripts/strip-basemap-labels.py")

            val layers = Json.parseToJsonElement(file.readText()).jsonObject
                .getValue("layers").jsonArray
                .map { it.jsonObject }
            val placeLayerIds = layers
                .filter { it["source-layer"]?.jsonPrimitive?.content == "place" }
                .map { it.getValue("id").jsonPrimitive.content }

            assertTrue(placeLayerIds.isNotEmpty(), "$name: state label layer missing")
            assertTrue(
                placeLayerIds.all { it.endsWith("_state") },
                "$name: unexpected place labels $placeLayerIds",
            )
        }
    }
}
