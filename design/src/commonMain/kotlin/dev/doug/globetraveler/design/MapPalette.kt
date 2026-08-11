package dev.doug.globetraveler.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import dev.doug.globetraveler.domain.MapAccent

/** Colors for map region fills, with a variant per basemap (light Positron, near-black Dark). */
@Immutable
data class MapPalette(
    val unvisitedFill: Color,
    val border: Color,
    val youAreHere: Color,
) {
    companion object {
        val Light = MapPalette(
            unvisitedFill = Color(0x14000000),
            border = Color(0xFF757575),
            youAreHere = Color(0xFF1E88E5),
        )

        val Dark = MapPalette(
            unvisitedFill = Color(0x14FFFFFF),
            border = Color(0xFF9E9E9E),
            youAreHere = Color(0xFF64B5F6),
        )
    }
}

/** Visited fill/outline pair for a tracked map's accent. */
@Immutable
data class AccentColors(val fill: Color, val outline: Color)

/** Green matches the original visited palette; the rest follow the same 700/300 pattern. */
fun MapAccent.accentColors(dark: Boolean): AccentColors = when (this) {
    MapAccent.Green ->
        if (dark) AccentColors(Color(0xFF81C784), Color(0xFFA5D6A7))
        else AccentColors(Color(0xFF2E7D32), Color(0xFF1B5E20))
    MapAccent.Blue ->
        if (dark) AccentColors(Color(0xFF64B5F6), Color(0xFF90CAF9))
        else AccentColors(Color(0xFF1565C0), Color(0xFF0D47A1))
    MapAccent.Orange ->
        if (dark) AccentColors(Color(0xFFFFB74D), Color(0xFFFFCC80))
        else AccentColors(Color(0xFFEF6C00), Color(0xFFE65100))
    MapAccent.Purple ->
        if (dark) AccentColors(Color(0xFFBA68C8), Color(0xFFCE93D8))
        else AccentColors(Color(0xFF6A1B9A), Color(0xFF4A148C))
    MapAccent.Red ->
        if (dark) AccentColors(Color(0xFFE57373), Color(0xFFEF9A9A))
        else AccentColors(Color(0xFFC62828), Color(0xFFB71C1C))
    MapAccent.Teal ->
        if (dark) AccentColors(Color(0xFF4DB6AC), Color(0xFF80CBC4))
        else AccentColors(Color(0xFF00695C), Color(0xFF004D40))
}
