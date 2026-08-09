package dev.doug.globetraveler.design

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified

/**
 * Fades [color] over the content's top [topHeight] and bottom [bottomHeight] edges — solid at
 * the edge, transparent at the given distance. Pass [Dp.Unspecified] to skip an edge.
 * (Ported from WorldClock's fadeInOut.)
 */
fun Modifier.fadeInOut(
    color: Color,
    topHeight: Dp = Dp.Unspecified,
    bottomHeight: Dp = Dp.Unspecified,
) = this.then(
    Modifier.drawWithContent {
        drawContent()

        val colorStops = mutableListOf<Pair<Float, Color>>()

        // Top
        if (topHeight.isSpecified) {
            colorStops.add(0f to color)
            colorStops.add(topHeight.toPx() / size.height to Color.Transparent)
        } else {
            colorStops.add(0f to Color.Transparent)
        }

        // Bottom
        if (bottomHeight.isSpecified) {
            colorStops.add((size.height - bottomHeight.toPx()) / size.height to Color.Transparent)
            colorStops.add(1f to color)
        } else {
            colorStops.add(1f to Color.Transparent)
        }

        val brush = Brush.linearGradient(
            *colorStops.toTypedArray(),
            start = Offset(0f, 0f),
            end = Offset(0f, size.height),
            tileMode = TileMode.Clamp,
        )
        drawRect(
            brush = brush,
            blendMode = BlendMode.SrcOver,
        )
    },
)
