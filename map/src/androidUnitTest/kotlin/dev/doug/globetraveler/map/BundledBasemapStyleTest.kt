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
 * US state names and country names — no other place names.
 */
class BundledBasemapStyleTest {

    @Test
    fun bundled_styles_show_only_us_states_and_countries() {
        for (name in listOf("basemap-light.json", "basemap-dark.json")) {
            val file = File("src/commonMain/composeResources/files/$name")
            assertTrue(file.exists(), "$name missing — run scripts/strip-basemap-labels.py")

            val layers = Json.parseToJsonElement(file.readText()).jsonObject
                .getValue("layers").jsonArray
                .map { it.jsonObject }
            val placeLayers = layers
                .filter { it["source-layer"]?.jsonPrimitive?.content == "place" }
                .associate { it.getValue("id").jsonPrimitive.content to it["filter"].toString() }

            val stateLayers = placeLayers.filterKeys { it.endsWith("_state") }
            val countryLayers = placeLayers.filterKeys { "country" in it }
            assertTrue(stateLayers.size == 1, "$name: state layers ${stateLayers.keys}")
            assertTrue(countryLayers.isNotEmpty(), "$name: country label layers missing")
            assertTrue(
                placeLayers.size == stateLayers.size + countryLayers.size,
                "$name: unexpected place labels ${placeLayers.keys}",
            )
            assertTrue(
                "Wyoming" in stateLayers.values.single(),
                "$name: state layer lacks the US name filter",
            )
        }
    }
}
