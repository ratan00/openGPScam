package org.fossify.camera.stamp

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * Reverse-geocodes coordinates with the platform [Geocoder]. Results are cached by coordinates
 * rounded to 4 decimals (~11 m). Offline or on failure the callback receives null.
 */
class AddressResolver(context: Context) {

    private val geocoder = Geocoder(context.applicationContext, Locale.getDefault())
    private val cache = ConcurrentHashMap<String, String>()

    /** Returns a cached address without any lookup, or null. */
    fun cached(latitude: Double, longitude: Double): String? = cache[key(latitude, longitude)]

    fun resolve(latitude: Double, longitude: Double, callback: (String?) -> Unit) {
        val key = key(latitude, longitude)
        cache[key]?.let { return callback(it) }

        if (!Geocoder.isPresent()) return callback(null)

        val onResult = { addresses: List<Address>? ->
            val text = addresses?.firstOrNull()?.let(::format)
            if (text != null) cache[key] = text
            callback(text)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = onResult(addresses)
                    override fun onError(errorMessage: String?) = onResult(null)
                })
            } else {
                // The synchronous API blocks, so keep it off the caller's thread.
                Thread {
                    @Suppress("DEPRECATION")
                    val result = try {
                        geocoder.getFromLocation(latitude, longitude, 1)
                    } catch (_: java.io.IOException) {
                        null
                    }
                    onResult(result)
                }.start()
            }
        } catch (_: IllegalArgumentException) {
            callback(null)
        }
    }

    private fun format(address: Address): String? {
        val lines = (0..address.maxAddressLineIndex).mapNotNull { address.getAddressLine(it) }
        return lines.joinToString(", ").ifBlank { null }
    }

    companion object {
        private const val KEY_SCALE = 10_000.0

        fun key(latitude: Double, longitude: Double): String =
            "${(latitude * KEY_SCALE).roundToLong()},${(longitude * KEY_SCALE).roundToLong()}"
    }
}
