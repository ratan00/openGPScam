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

    /** The text lines in drawing order: headline, address, coordinates (+accuracy), date/time (+ altitude and heading). */
    fun lines(data: StampData, settings: StampSettings, locale: Locale = Locale.getDefault()): StampLines {
        val address = if (settings.showAddress) data.address else null
        val headline = if (settings.showAddress) data.headline else null
        val coords = if (settings.showCoordinates) {
            if (data.latitude != null && data.longitude != null) {
                buildString {
                    append(coordinates(data.latitude, data.longitude, settings.useDms))
                    accuracy(data.accuracyMeters)?.let { append("  ").append(it) }
                    if (data.isStale) append("  (last known)")
                    if (data.isMock) append("  ⚠ MOCK LOCATION")
                }
            } else {
                "Location unavailable"
            }
        } else {
            null
        }
        val orientation = orientation(data, settings)
        val time = if (settings.showDateTime) {
            buildString {
                append(dateTime(data.time, settings.use24Hour, locale))
                clockWarning(data, settings.use24Hour, locale)?.let { append("  ").append(it) }
                orientation?.let { append("  ").append(it) }
            }
        } else {
            orientation
        }
        val remarks = settings.remarks.trim().ifEmpty { null }
        return StampLines(address, coords, time, headline, remarks)
    }

    private val COMPASS_POINTS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    /** "NE 42°" for a heading in degrees from north. */
    fun heading(degrees: Float): String {
        val normalized = ((degrees % 360f) + 360f) % 360f
        val point = COMPASS_POINTS[((normalized + 22.5f) / 45f).toInt() % COMPASS_POINTS.size]
        return String.format(Locale.US, "%s %d°", point, Math.round(normalized) % 360)
    }

    /** "Alt 215 m · NE 42°", with whichever parts are enabled and available; null when neither is. */
    fun orientation(data: StampData, settings: StampSettings): String? {
        val parts = listOfNotNull(
            data.altitudeMeters.takeIf { settings.showAltitude }
                ?.let { String.format(Locale.US, "Alt %d m", Math.round(it)) },
            data.headingDegrees.takeIf { settings.showCompass }?.let(::heading),
        )
        return parts.joinToString(" · ").ifEmpty { null }
    }

    /** "⚠ GPS time 22:51" when the phone clock disagrees with GPS time by more than the tolerance. */
    fun clockWarning(data: StampData, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String? {
        val skew = data.clockSkewMs ?: return null
        if (abs(skew) <= CLOCK_TOLERANCE_MS) return null
        val gpsTime = data.time.minusNanos(skew * 1_000_000)
        val pattern = if (use24Hour) "dd MMM HH:mm" else "dd MMM hh:mm a"
        return "⚠ GPS time " + DateTimeFormatter.ofPattern(pattern, locale).format(gpsTime)
    }

    private const val CLOCK_TOLERANCE_MS = 2 * 60 * 1000L

    /** Google Maps link to the photo's coordinates, for the QR code; null without a fix. */
    fun qrText(data: StampData): String? {
        if (data.latitude == null || data.longitude == null) return null
        return String.format(
            Locale.US,
            "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f",
            data.latitude, data.longitude
        )
    }

    /**
     * File name without extension: Organisation_City_2026-10-06_224320, skipping missing parts.
     * Only letters, digits and dashes are kept so it is safe on every file system.
     */
    fun fileName(data: StampData, settings: StampSettings): String {
        fun clean(s: String?) = s.orEmpty()
            .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
            .trim('-')
            .take(FILE_NAME_PART_MAX)
        val time = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss", Locale.US).format(data.time)
        return listOf(clean(settings.organisation), clean(data.city), time)
            .filter { it.isNotEmpty() }
            .joinToString("_")
    }

    private const val FILE_NAME_PART_MAX = 32

    /** Single-line summary used for the EXIF ImageDescription. */
    fun summary(data: StampData, settings: StampSettings): String {
        val l = lines(data, settings.copy(showAddress = true, showCoordinates = true, showDateTime = true))
        return listOfNotNull(l.address, l.coordinates, l.dateTime).joinToString(" | ")
    }
}

data class StampLines(
    val address: String?,
    val coordinates: String?,
    val dateTime: String?,
    val headline: String? = null,
    val remarks: String? = null,
) {
    val isEmpty: Boolean
        get() = headline == null && address == null && coordinates == null && dateTime == null && remarks == null
}
