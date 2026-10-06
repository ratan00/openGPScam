package org.fossify.camera.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StampLayoutTest {

    @Test
    fun scalesWithShortEdgeNotOrientation() {
        val portrait = StampLayout.forImage(3000, 4000)
        val landscape = StampLayout.forImage(4000, 3000)
        assertEquals(portrait.textSize, landscape.textSize, 0.001f)
    }

    @Test
    fun sameRelativeSizeAt12And50Megapixels() {
        val small = StampLayout.forImage(3000, 4000)
        val large = StampLayout.forImage(6000, 8000)
        assertEquals(small.textSize / 3000, large.textSize / 6000, 0.0001f)
    }

    @Test
    fun mapIsAtLeastMinSizeAndCappedToImageWidth() {
        val layout = StampLayout.forImage(3000, 4000)
        assertTrue(layout.mapSize(10f, 3000) >= layout.minMapSize.toInt())
        assertEquals(900, layout.mapSize(5000f, 3000))
    }
}
