package org.fossify.camera.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import java.util.concurrent.TimeUnit
import org.fossify.camera.extensions.checkLocationPermission
import org.fossify.commons.activities.BaseSimpleActivity

class SimpleLocationManager(private val activity: BaseSimpleActivity) {

    companion object {
        private const val LOCATION_UPDATE_MIN_TIME_INTERVAL_MS = 5000L
        private const val LOCATION_UPDATE_MIN_DISTANCE_M = 10F
        const val STALE_AFTER_MS = 2 * 60 * 1000L

        /** Phone clock vs GPS time differences below this are ignored. */
        const val CLOCK_TOLERANCE_MS = 2 * 60 * 1000L

        /** GPS time is only trusted from fixes this recent. */
        private const val GPS_TIME_MAX_AGE_MS = 10 * 60 * 1000L

        /** True when the fix came from a mock-location app. */
        fun isMock(location: Location?): Boolean {
            location ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                location.isMock
            } else {
                @Suppress("DEPRECATION")
                location.isFromMockProvider
            }
        }

        private fun ageMs(location: Location) =
            TimeUnit.NANOSECONDS.toMillis(SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos)
    }

    private var location: Location? = null

    /** Newest fix from the GPS chip itself; its time comes from the satellites, not the phone. */
    private var lastGpsFix: Location? = null

    /** Called on the main thread for each new fix from the system. */
    var onLocationUpdate: ((Location) -> Unit)? = null

    private val locationManager = activity.getSystemService(LocationManager::class.java)!!

    @Suppress("EmptyFunctionBlock")
    private val locationListener = object: LocationListener {
        override fun onLocationChanged(location: Location) = onNewFix(location)

        // No-op methods that must be overridden.
        // See https://github.com/FossifyOrg/Camera/issues/177
        @Suppress("DEPRECATION")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun onNewFix(location: Location) {
        this.location = location
        if (location.provider == LocationManager.GPS_PROVIDER && !isMock(location)) lastGpsFix = location
        onLocationUpdate?.invoke(location)
    }

    /**
     * How far the phone's clock is ahead of GPS time (negative = behind), or null when there is no
     * recent genuine GPS fix to compare with.
     */
    fun clockSkewMs(): Long? {
        val gps = lastGpsFix ?: return null
        val age = ageMs(gps)
        if (age > GPS_TIME_MAX_AGE_MS) return null
        return System.currentTimeMillis() - (gps.time + age)
    }

    /**
     * Asks GPS and network location for brand-new fixes at the same time and reports the first one
     * that arrives (indoors GPS often can't get one at all). [callback] runs once, on the main
     * thread, with null only when every provider came back empty.
     */
    @SuppressLint("MissingPermission")
    fun requestFreshFix(callback: (Location?) -> Unit) {
        if (!activity.checkLocationPermission()) return callback(null)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { it in locationManager.allProviders && locationManager.isProviderEnabled(it) }
        if (providers.isEmpty()) return callback(null)

        var pending = providers.size
        var delivered = false
        val onResult = { fix: Location? ->
            pending--
            if (fix != null) onNewFix(fix)
            if (!delivered && (fix != null || pending == 0)) {
                delivered = true
                callback(fix)
            }
        }
        providers.forEach { provider ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(provider, null, activity.mainExecutor) { onResult(it) }
            } else {
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(provider, { onResult(it) }, activity.mainLooper)
            }
        }
    }

    fun getLocation(): Location? {
        if (location == null) {
            location = getLastKnownLocation()
        }

        return location
    }

    /** True when there is no fix, or the newest one is older than [STALE_AFTER_MS]. */
    fun isStale(location: Location? = getLocation()): Boolean {
        location ?: return true
        return ageMs(location) > STALE_AFTER_MS
    }

    private fun getLastKnownLocation(): Location? {
        return if (activity.checkLocationPermission()) {
            var accurateLocation: Location? = null
            for (provider in locationManager.allProviders) {
                val location = locationManager.getLastKnownLocation(provider) ?: continue
                if (accurateLocation == null || location.accuracy < accurateLocation.accuracy) {
                    accurateLocation = location
                }
            }
            accurateLocation
        } else {
            null
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun requestLocationUpdates() {
        locationManager.allProviders.forEach { provider ->
            locationManager.requestLocationUpdates(
                provider,
                LOCATION_UPDATE_MIN_TIME_INTERVAL_MS,
                LOCATION_UPDATE_MIN_DISTANCE_M,
                locationListener
            )
        }
    }

    fun dropLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }
}
