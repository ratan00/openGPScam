package org.fossify.camera.implementations

import android.graphics.Bitmap
import android.net.Uri
import org.fossify.camera.models.ResolutionOption

interface CameraXPreviewListener {
    fun onInitPhotoMode()
    fun onInitVideoMode()
    fun setCameraAvailable(available: Boolean) {}
    fun setHasFrontAndBackCamera(hasFrontAndBack: Boolean)
    fun setFlashAvailable(available: Boolean)
    fun onChangeCamera(frontCamera: Boolean) {}
    fun shutterAnimation()
    fun onMediaSaved(uri: Uri)
    fun onImageCaptured(bitmap: Bitmap)
    fun onChangeFlashMode(flashMode: Int)
    fun onPhotoCaptureStart()
    fun onPhotoCaptureEnd()
    fun onVideoRecordingStarted()
    fun onVideoRecordingStopped()
    fun onVideoDurationChanged(durationNanos: Long)
    fun onFocusCamera(xPos: Float, yPos: Float)
    fun onTouchPreview()
    fun displaySelectedResolution(resolutionOption: ResolutionOption)
    /** [label] is e.g. "12M", or null when there is nothing to choose (video, single size). */
    fun displayPhotoSize(label: String?)
    fun onZoomChanged(minRatio: Float, maxRatio: Float, ratio: Float)
    fun showPhotoSizeMenu(options: List<String>, selected: Int, onSelect: (Int) -> Unit)
    fun showImageSizes(
        selectedResolution: ResolutionOption,
        resolutions: List<ResolutionOption>,
        isPhotoCapture: Boolean,
        isFrontCamera: Boolean,
        onSelect: (index: Int, changed: Boolean) -> Unit,
    )

    fun showFlashOptions(photoCapture: Boolean)
    fun adjustPreviewView(requiresCentering: Boolean)
}
