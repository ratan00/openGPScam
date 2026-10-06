@file:Suppress("MagicNumber") // layout ratios and unit conversions

package org.fossify.camera.stamp

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Sizes for the floating card, all derived from the image's short edge so the label
 * looks the same at 12 MP and 50 MP.
 */
data class StampLayout(
    /** Distance of the tiles from the image edges. */
    val margin: Float,
    /** Space between the card and the watermark / officer card. */
    val gap: Float,
    val padding: Float,
    val headlineSize: Float,
    val textSize: Float,
    val smallTextSize: Float,
    val attributionSize: Float,
    val watermarkSize: Float,
    val minMapSize: Float,
    val cornerRadius: Float,
) {
    companion object {
        fun forImage(width: Int, height: Int): StampLayout {
            val short = minOf(width, height).toFloat()
            val text = short * 0.027f
            return StampLayout(
                margin = short * 0.03f,
                gap = short * 0.015f,
                padding = short * 0.024f,
                headlineSize = short * 0.042f,
                textSize = text,
                smallTextSize = text * 0.92f,
                attributionSize = text * 0.62f,
                watermarkSize = short * 0.022f,
                minMapSize = short * 0.19f,
                cornerRadius = short * 0.03f,
            )
        }

        /** Maximum share of the image width that the map may take. */
        const val MAX_MAP_WIDTH_FRACTION = 0.3f

        /** Per-tile maximum when both the map and the QR code are shown. */
        const val MAX_TWO_TILE_FRACTION = 0.22f

        /** Maximum share of the image width for the officer card. */
        const val MAX_OFFICER_WIDTH_FRACTION = 0.6f
    }

    /** Square map side for a given text block height, clamped to the image width. */
    fun mapSize(contentHeight: Float, imageWidth: Int): Int {
        val wanted = max(contentHeight, minMapSize)
        return wanted.coerceAtMost(imageWidth * MAX_MAP_WIDTH_FRACTION).roundToInt()
    }
}
