package org.fossify.camera.stamp

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.io.IOException
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Reverse geocodes coordinates using the platform [Geocoder] (no Play Services).
 * Results are cached by coordinates rounded to ~4 decimals (about 11 m).
 * Returns null when offline or no address is available, so the stamp falls back to coordinates.
 */
class AddressResolver(context: Context) {

    private val geocoder = Geocoder(context.applicationContext, Locale.getDefault())
    private val cache = object : LinkedHashMap<String, String?>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String?>?) = size > MAX_CACHE
    }

    /** Blocking; call from a background thread only. */
    fun resolve(latitude: Double, longitude: Double, timeoutMs: Long = TIMEOUT_MS): String? {
        if (!Geocoder.isPresent()) {
            return null
        }

        val key = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
        synchronized(cache) {
            if (cache.containsKey(key)) {
                return cache[key]
            }
        }

        val address = fetch(latitude, longitude, timeoutMs)?.let(::format)
        if (address != null) {
            synchronized(cache) { cache[key] = address }
        }
        return address
    }

    @Suppress("DEPRECATION")
    private fun fetch(latitude: Double, longitude: Double, timeoutMs: Long): Address? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var result: Address? = null
                val latch = CountDownLatch(1)
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        result = addresses.firstOrNull()
                        latch.countDown()
                    }

                    override fun onError(errorMessage: String?) {
                        latch.countDown()
                    }
                })
                latch.await(timeoutMs, TimeUnit.MILLISECONDS)
                result
            } else {
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun format(address: Address): String? {
        val full = (0..address.maxAddressLineIndex).mapNotNull { address.getAddressLine(it) }
            .joinToString(", ")
        return full.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val MAX_CACHE = 32
        const val TIMEOUT_MS = 3000L
    }
}
