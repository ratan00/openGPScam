package org.fossify.camera.stamp

import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Pure text formatting for the stamp strip, kept free of Android classes so it can be unit tested.
 */
object StampFormatter {

    fun coordinates(data: StampData): String {
        val lat = data.latitude
        val lng = data.longitude
        if (lat == null || lng == null) {
            return "Location unavailable"
        }

        val text = "Lat ${degrees(lat)} Long ${degrees(lng)}"
        val accuracy = data.accuracyMeters
        return if (accuracy != null && accuracy > 0f) {
            "$text (±${accuracy.toInt()} m)"
        } else {
            text
        }
    }

    fun dateTime(data: StampData, use24Hour: Boolean = true): String {
        val pattern = if (use24Hour) "EEE, dd MMM yyyy HH:mm z" else "EEE, dd MMM yyyy hh:mm a z"
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(data.time)
    }

    fun lines(data: StampData, use24Hour: Boolean = true): List<String> {
        return listOfNotNull(
            data.address?.takeIf { it.isNotBlank() },
            coordinates(data),
            dateTime(data, use24Hour),
        )
    }

    private fun degrees(value: Double): String {
        return String.format(Locale.US, "%.4f°", abs(value)).let {
            if (value < 0) "-$it" else it
        }
    }
}
