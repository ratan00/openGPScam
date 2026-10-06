package org.fossify.camera.helpers

class MediaSizeStore(private val config: Config) {

    fun storeSize(isPhotoCapture: Boolean, isFrontCamera: Boolean, currentIndex: Int) {
        if (isPhotoCapture) {
            if (getCurrentSizeIndex(true, isFrontCamera) != currentIndex) {
                // A new aspect ratio starts at its largest size.
                storePhotoSizeIndex(isFrontCamera, 0)
            }
            if (isFrontCamera) {
                config.frontPhotoResIndex = currentIndex
            } else {
                config.backPhotoResIndex = currentIndex
            }
        } else {
            if (isFrontCamera) {
                config.frontVideoResIndex = currentIndex
            } else {
                config.backVideoResIndex = currentIndex
            }
        }
    }

    fun getCurrentSizeIndex(isPhotoCapture: Boolean, isFrontCamera: Boolean): Int {
        return if (isPhotoCapture) {
            if (isFrontCamera) {
                config.frontPhotoResIndex
            } else {
                config.backPhotoResIndex
            }
        } else {
            if (isFrontCamera) {
                config.frontVideoResIndex
            } else {
                config.backVideoResIndex
            }
        }
    }

    fun getPhotoSizeIndex(isFrontCamera: Boolean): Int =
        if (isFrontCamera) config.frontPhotoSizeIndex else config.backPhotoSizeIndex

    fun storePhotoSizeIndex(isFrontCamera: Boolean, index: Int) {
        if (isFrontCamera) {
            config.frontPhotoSizeIndex = index
        } else {
            config.backPhotoSizeIndex = index
        }
    }
}
