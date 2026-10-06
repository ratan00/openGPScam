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
) {
    val hasFix: Boolean get() = latitude != null && longitude != null
}
