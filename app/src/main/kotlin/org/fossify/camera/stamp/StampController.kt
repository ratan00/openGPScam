package org.fossify.camera.stamp

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import org.fossify.camera.helpers.SimpleLocationManager
import java.time.ZonedDateTime

/**
 * Keeps the label inputs warm while the camera is open: the latest fix, its address and a
 * pre-rendered minimap, so taking a photo never waits on the network.
 */
class StampController(
    context: Context,
    private val locations: SimpleLocationManager,
    private val showMap: () -> Boolean,
    userAgent: String,
) {

    fun interface Listener {
        fun onStampInputsChanged()
    }

    class Snapshot(val data: StampData, val minimap: Bitmap?, val watermarkIcon: Bitmap?)

    var listener: Listener? = null

    private val resolver = AddressResolver(context)

    /** App icon for the watermark, rendered once. */
    val watermarkIcon: Bitmap? = try {
        context.packageManager.getApplicationIcon(context.applicationInfo)
            .toBitmap(WATERMARK_ICON_PX, WATERMARK_ICON_PX)
    } catch (_: Exception) {
        null
    }
    private val minimapRenderer = MinimapRenderer(context, userAgent)
    private val compass = CompassProvider(context)

    @Volatile private var addressKey: String? = null
    @Volatile private var minimap: Bitmap? = null
    private var minimapLocation: Location? = null
    @Volatile private var renderingMap = false

    fun start() {
        compass.start()
        locations.onLocationUpdate = ::onFix
        locations.getLocation()?.let(::onFix)
        listener?.onStampInputsChanged()
    }

    fun stop() {
        compass.stop()
        locations.onLocationUpdate = null
    }

    private fun onFix(location: Location) {
        val key = AddressResolver.key(location.latitude, location.longitude)
        if (key != addressKey) {
            resolver.resolve(location.latitude, location.longitude) { text ->
                if (text != null) {
                    addressKey = key
                    listener?.onStampInputsChanged()
                }
            }
        }

        val last = minimapLocation
        val moved = last == null || location.distanceTo(last) > MAP_REFRESH_DISTANCE_M
        if (showMap() && moved && !renderingMap) {
            renderingMap = true
            Thread {
                val bmp = minimapRenderer.render(location.latitude, location.longitude)
                if (bmp != null) {
                    minimap = bmp
                    minimapLocation = location
                }
                renderingMap = false
                listener?.onStampInputsChanged()
            }.start()
        }
        listener?.onStampInputsChanged()
    }

    /** Current label data. Never blocks; uses the last known fix if there is no recent one. */
    fun currentData(now: ZonedDateTime = ZonedDateTime.now()): StampData {
        val location = locations.getLocation()
        // Only an address resolved for this very spot; a stale one would be misleading.
        val resolved = location?.let { resolver.cached(it.latitude, it.longitude) }
        return StampData(
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracyMeters = location?.takeIf { it.hasAccuracy() }?.accuracy,
            altitudeMeters = location?.let(::altitudeOf),
            address = resolved?.full,
            time = now,
            isStale = locations.isStale(location),
            headline = resolved?.headline,
            city = resolved?.city,
            isMock = SimpleLocationManager.isMock(location),
            clockSkewMs = locations.clockSkewMs(),
            headingDegrees = compass.headingDegrees(location),
        )
    }

    /** Height above sea level where the platform knows it (API 34+), else the raw GPS altitude. */
    private fun altitudeOf(location: Location): Double? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && location.hasMslAltitude() ->
            location.mslAltitudeMeters
        location.hasAltitude() -> location.altitude
        else -> null
    }

    fun currentMinimap(): Bitmap? = minimap

    /** Requests a brand-new fix; [callback] gets it (or null) on the main thread. */
    fun refreshLocation(callback: (Location?) -> Unit) {
        // Redraw the map for the fresh position even if it barely moved.
        minimapLocation = null
        locations.requestFreshFix(callback)
    }

    /** Taken at the shutter press. */
    fun snapshot(): Snapshot = Snapshot(currentData(), minimap, watermarkIcon)

    companion object {
        private const val MAP_REFRESH_DISTANCE_M = 20f
        private const val WATERMARK_ICON_PX = 192
    }
}
