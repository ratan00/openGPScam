package org.fossify.camera.stamp

/** User-facing options that control how the strip looks. */
data class StampSettings(
    val showMap: Boolean = true,
    val showAddress: Boolean = true,
    val showCoordinates: Boolean = true,
    val showDateTime: Boolean = true,
    val use24Hour: Boolean = true,
    val useDms: Boolean = false,
    val atTop: Boolean = false,
    val opacityPercent: Int = DEFAULT_OPACITY,
) {
    companion object {
        const val DEFAULT_OPACITY = 55
        val OPACITY_CHOICES = listOf(100, 85, 70, DEFAULT_OPACITY, 40, 25)
    }
}
