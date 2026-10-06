package org.fossify.camera.stamp

enum class QrPlacement { OFF, INSTEAD_OF_MAP, BESIDE_MAP }

/** User-facing options that control how the strip looks. */
data class StampSettings(
    val showMap: Boolean = true,
    val showAddress: Boolean = true,
    val showCoordinates: Boolean = true,
    val showDateTime: Boolean = true,
    val showAltitude: Boolean = true,
    val showCompass: Boolean = true,
    val use24Hour: Boolean = true,
    val useDms: Boolean = false,
    val atTop: Boolean = false,
    val opacityPercent: Int = DEFAULT_OPACITY,
    /** App icon + name above the card. */
    val showWatermark: Boolean = true,
    val appName: String = "OpenGPS Cam",
    /** Optional top-left card; hidden when both are blank. */
    val officerName: String = "",
    val designation: String = "",
    val organisation: String = "",
    /** Free text such as a case or site number, printed as its own line. */
    val remarks: String = "",
    /** QR code with the stamp data, so edits to the printed text can be spotted. */
    val qrPlacement: QrPlacement = QrPlacement.OFF,
    /** Live preview only: shown in the officer card while all its fields are blank. */
    val officerPlaceholder: String? = null,
) {
    companion object {
        const val DEFAULT_OPACITY = 55
        val OPACITY_CHOICES = listOf(100, 85, 70, DEFAULT_OPACITY, 40, 25)
    }
}
