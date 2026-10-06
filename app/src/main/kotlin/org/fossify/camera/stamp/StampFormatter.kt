@file:Suppress("MagicNumber") // layout ratios and unit conversions

package org.fossify.camera.stamp

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Pure text formatting for the strip; no Android dependencies so it is unit-testable. */
object StampFormatter {

    fun coordinates(latitude: Double, longitude: Double, dms: Boolean): String {
        return if (dms) {
            "Lat ${dms(latitude, 'N', 'S')} Long ${dms(longitude, 'E', 'W')}"
        } else {
            String.format(Locale.US, "Lat %.4f° Long %.4f°", latitude, longitude)
        }
    }

    private fun dms(value: Double, positive: Char, negative: Char): String {
        val abs = abs(value)
        var degrees = abs.toInt()
        var minutes = ((abs - degrees) * 60).toInt()
        var seconds = Math.round(((abs - degrees) * 60 - minutes) * 60 * 10) / 10.0
        if (seconds >= 60.0) {
            seconds = 0.0
            minutes++
        }
        if (minutes >= 60) {
            minutes = 0
            degrees++
        }
        val hemisphere = if (value >= 0) positive else negative
        return String.format(Locale.US, "%d°%02d'%04.1f\"%c", degrees, minutes, seconds, hemisphere)
    }

    fun dateTime(time: ZonedDateTime, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val pattern = if (use24Hour) "EEE, dd MMM yyyy HH:mm z" else "EEE, dd MMM yyyy hh:mm a z"
        return DateTimeFormatter.ofPattern(pattern, locale).format(time)
    }

    fun accuracy(meters: Float?): String? {
        meters ?: return null
        return String.format(Locale.US, "±%d m", Math.round(meters))
    }

    /** The text lines in drawing order: address, coordinates (+accuracy), date/time. */
    fun lines(data: StampData, settings: StampSettings, locale: Locale = Locale.getDefault()): StampLines {
        val address = if (settings.showAddress) data.address else null
        val coords = if (settings.showCoordinates) {
            if (data.latitude != null && data.longitude != null) {
                buildString {
                    append(coordinates(data.latitude, data.longitude, settings.useDms))
                    accuracy(data.accuracyMeters)?.let { append("  ").append(it) }
                    if (data.isStale) append("  (last known)")
                }
            } else {
                "Location unavailable"
            }
        } else {
            null
        }
        val time = if (settings.showDateTime) dateTime(data.time, settings.use24Hour, locale) else null
        return StampLines(address, coords, time)
    }

    /** Single-line summary used for the EXIF ImageDescription. */
    fun summary(data: StampData, settings: StampSettings): String {
        val l = lines(data, settings.copy(showAddress = true, showCoordinates = true, showDateTime = true))
        return listOfNotNull(l.address, l.coordinates, l.dateTime).joinToString(" | ")
    }
}

data class StampLines(val address: String?, val coordinates: String?, val dateTime: String?) {
    val isEmpty: Boolean get() = address == null && coordinates == null && dateTime == null
}
