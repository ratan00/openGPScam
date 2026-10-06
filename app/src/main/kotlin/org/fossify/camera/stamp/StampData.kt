package org.fossify.camera.stamp

import java.time.ZonedDateTime

/**
 * Immutable snapshot taken at the moment of the shutter press.
 */
data class StampData(
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val address: String?,
    val time: ZonedDateTime,
    val isStale: Boolean = false,
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
}
