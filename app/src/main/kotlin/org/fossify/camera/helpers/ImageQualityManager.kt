package org.fossify.camera.helpers

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import org.fossify.camera.extensions.config
import org.fossify.camera.models.CameraSelectorImageQualities
import org.fossify.camera.models.CaptureMode
import org.fossify.camera.models.MySize
import org.fossify.commons.extensions.showErrorToast

class ImageQualityManager(private val activity: AppCompatActivity) {
    companion object {
        private val CAMERA_LENS =
            arrayOf(CameraCharacteristics.LENS_FACING_FRONT, CameraCharacteristics.LENS_FACING_BACK)
        private const val MAX_SIZES_PER_ASPECT = 4
    }

    private val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val imageQualities = mutableListOf<CameraSelectorImageQualities>()
    private val mediaSizeStore = MediaSizeStore(activity.config)

    fun initSupportedQualities() {
        if (imageQualities.isEmpty()) {
            for (cameraId in cameraManager.cameraIdList) {
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val lensFacing =
                        characteristics.get(CameraCharacteristics.LENS_FACING) ?: continue
                    if (lensFacing in CAMERA_LENS) {
                        val configMap =
                            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                                ?: continue
                        val standardImageSizes = configMap.getOutputSizes(ImageFormat.JPEG)
                            .map { MySize(it.width, it.height) }
                        val imageSizes = if (activity.config.captureMode == CaptureMode.MAXIMIZE_QUALITY) {
                            standardImageSizes + configMap.getHighResolutionOutputSizes(ImageFormat.JPEG)
                                .map { MySize(it.width, it.height) }
                        } else {
                            standardImageSizes
                        }
                        val cameraSelector = lensFacing.toCameraSelector()
                        imageQualities.add(CameraSelectorImageQualities(cameraSelector, imageSizes))
                    }
                } catch (e: Exception) {
                    activity.showErrorToast(e)
                }
            }
        }
    }

    private fun Int.toCameraSelector(): CameraSelector {
        return if (this == CameraCharacteristics.LENS_FACING_FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun getUserSelectedResolution(cameraSelector: CameraSelector): MySize {
        val aspect = getUserSelectedAspect(cameraSelector)
        val sizes = getSizesForAspect(cameraSelector, aspect)
        val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        val sizeIndex = mediaSizeStore.getPhotoSizeIndex(isFrontCamera)
        val size = sizes.getOrNull(sizeIndex) ?: sizes.firstOrNull() ?: return aspect
        return size.copy(isFullScreen = aspect.isFullScreen)
    }

    /** The selected entry of [getSupportedResolutions]; defaults to the largest 4:3 size. */
    private fun getUserSelectedAspect(cameraSelector: CameraSelector): MySize {
        val resolutions = getSupportedResolutions(cameraSelector)
        val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        var index = mediaSizeStore.getCurrentSizeIndex(
            isPhotoCapture = true, isFrontCamera = isFrontCamera
        )
        if (index == PHOTO_RESOLUTION_UNSET && resolutions.isNotEmpty()) {
            index = resolutions.indexOfFirst { it.getAspectRatio(activity) == "4:3" }
                .coerceAtLeast(0)
            mediaSizeStore.storeSize(isPhotoCapture = true, isFrontCamera = isFrontCamera, currentIndex = index)
        }
        index = index.coerceAtMost(resolutions.lastIndex).coerceAtLeast(0)
        return resolutions[index]
    }

    /** All sizes sharing [aspect]'s ratio, largest first, one per megapixel label. */
    fun getSizesForAspect(cameraSelector: CameraSelector, aspect: MySize): List<MySize> {
        val ratio = aspect.getAspectRatio(activity)
        return imageQualities.filter { it.camSelector == cameraSelector }
            .flatMap { it.qualities }
            .filter { it.getAspectRatio(activity) == ratio }
            .sortedByDescending { it.pixels }
            .distinctBy { it.megaPixelLabel() }
            .take(MAX_SIZES_PER_ASPECT)
    }

    /** Sizes for the currently selected aspect ratio, for the megapixel selector. */
    fun getSizesForSelectedAspect(cameraSelector: CameraSelector): List<MySize> =
        getSizesForAspect(cameraSelector, getUserSelectedAspect(cameraSelector))

    /** One entry per aspect ratio (4:3, 16:9, 1:1), largest first. No full-screen mode. */
    fun getSupportedResolutions(cameraSelector: CameraSelector): List<MySize> {
        return imageQualities.filter { it.camSelector == cameraSelector }
            .flatMap { it.qualities }
            .filter { it.isSupported(false) }
            .sortedByDescending { it.pixels }
            .distinctBy { it.getAspectRatio(activity) }
            .sortedByDescending {
                it.getAspectRatio(activity).split(":").firstOrNull()?.toIntOrNull()
            }
    }
}
