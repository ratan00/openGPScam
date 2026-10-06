package org.fossify.camera.stamp

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Sizes for the strip, all derived from the image's short edge so the label looks the same at
 * 12 MP and 50 MP.
 */
data class StampLayout(
    val padding: Float,
    val textSize: Float,
    val smallTextSize: Float,
    val attributionSize: Float,
    val minMapSize: Float,
    val cornerRadius: Float,
) {
    companion object {
        fun forImage(width: Int, height: Int): StampLayout {
            val short = minOf(width, height).toFloat()
            val text = short * 0.034f
            return StampLayout(
                padding = short * 0.022f,
                textSize = text,
                smallTextSize = text * 0.85f,
                attributionSize = text * 0.5f,
                minMapSize = short * 0.16f,
                cornerRadius = short * 0.008f,
            )
        }

        /** Maximum share of the image width that the map may take. */
        const val MAX_MAP_WIDTH_FRACTION = 0.3f
    }

    /** Square map side for a given text block height, clamped to the image width. */
    fun mapSize(contentHeight: Float, imageWidth: Int): Int {
        val wanted = max(contentHeight, minMapSize)
        return wanted.coerceAtMost(imageWidth * MAX_MAP_WIDTH_FRACTION).roundToInt()
    }
}
