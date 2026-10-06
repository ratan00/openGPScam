package org.fossify.camera.extensions

import android.content.Context
import android.content.res.Configuration
import org.fossify.camera.helpers.CAMERA_UI_LIGHT
import org.fossify.camera.helpers.CAMERA_UI_SYSTEM
import org.fossify.camera.helpers.Config
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.helpers.PERMISSION_ACCESS_COARSE_LOCATION
import org.fossify.commons.helpers.PERMISSION_ACCESS_FINE_LOCATION
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.config: Config get() = Config.newInstance(applicationContext)

fun Context.getOutputMediaFilePath(isPhoto: Boolean, baseName: String? = null): String {
    val mediaStorageDir = File(config.savePhotosFolder)

    if (!mediaStorageDir.exists()) {
        if (!mediaStorageDir.mkdirs()) {
            return ""
        }
    }

    val mediaName = baseName ?: getRandomMediaName(isPhoto)
    return if (isPhoto) {
        "${mediaStorageDir.path}/$mediaName.jpg"
    } else {
        "${mediaStorageDir.path}/$mediaName.mp4"
    }
}

fun Context.getOutputMediaFileName(isPhoto: Boolean): String {
    val mediaName = getRandomMediaName(isPhoto)
    return if (isPhoto) {
        "$mediaName.jpg"
    } else {
        "$mediaName.mp4"
    }
}

fun getRandomMediaName(isPhoto: Boolean): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return if (isPhoto) {
        "IMG_$timestamp"
    } else {
        "VID_$timestamp"
    }
}

/** Whether the camera screen should use its light (daylight) controls. */
fun Context.isLightCameraUi(): Boolean = when (config.cameraUiMode) {
    CAMERA_UI_LIGHT -> true
    CAMERA_UI_SYSTEM -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
    else -> false
}

fun Context.checkLocationPermission(): Boolean {
    return hasPermission(PERMISSION_ACCESS_FINE_LOCATION) || hasPermission(
        PERMISSION_ACCESS_COARSE_LOCATION
    )
}
