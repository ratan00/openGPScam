package org.fossify.camera.stamp

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
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

    class Snapshot(val data: StampData, val minimap: Bitmap?)

    var listener: Listener? = null

    private val resolver = AddressResolver(context)
    private val minimapRenderer = MinimapRenderer(context, userAgent)

    @Volatile private var addressKey: String? = null
    @Volatile private var minimap: Bitmap? = null
    private var minimapLocation: Location? = null
    @Volatile private var renderingMap = false

    fun start() {
        locations.onLocationUpdate = ::onFix
        locations.getLocation()?.let(::onFix)
        listener?.onStampInputsChanged()
    }

    fun stop() {
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
        return StampData(
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracyMeters = location?.takeIf { it.hasAccuracy() }?.accuracy,
            altitudeMeters = location?.takeIf { it.hasAltitude() }?.altitude,
            // Only an address resolved for this very spot; a stale one would be misleading.
            address = location?.let { resolver.cached(it.latitude, it.longitude) },
            time = now,
            isStale = locations.isStale(location),
        )
    }

    fun currentMinimap(): Bitmap? = minimap

    /** Taken at the shutter press. */
    fun snapshot(): Snapshot = Snapshot(currentData(), minimap)

    companion object {
        private const val MAP_REFRESH_DISTANCE_M = 20f
    }
}
