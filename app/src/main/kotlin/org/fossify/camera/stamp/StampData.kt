package org.fossify.camera.stamp

import java.time.ZonedDateTime

/**
 * Immutable snapshot of everything printed on a photo, taken at the moment of the shutter press.
 */
data class StampData(
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val address: String?,
    val time: ZonedDateTime,
    val isStale: Boolean,
    /** Short "City, State, Country" line shown large above the full address. */
    val headline: String? = null,
    /** City (or district) alone, used for file names. */
    val city: String? = null,
    /** The fix came from a mock-location app. */
    val isMock: Boolean = false,
    /** Phone clock minus GPS time; null when there is no recent GPS fix to compare with. */
    val clockSkewMs: Long? = null,
    /** Direction the camera pointed at, degrees from true north; null without a compass reading. */
    val headingDegrees: Float? = null,
) {
    val hasFix: Boolean get() = latitude != null && longitude != null
}
