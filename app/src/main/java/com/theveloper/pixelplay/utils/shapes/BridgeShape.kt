package com.theveloper.pixelplay.utils.shapes

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * A custom shape that creates a "bridge" between components.
 * It features a wider base at the ends and a slightly narrowed center,
 * creating an expressive Material 3 look.
 */
class BridgeShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val r = h / 2
            val inset = h * 0.15f // Subtle narrowing in the middle

            // Start at top left arc
            moveTo(0f, r)
            arcTo(Rect(0f, 0f, h, h), 180f, 90f, false)

            // Bridge top: curves slightly inward
            cubicTo(w * 0.25f, 0f, w * 0.4f, inset, w * 0.5f, inset)
            cubicTo(w * 0.6f, inset, w * 0.75f, 0f, w - r, 0f)

            // Top right arc
            arcTo(Rect(w - h, 0f, w, h), 270f, 90f, false)

            // Bottom right arc
            arcTo(Rect(w - h, 0f, w, h), 0f, 90f, false)

            // Bridge bottom: curves slightly inward
            cubicTo(w * 0.75f, h, w * 0.6f, h - inset, w * 0.5f, h - inset)
            cubicTo(w * 0.4f, h - inset, w * 0.25f, h, r, h)

            // Bottom left arc
            arcTo(Rect(0f, 0f, h, h), 90f, 90f, false)

            close()
        }
        return Outline.Generic(path)
    }
}
