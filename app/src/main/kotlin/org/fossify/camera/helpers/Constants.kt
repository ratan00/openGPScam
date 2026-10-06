package org.fossify.camera.helpers

const val ORIENT_PORTRAIT = 0
const val ORIENT_LANDSCAPE_LEFT = 1
const val ORIENT_LANDSCAPE_RIGHT = 2

// shared preferences
const val SAVE_PHOTOS = "save_photos"
const val SOUND = "sound"
const val VOLUME_BUTTONS_AS_SHUTTER = "volume_buttons_as_shutter"
const val FLIP_PHOTOS = "flip_photos"
const val LAST_USED_CAMERA = "last_used_camera_3"
const val LAST_USED_CAMERA_LENS = "last_used_camera_lens"
const val FLASHLIGHT_STATE = "flashlight_state"
const val INIT_PHOTO_MODE = "init_photo_mode"
const val BACK_PHOTO_RESOLUTION_INDEX = "back_photo_resolution_index_4"
const val BACK_VIDEO_RESOLUTION_INDEX = "back_video_resolution_index_3"
const val FRONT_PHOTO_RESOLUTION_INDEX = "front_photo_resolution_index_4"
const val FRONT_VIDEO_RESOLUTION_INDEX = "front_video_resolution_index_3"
const val BACK_PHOTO_SIZE_INDEX = "back_photo_size_index"
const val FRONT_PHOTO_SIZE_INDEX = "front_photo_size_index"
const val SAVE_PHOTO_METADATA = "save_photo_metadata"
const val SAVE_PHOTO_VIDEO_LOCATION = "save_photo_video_location"
const val STAMP_ENABLED = "stamp_enabled"
const val STAMP_SHOW_MAP = "stamp_show_map"
const val STAMP_SHOW_ADDRESS = "stamp_show_address"
const val STAMP_SHOW_COORDINATES = "stamp_show_coordinates"
const val STAMP_SHOW_DATE_TIME = "stamp_show_date_time"
const val STAMP_SHOW_ALTITUDE = "stamp_show_altitude"
const val STAMP_SHOW_COMPASS = "stamp_show_compass"
const val STAMP_24_HOUR = "stamp_24_hour"
const val STAMP_DMS = "stamp_dms"
const val STAMP_AT_TOP = "stamp_at_top"
const val STAMP_OPACITY = "stamp_opacity"
const val STAMP_KEEP_ORIGINAL = "stamp_keep_original"
const val STAMP_SHOW_WATERMARK = "stamp_show_watermark"
const val STAMP_OFFICER_NAME = "stamp_officer_name"
const val STAMP_ORGANISATION = "stamp_organisation"
const val STAMP_DESIGNATION = "stamp_designation"
const val STAMP_REMARKS = "stamp_remarks"
const val CAMERA_UI_MODE = "camera_ui_mode"
const val CAMERA_UI_DARK = 0
const val CAMERA_UI_LIGHT = 1
const val CAMERA_UI_SYSTEM = 2
const val STAMP_QR_PLACEMENT = "stamp_qr_placement"
const val PHOTO_QUALITY = "photo_quality"
const val CAPTURE_MODE = "capture_mode"
const val TIMER_MODE = "timer_mode"
const val MAX_BRIGHTNESS = "max_brightness"

/** Photo resolution index not chosen yet; resolved to the largest 4:3 size on first use. */
const val PHOTO_RESOLUTION_UNSET = -1

const val FLASH_OFF = 0
const val FLASH_ON = 1
const val FLASH_AUTO = 2
const val FLASH_ALWAYS_ON = 3

fun compensateDeviceRotation(orientation: Int) = when (orientation) {
    ORIENT_LANDSCAPE_LEFT -> 270
    ORIENT_LANDSCAPE_RIGHT -> 90
    else -> 0
}
