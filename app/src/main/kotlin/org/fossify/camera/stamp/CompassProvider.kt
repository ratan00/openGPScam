package org.fossify.camera.stamp

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Compass heading of the direction the camera points at, in degrees from true north (0..360).
 * Uses the rotation-vector sensor (no permission needed), smoothed so the number doesn't jitter.
 */
class CompassProvider(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotation = FloatArray(MATRIX_SIZE)
    private val remapped = FloatArray(MATRIX_SIZE)
    private val orientation = FloatArray(ORIENTATION_SIZE)

    // Smoothed on the unit circle so 359° -> 1° doesn't average to 180°.
    private var sinAvg = 0f
    private var cosAvg = 0f
    @Volatile private var hasReading = false
    @Volatile private var magneticHeading = 0f
    @Volatile private var lastReadingMs = 0L

    val isAvailable: Boolean get() = rotationSensor != null

    fun start() {
        rotationSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        hasReading = false
    }

    /** True-north heading, or null without a recent sensor reading. [location] supplies the declination. */
    fun headingDegrees(location: Location?): Float? {
        if (!hasReading || System.currentTimeMillis() - lastReadingMs > MAX_AGE_MS) return null
        var heading = magneticHeading
        if (location != null) {
            val field = GeomagneticField(
                location.latitude.toFloat(), location.longitude.toFloat(),
                if (location.hasAltitude()) location.altitude.toFloat() else 0f, System.currentTimeMillis()
            )
            heading += field.declination
        }
        return normalize(heading)
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        // Flat on a table the camera looks down, so use the phone's top edge; upright, use the lens axis.
        val flat = abs(rotation[FLAT_COMPONENT]) > FLAT_THRESHOLD
        val matrix = if (flat) {
            rotation
        } else {
            SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped)
            remapped
        }
        SensorManager.getOrientation(matrix, orientation)
        val radians = orientation[0].toDouble()
        if (!hasReading) {
            sinAvg = sin(radians).toFloat()
            cosAvg = cos(radians).toFloat()
        } else {
            sinAvg += SMOOTHING * (sin(radians).toFloat() - sinAvg)
            cosAvg += SMOOTHING * (cos(radians).toFloat() - cosAvg)
        }
        magneticHeading = Math.toDegrees(atan2(sinAvg, cosAvg).toDouble()).toFloat()
        lastReadingMs = System.currentTimeMillis()
        hasReading = true
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val MATRIX_SIZE = 9
        private const val ORIENTATION_SIZE = 3
        private const val FLAT_COMPONENT = 8
        private const val FLAT_THRESHOLD = 0.8f
        private const val SMOOTHING = 0.15f
        private const val MAX_AGE_MS = 3000L

        fun normalize(degrees: Float): Float = ((degrees % FULL_CIRCLE) + FULL_CIRCLE) % FULL_CIRCLE

        private const val FULL_CIRCLE = 360f
    }
}
