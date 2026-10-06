package org.fossify.camera.interfaces

interface MyPreview {

    fun isInPhotoMode(): Boolean

    fun setFlashlightState(state: Int)

    fun toggleFrontBackCamera()

    fun handleFlashlightClick()

    fun tryTakePicture()

    fun toggleRecording()

    fun initPhotoMode()

    fun initVideoMode()

    fun showChangeResolution()

    /** Opens the megapixel menu for the current photo aspect ratio. */
    fun showPhotoSizes()

    fun setZoomRatio(ratio: Float)

    /** Asks for a brand-new location fix; [callback] gets it, or null if none arrived. */
    fun refreshLocation(callback: (android.location.Location?) -> Unit)
}
