package org.fossify.camera.helpers

import android.content.Context
import android.os.Environment
import androidx.camera.core.CameraSelector
import org.fossify.camera.models.CaptureMode
import org.fossify.camera.models.TimerMode
import org.fossify.camera.stamp.StampSettings
import org.fossify.commons.helpers.BaseConfig
import java.io.File

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var savePhotosFolder: String
        get(): String {
            var path = prefs.getString(
                SAVE_PHOTOS,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()
            )
            if (!File(path).exists() || !File(path).isDirectory) {
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .toString()
                savePhotosFolder = path
            }
            return path!!
        }
        set(path) = prefs.edit().putString(SAVE_PHOTOS, path).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(SOUND, true)
        set(enabled) = prefs.edit().putBoolean(SOUND, enabled).apply()

    var volumeButtonsAsShutter: Boolean
        get() = prefs.getBoolean(VOLUME_BUTTONS_AS_SHUTTER, false)
        set(volumeButtonsAsShutter) = prefs.edit()
            .putBoolean(VOLUME_BUTTONS_AS_SHUTTER, volumeButtonsAsShutter).apply()

    var flipPhotos: Boolean
        get() = prefs.getBoolean(FLIP_PHOTOS, true)
        set(flipPhotos) = prefs.edit().putBoolean(FLIP_PHOTOS, flipPhotos).apply()

    var lastUsedCameraLens: Int
        get() = prefs.getInt(LAST_USED_CAMERA_LENS, CameraSelector.LENS_FACING_BACK)
        set(lens) = prefs.edit().putInt(LAST_USED_CAMERA_LENS, lens).apply()

    var initPhotoMode: Boolean
        get() = prefs.getBoolean(INIT_PHOTO_MODE, true)
        set(initPhotoMode) = prefs.edit().putBoolean(INIT_PHOTO_MODE, initPhotoMode).apply()

    var flashlightState: Int
        get() = prefs.getInt(FLASHLIGHT_STATE, FLASH_OFF)
        set(state) = prefs.edit().putInt(FLASHLIGHT_STATE, state).apply()

    var backPhotoResIndex: Int
        get() = prefs.getInt(BACK_PHOTO_RESOLUTION_INDEX, 0)
        set(backPhotoResIndex) = prefs.edit().putInt(BACK_PHOTO_RESOLUTION_INDEX, backPhotoResIndex)
            .apply()

    var backVideoResIndex: Int
        get() = prefs.getInt(BACK_VIDEO_RESOLUTION_INDEX, 0)
        set(backVideoResIndex) = prefs.edit().putInt(BACK_VIDEO_RESOLUTION_INDEX, backVideoResIndex)
            .apply()

    var frontPhotoResIndex: Int
        get() = prefs.getInt(FRONT_PHOTO_RESOLUTION_INDEX, 0)
        set(frontPhotoResIndex) = prefs.edit()
            .putInt(FRONT_PHOTO_RESOLUTION_INDEX, frontPhotoResIndex).apply()

    var frontVideoResIndex: Int
        get() = prefs.getInt(FRONT_VIDEO_RESOLUTION_INDEX, 0)
        set(frontVideoResIndex) = prefs.edit()
            .putInt(FRONT_VIDEO_RESOLUTION_INDEX, frontVideoResIndex).apply()

    var savePhotoMetadata: Boolean
        get() = prefs.getBoolean(SAVE_PHOTO_METADATA, true)
        set(savePhotoMetadata) = prefs.edit().putBoolean(SAVE_PHOTO_METADATA, savePhotoMetadata)
            .apply()

    var savePhotoVideoLocation: Boolean
        get() = prefs.getBoolean(SAVE_PHOTO_VIDEO_LOCATION, false)
        set(savePhotoVideoLocation) = prefs.edit()
            .putBoolean(SAVE_PHOTO_VIDEO_LOCATION, savePhotoVideoLocation).apply()

    var stampEnabled: Boolean
        get() = prefs.getBoolean(STAMP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(STAMP_ENABLED, value).apply()

    var stampShowMap: Boolean
        get() = prefs.getBoolean(STAMP_SHOW_MAP, true)
        set(value) = prefs.edit().putBoolean(STAMP_SHOW_MAP, value).apply()

    var stampShowAddress: Boolean
        get() = prefs.getBoolean(STAMP_SHOW_ADDRESS, true)
        set(value) = prefs.edit().putBoolean(STAMP_SHOW_ADDRESS, value).apply()

    var stampShowCoordinates: Boolean
        get() = prefs.getBoolean(STAMP_SHOW_COORDINATES, true)
        set(value) = prefs.edit().putBoolean(STAMP_SHOW_COORDINATES, value).apply()

    var stampShowDateTime: Boolean
        get() = prefs.getBoolean(STAMP_SHOW_DATE_TIME, true)
        set(value) = prefs.edit().putBoolean(STAMP_SHOW_DATE_TIME, value).apply()

    var stamp24Hour: Boolean
        get() = prefs.getBoolean(STAMP_24_HOUR, true)
        set(value) = prefs.edit().putBoolean(STAMP_24_HOUR, value).apply()

    var stampDms: Boolean
        get() = prefs.getBoolean(STAMP_DMS, false)
        set(value) = prefs.edit().putBoolean(STAMP_DMS, value).apply()

    var stampAtTop: Boolean
        get() = prefs.getBoolean(STAMP_AT_TOP, false)
        set(value) = prefs.edit().putBoolean(STAMP_AT_TOP, value).apply()

    var stampKeepOriginal: Boolean
        get() = prefs.getBoolean(STAMP_KEEP_ORIGINAL, false)
        set(value) = prefs.edit().putBoolean(STAMP_KEEP_ORIGINAL, value).apply()

    var stampOpacity: Int
        get() = prefs.getInt(STAMP_OPACITY, StampSettings.DEFAULT_OPACITY)
        set(value) = prefs.edit().putInt(STAMP_OPACITY, value).apply()

    val stampSettings: StampSettings
        get() = StampSettings(
            showMap = stampShowMap,
            showAddress = stampShowAddress,
            showCoordinates = stampShowCoordinates,
            showDateTime = stampShowDateTime,
            use24Hour = stamp24Hour,
            useDms = stampDms,
            atTop = stampAtTop,
            opacityPercent = stampOpacity,
        )

    /** Location is only read while the camera is open and something needs it. */
    val needsLocation: Boolean
        get() = savePhotoVideoLocation || stampEnabled

    var photoQuality: Int
        get() = prefs.getInt(PHOTO_QUALITY, 80)
        set(photoQuality) = prefs.edit().putInt(PHOTO_QUALITY, photoQuality).apply()

    var captureMode: CaptureMode
        get() = CaptureMode.values()[prefs.getInt(
            CAPTURE_MODE,
            CaptureMode.MINIMIZE_LATENCY.ordinal
        )]
        set(captureMode) = prefs.edit().putInt(CAPTURE_MODE, captureMode.ordinal).apply()

    var maxBrightness: Boolean
        get() = prefs.getBoolean(MAX_BRIGHTNESS, false)
        set(maxBrightness) = prefs.edit().putBoolean(MAX_BRIGHTNESS, maxBrightness).apply()

    var timerMode: TimerMode
        get() = TimerMode.values().getOrNull(prefs.getInt(TIMER_MODE, TimerMode.OFF.ordinal))
            ?: TimerMode.OFF
        set(timerMode) = prefs.edit().putInt(TIMER_MODE, timerMode.ordinal).apply()

}
