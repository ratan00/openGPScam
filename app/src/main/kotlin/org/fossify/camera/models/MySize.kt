package org.fossify.camera.models

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import org.fossify.camera.R
import kotlin.math.abs
import kotlin.math.roundToInt

data class MySize(val width: Int, val height: Int, val isFullScreen: Boolean = false) {
    companion object {
        private const val RATIO_TOLERANCE = 0.01f
        private const val ONE_MEGA_PIXEL = 1000000
        private const val ZERO_MEGA_PIXEL = "0.0"
    }

    private val ratio = width / height.toFloat()

    /** Sensors often report sizes a few pixels off the exact ratio (e.g. 4080x2296 for 16:9). */
    private fun isRatio(target: Float) = abs(ratio - target) < RATIO_TOLERANCE

    val pixels: Int = width * height

    val megaPixels: String = String.format("%.1f", (width * height.toFloat()) / ONE_MEGA_PIXEL)

    fun requiresCentering(): Boolean {
        return !isFullScreen && (isFourToThree() || isThreeToTwo() || isSquare())
    }

    fun isSixteenToNine() = isRatio(16 / 9f)

    private fun isFiveToThree() = isRatio(5 / 3f)

    private fun isFourToThree() = isRatio(4 / 3f)

    private fun isTwoToOne() = isRatio(2f)

    private fun isThreeToFour() = isRatio(3 / 4f)

    private fun isThreeToTwo() = isRatio(3 / 2f)

    private fun isSixToFive() = isRatio(6 / 5f)

    private fun isNineteenToNine() = isRatio(19 / 9f)

    private fun isNineteenToEight() = isRatio(19 / 8f)

    private fun isOneNineToOne() = isRatio(1.9f)

    private fun isSquare() = width == height

    fun isSupported(isFullScreenSize16x9: Boolean): Boolean {
        return if (isFullScreenSize16x9) {
            isFourToThree() || isSquare()
        } else {
            isFourToThree() || isSixteenToNine() || isSquare()
        } && megaPixels != ZERO_MEGA_PIXEL
    }

    fun getAspectRatio(context: Context) = when {
        isSixteenToNine() -> "16:9"
        isFiveToThree() -> "5:3"
        isFourToThree() -> "4:3"
        isThreeToFour() -> "3:4"
        isThreeToTwo() -> "3:2"
        isSixToFive() -> "6:5"
        isOneNineToOne() -> "1.9:1"
        isNineteenToNine() -> "19:9"
        isNineteenToEight() -> "19:8"
        isSquare() -> "1:1"
        isTwoToOne() -> "2:1"
        else -> context.resources.getString(org.fossify.commons.R.string.other)
    }

    @DrawableRes
    fun getImageResId(): Int = when {
        isFullScreen -> R.drawable.ic_photo_full_vector
        isSixteenToNine() -> R.drawable.ic_photo_16x9_vector
        isFourToThree() -> R.drawable.ic_photo_4x3_vector
        isSquare() -> R.drawable.ic_photo_1x1_vector
        else -> throw UnsupportedOperationException("This size $this is not supported")
    }

    @IdRes
    fun getButtonId(): Int = when {
        isFullScreen -> R.id.photo_full
        isSixteenToNine() -> R.id.photo_16x9
        isFourToThree() -> R.id.photo_4x3
        isSquare() -> R.id.photo_1x1
        else -> throw UnsupportedOperationException("This size $this is not supported")
    }

    /** Short label for the megapixel selector, e.g. "12M" or "50M". */
    fun megaPixelLabel(): String = "${(pixels / ONE_MEGA_PIXEL.toFloat()).roundToInt()}M"

    fun toResolutionOption(): ResolutionOption {
        return ResolutionOption(buttonViewId = getButtonId(), imageDrawableResId = getImageResId())
    }
}
