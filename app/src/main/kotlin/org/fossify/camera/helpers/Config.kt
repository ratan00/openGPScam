package org.fossify.camera.helpers

import android.content.Context
import android.os.Environment
import androidx.camera.core.CameraSelector
import org.fossify.camera.R
import org.fossify.camera.models.CaptureMode
import org.fossify.camera.models.TimerMode
import org.fossify.camera.stamp.QrPlacement
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
        get() = prefs.getBoolean(VOLUME_BUTTONS_AS_SHUTTER, true)
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
        get() = prefs.getInt(BACK_PHOTO_RESOLUTION_INDEX, PHOTO_RESOLUTION_UNSET)
        set(backPhotoResIndex) = prefs.edit().putInt(BACK_PHOTO_RESOLUTION_INDEX, backPhotoResIndex)
            .apply()

    var backVideoResIndex: Int
        get() = prefs.getInt(BACK_VIDEO_RESOLUTION_INDEX, 0)
        set(backVideoResIndex) = prefs.edit().putInt(BACK_VIDEO_RESOLUTION_INDEX, backVideoResIndex)
            .apply()

    var frontPhotoResIndex: Int
        get() = prefs.getInt(FRONT_PHOTO_RESOLUTION_INDEX, PHOTO_RESOLUTION_UNSET)
        set(frontPhotoResIndex) = prefs.edit()
            .putInt(FRONT_PHOTO_RESOLUTION_INDEX, frontPhotoResIndex).apply()

    var frontVideoResIndex: Int
        get() = prefs.getInt(FRONT_VIDEO_RESOLUTION_INDEX, 0)
        set(frontVideoResIndex) = prefs.edit()
            .putInt(FRONT_VIDEO_RESOLUTION_INDEX, frontVideoResIndex).apply()

    /** Index into the sizes of the current aspect ratio, largest first. */
    var backPhotoSizeIndex: Int
        get() = prefs.getInt(BACK_PHOTO_SIZE_INDEX, 0)
        set(backPhotoSizeIndex) = prefs.edit().putInt(BACK_PHOTO_SIZE_INDEX, backPhotoSizeIndex).apply()

    var frontPhotoSizeIndex: Int
        get() = prefs.getInt(FRONT_PHOTO_SIZE_INDEX, 0)
        set(frontPhotoSizeIndex) = prefs.edit().putInt(FRONT_PHOTO_SIZE_INDEX, frontPhotoSizeIndex).apply()

    var savePhotoMetadata: Boolean
        get() = prefs.getBoolean(SAVE_PHOTO_METADATA, true)
        set(savePhotoMetadata) = prefs.edit().putBoolean(SAVE_PHOTO_METADATA, savePhotoMetadata)
            .apply()

    var savePhotoVideoLocation: Boolean
        get() = prefs.getBoolean(SAVE_PHOTO_VIDEO_LOCATION, true)
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

    var stampShowAltitude: Boolean
        get() = prefs.getBoolean(STAMP_SHOW_ALTITUDE, true)
        set(value) = prefs.edit().putBoolean(STAMP_SHOW_ALTITUDE, value).apply()

    var stampShowCompass: Boolean
        get() = prefs.getBoolean(STAMP_SHOW_COMPASS, true)
        set(value) = prefs.edit().putBoolean(STAMP_SHOW_COMPASS, value).apply()

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

    var stampShowWatermark: Boolean
        get() = prefs.getBoolean(STAMP_SHOW_WATERMARK, true)
        set(value) = prefs.edit().putBoolean(STAMP_SHOW_WATERMARK, value).apply()

    var stampOfficerName: String
        get() = prefs.getString(STAMP_OFFICER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(STAMP_OFFICER_NAME, value).apply()

    /** Camera screen chrome: [CAMERA_UI_DARK], [CAMERA_UI_LIGHT] (daylight) or [CAMERA_UI_SYSTEM]. */
    var cameraUiMode: Int
        get() = prefs.getInt(CAMERA_UI_MODE, CAMERA_UI_SYSTEM)
        set(value) = prefs.edit().putInt(CAMERA_UI_MODE, value).apply()

    var stampRemarks: String
        get() = prefs.getString(STAMP_REMARKS, "") ?: ""
        set(value) = prefs.edit().putString(STAMP_REMARKS, value).apply()

    var stampQrPlacement: QrPlacement
        get() = QrPlacement.entries.getOrNull(prefs.getInt(STAMP_QR_PLACEMENT, QrPlacement.OFF.ordinal)) ?: QrPlacement.OFF
        set(value) = prefs.edit().putInt(STAMP_QR_PLACEMENT, value.ordinal).apply()

    var stampDesignation: String
        get() = prefs.getString(STAMP_DESIGNATION, "") ?: ""
        set(value) = prefs.edit().putString(STAMP_DESIGNATION, value).apply()

    var stampOrganisation: String
        get() = prefs.getString(STAMP_ORGANISATION, "") ?: ""
        set(value) = prefs.edit().putString(STAMP_ORGANISATION, value).apply()

    val stampSettings: StampSettings
        get() = StampSettings(
            showMap = stampShowMap,
            showAddress = stampShowAddress,
            showCoordinates = stampShowCoordinates,
            showDateTime = stampShowDateTime,
            showAltitude = stampShowAltitude,
            showCompass = stampShowCompass,
            use24Hour = stamp24Hour,
            useDms = stampDms,
            atTop = stampAtTop,
            opacityPercent = stampOpacity,
            showWatermark = stampShowWatermark,
            appName = context.getString(R.string.app_name),
            officerName = stampOfficerName,
            designation = stampDesignation,
            remarks = stampRemarks,
            qrPlacement = stampQrPlacement,
            organisation = stampOrganisation,
        )

    /** Location is only read while the camera is open and something needs it. */
    val needsLocation: Boolean
        get() = savePhotoVideoLocation || stampEnabled

    var photoQuality: Int
        get() = prefs.getInt(PHOTO_QUALITY, 50)
        set(photoQuality) = prefs.edit().putInt(PHOTO_QUALITY, photoQuality).apply()

    var captureMode: CaptureMode
        get() = CaptureMode.values()[prefs.getInt(
            CAPTURE_MODE,
            CaptureMode.MAXIMIZE_QUALITY.ordinal
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
