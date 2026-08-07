package dev.doug.globetraveler.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Colors for map region fills, with a variant per basemap (light Positron, near-black Dark). */
@Immutable
data class MapPalette(
    val visitedFill: Color,
    val visitedOutline: Color,
    val unvisitedFill: Color,
    val border: Color,
    val youAreHere: Color,
) {
    companion object {
        val Light = MapPalette(
            visitedFill = Color(0xFF2E7D32),
            visitedOutline = Color(0xFF1B5E20),
            unvisitedFill = Color(0x14000000),
            border = Color(0xFF757575),
            youAreHere = Color(0xFF1E88E5),
        )

        val Dark = MapPalette(
            visitedFill = Color(0xFF81C784),
            visitedOutline = Color(0xFFA5D6A7),
            unvisitedFill = Color(0x14FFFFFF),
            border = Color(0xFF9E9E9E),
            youAreHere = Color(0xFF64B5F6),
        )
    }
}
