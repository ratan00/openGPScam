package org.fossify.camera.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class StampFormatterTest {

    private val time = ZonedDateTime.of(2026, 10, 6, 19, 26, 0, 0, ZoneId.of("Asia/Kolkata"))

    private fun data(
        lat: Double? = 28.6315,
        lng: Double? = 77.2167,
        accuracy: Float? = null,
        address: String? = "Connaught Place, New Delhi",
        stale: Boolean = false,
    ) = StampData(lat, lng, accuracy, null, address, time, stale)

    @Test
    fun decimalCoordinates() {
        assertEquals("Lat 28.6315° Long 77.2167°", StampFormatter.coordinates(28.6315, 77.2167, false))
        assertEquals("Lat -33.8688° Long -151.2093°", StampFormatter.coordinates(-33.8688, -151.2093, false))
    }

    @Test
    fun dmsCoordinates() {
        assertEquals(
            "Lat 28°37'53.4\"N Long 77°13'00.1\"E",
            StampFormatter.coordinates(28.6315, 77.2167, true)
        )
        assertEquals(
            "Lat 33°52'07.7\"S Long 151°12'33.5\"W",
            StampFormatter.coordinates(-33.8688, -151.2093, true)
        )
    }

    @Test
    fun dmsRoundingNeverShows60Seconds() {
        // 0.99999999° is 59'59.99996" and must carry into the next degree.
        assertEquals("Lat 1°00'00.0\"N Long 0°00'00.0\"E", StampFormatter.coordinates(0.99999999, 0.0, true))
    }

    @Test
    fun dateTime24And12Hour() {
        assertEquals("Tue, 06 Oct 2026 19:26 IST", StampFormatter.dateTime(time, true, Locale.US))
        assertEquals("Tue, 06 Oct 2026 07:26 PM IST", StampFormatter.dateTime(time, false, Locale.US))
    }

    @Test
    fun accuracyRoundsToMeters() {
        assertEquals("±12 m", StampFormatter.accuracy(12.4f))
        assertNull(StampFormatter.accuracy(null))
    }

    @Test
    fun linesIncludeAccuracyAndStaleMarker() {
        val lines = StampFormatter.lines(data(accuracy = 8f, stale = true), StampSettings(), Locale.US)
        assertEquals("Connaught Place, New Delhi", lines.address)
        assertEquals("Lat 28.6315° Long 77.2167°  ±8 m  (last known)", lines.coordinates)
    }

    @Test
    fun linesWithoutFixSayLocationUnavailable() {
        val lines = StampFormatter.lines(data(lat = null, lng = null, address = null), StampSettings(), Locale.US)
        assertNull(lines.address)
        assertEquals("Location unavailable", lines.coordinates)
    }

    @Test
    fun disabledFieldsAreOmitted() {
        val settings = StampSettings(showAddress = false, showCoordinates = false, showDateTime = false)
        assertTrue(StampFormatter.lines(data(), settings, Locale.US).isEmpty)
    }

    @Test
    fun summaryAlwaysHasEverything() {
        val summary = StampFormatter.summary(data(), StampSettings(showAddress = false))
        assertTrue(summary.startsWith("Connaught Place, New Delhi | Lat 28.6315°"))
    }
}
