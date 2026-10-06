package org.fossify.camera.helpers

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.internal.compat.workaround.ExifRotationAvailability
import androidx.exifinterface.media.ExifInterface
import org.fossify.camera.helpers.ImageUtil.CodecFailedException
import org.fossify.camera.helpers.ImageUtil.imageToJpegByteArray
import org.fossify.camera.helpers.ImageUtil.jpegImageToJpegByteArray
import org.fossify.camera.models.MediaOutput
import org.fossify.camera.stamp.PhotoStamper
import org.fossify.camera.stamp.StampFormatter
import org.fossify.camera.stamp.StampJob
import org.fossify.commons.extensions.copyTo
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isQPlus
import java.io.*
import java.util.*

/**
 * Inspired by
 * @see androidx.camera.core.ImageSaver
 * */
@Suppress("LongParameterList")
class ImageSaver private constructor(
    private val contentResolver: ContentResolver,
    private val image: ImageProxy,
    private val mediaOutput: MediaOutput.ImageCaptureOutput,
    private val metadata: Metadata,
    private val jpegQuality: Int,
    private val saveExifAttributes: Boolean,
    private val stamp: StampJob?,
    private val onImageSaved: (Uri) -> Unit,
    private val onError: (ImageCaptureException) -> Unit,
) {

    companion object {
        private const val TEMP_FILE_PREFIX = "SimpleCamera"
        private const val TEMP_FILE_SUFFIX = ".tmp"
        private const val COPY_BUFFER_SIZE = 1024
        private const val PENDING = 1
        private const val NOT_PENDING = 0
        private const val MAX_SAMPLE_SIZE = 4

        fun saveImage(
            contentResolver: ContentResolver,
            image: ImageProxy,
            mediaOutput: MediaOutput.ImageCaptureOutput,
            metadata: Metadata,
            jpegQuality: Int,
            saveExifAttributes: Boolean,
            stamp: StampJob? = null,
            onImageSaved: (Uri) -> Unit,
            onError: (ImageCaptureException) -> Unit,
        ) = ImageSaver(
            contentResolver = contentResolver,
            image = image,
            mediaOutput = mediaOutput,
            metadata = metadata,
            jpegQuality = jpegQuality,
            saveExifAttributes = saveExifAttributes,
            stamp = stamp,
            onImageSaved = onImageSaved,
            onError = onError,
        ).saveImage()
    }

    fun saveImage() {
        ensureBackgroundThread {
            // Save the image to a temp file first. This is necessary because ExifInterface only
            // supports saving to File.
            val tempFile = saveImageToTempFile()
            if (tempFile != null) {
                copyTempFileToDestination(tempFile)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Suppress("NestedBlockDepth", "CyclomaticComplexMethod")
    private fun saveImageToTempFile(): File? {
        var saveError: SaveError? = null
        var errorMessage: String? = null
        var exception: Exception? = null

        val tempFile = try {
            if (mediaOutput is MediaOutput.FileMediaOutput) {
                // For saving to file, write to the target folder and rename for better performance.
                File(
                    mediaOutput.file.parent,
                    TEMP_FILE_PREFIX + UUID.randomUUID().toString() + TEMP_FILE_SUFFIX
                )
            } else {
                File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)
            }

        } catch (e: IOException) {
            postError(SaveError.FILE_IO_FAILED, "Error saving temp file", e)
            return null
        }

        try {
            val byteArray: ByteArray = imageToJpegByteArray(image, jpegQuality)
            val stampedBitmapSize = stamp?.let { stampJpeg(byteArray, it, tempFile) }
            if (stampedBitmapSize == null) {
                FileOutputStream(tempFile).use { it.write(byteArray) }
            }

            if (saveExifAttributes || stampedBitmapSize != null) {
                val exifInterface = ExifInterface(tempFile)
                val imageByteArray = jpegImageToJpegByteArray(image)
                val inputStream: InputStream = ByteArrayInputStream(imageByteArray)
                ExifInterface(inputStream).copyTo(exifInterface)

                if (stampedBitmapSize != null) {
                    applyStampedExif(exifInterface, stampedBitmapSize)
                } else {
                    // Overwrite the original orientation if the quirk exists.
                    if (!ExifRotationAvailability().shouldUseExifOrientation(image)) {
                        exifInterface.rotate(image.imageInfo.rotationDegrees)
                    }

                    if (metadata.isReversedHorizontal) {
                        exifInterface.flipHorizontally()
                    }

                    if (metadata.isReversedVertical) {
                        exifInterface.flipVertically()
                    }
                }

                if (metadata.location != null) {
                    exifInterface.setGpsInfo(metadata.location)
                }

                exifInterface.saveAttributes()
            }
        } catch (e: IOException) {
            saveError = SaveError.FILE_IO_FAILED
            errorMessage = "Failed to write temp file"
            exception = e
        } catch (e: IllegalArgumentException) {
            saveError = SaveError.FILE_IO_FAILED
            errorMessage = "Failed to write temp file"
            exception = e
        } catch (e: CodecFailedException) {
            when (e.failureType) {
                CodecFailedException.FailureType.ENCODE_FAILED -> {
                    saveError = SaveError.ENCODE_FAILED
                    errorMessage = "Failed to encode Image"
                }

                CodecFailedException.FailureType.DECODE_FAILED -> {
                    saveError = SaveError.CROP_FAILED
                    errorMessage = "Failed to crop Image"
                }

                CodecFailedException.FailureType.UNKNOWN -> {
                    saveError = SaveError.UNKNOWN
                    errorMessage = "Failed to transcode Image"
                }
            }
            exception = e
        }

        if (saveError != null) {
            postError(saveError, errorMessage, exception)
            tempFile.delete()
            return null
        }

        return tempFile
    }

    /** Rotation and mirroring are already in the pixels, so the file is upright with a fresh size. */
    private fun applyStampedExif(exif: ExifInterface, size: Pair<Int, Int>) {
        val (width, height) = size
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
        exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
        exif.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
        exif.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
        stamp?.let {
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, StampFormatter.summary(it.data, it.settings))
        }
    }

    /**
     * Decodes [jpeg], bakes in rotation/mirroring, draws the strip and writes the result to
     * [target]. Returns the final (width, height), or null to fall back to the unstamped JPEG.
     */
    @SuppressLint("RestrictedApi")
    private fun stampJpeg(jpeg: ByteArray, job: StampJob, target: File): Pair<Int, Int>? {
        var sample = 1
        while (sample <= MAX_SAMPLE_SIZE) {
            try {
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options) ?: return null

                val matrix = Matrix()
                // Orientation the camera wrote into the JPEG, if any.
                // Cropping re-encodes without EXIF, so read it from the camera's original JPEG.
                val exifSource = if (image.format == ImageFormat.JPEG) jpegImageToJpegByteArray(image) else jpeg
                val exif = ExifInterface(ByteArrayInputStream(exifSource))
                matrix.postRotate(exif.rotationDegrees.toFloat())
                if (exif.isFlipped) matrix.postScale(-1f, 1f)
                // Same rules the unstamped path applies to the EXIF tag.
                if (!ExifRotationAvailability().shouldUseExifOrientation(image)) {
                    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
                }
                if (metadata.isReversedHorizontal) matrix.postScale(-1f, 1f)
                if (metadata.isReversedVertical) matrix.postScale(1f, -1f)

                val oriented = if (matrix.isIdentity) {
                    decoded
                } else {
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                        .also { if (it !== decoded) decoded.recycle() }
                }
                val stamped = PhotoStamper.stamp(oriented, job.data, job.minimap, job.settings)
                FileOutputStream(target).use {
                    stamped.compress(Bitmap.CompressFormat.JPEG, jpegQuality, it)
                }
                val size = stamped.width to stamped.height
                stamped.recycle()
                return size
            } catch (_: OutOfMemoryError) {
                // A 50 MP bitmap is ~200 MB; retry at half the resolution on low-memory devices.
                sample *= 2
            }
        }
        return null
    }

    /**
     * Copy the temp file to user specified destination.
     *
     *
     *  The temp file will be deleted afterwards.
     */
    private fun copyTempFileToDestination(tempFile: File) {
        var saveError: SaveError? = null
        var errorMessage: String? = null
        var exception: java.lang.Exception? = null
        var outputUri: Uri? = null
        try {
            when (mediaOutput) {
                is MediaOutput.MediaStoreOutput -> {
                    val values = mediaOutput.contentValues
                    setContentValuePending(values, PENDING)
                    outputUri = contentResolver.insert(
                        mediaOutput.contentUri, values
                    )
                    if (outputUri == null) {
                        saveError = SaveError.FILE_IO_FAILED
                        errorMessage = "Failed to insert URI."
                    } else {
                        if (!copyTempFileToUri(tempFile, outputUri)) {
                            saveError = SaveError.FILE_IO_FAILED
                            errorMessage = "Failed to save to URI."
                        }
                        setUriNotPending(outputUri)
                    }
                }

                is MediaOutput.OutputStreamMediaOutput -> {
                    copyTempFileToOutputStream(tempFile, mediaOutput.outputStream)
                    outputUri = mediaOutput.uri
                }

                is MediaOutput.FileMediaOutput -> {
                    val targetFile: File = mediaOutput.file
                    // Normally File#renameTo will overwrite the targetFile even if it already exists.
                    // Just in case of unexpected behavior on certain platforms or devices, delete the
                    // target file before renaming.
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    if (!tempFile.renameTo(targetFile)) {
                        saveError = SaveError.FILE_IO_FAILED
                        errorMessage = "Failed to rename file."
                    }
                    outputUri = Uri.fromFile(targetFile)
                }

                MediaOutput.BitmapOutput -> throw UnsupportedOperationException("Bitmap output cannot be saved to disk")
            }
        } catch (e: IOException) {
            saveError = SaveError.FILE_IO_FAILED
            errorMessage = "Failed to write destination file."
            exception = e
        } catch (e: IllegalArgumentException) {
            saveError = SaveError.FILE_IO_FAILED
            errorMessage = "Failed to write destination file."
            exception = e
        } finally {
            tempFile.delete()
        }

        outputUri?.let(onImageSaved) ?: postError(saveError!!, errorMessage, exception)
    }

    private fun postError(saveError: SaveError, errorMessage: String?, exception: Exception?) {
        val imageCaptureError = if (saveError == SaveError.FILE_IO_FAILED) {
            ImageCapture.ERROR_FILE_IO
        } else {
            ImageCapture.ERROR_UNKNOWN
        }

        onError.invoke(ImageCaptureException(imageCaptureError, errorMessage!!, exception!!))
    }

    /**
     * Removes IS_PENDING flag during the writing to [Uri].
     */
    private fun setUriNotPending(outputUri: Uri) {
        if (isQPlus()) {
            val values = ContentValues()
            setContentValuePending(values, NOT_PENDING)
            contentResolver.update(outputUri, values, null, null)
        }
    }

    /** Set IS_PENDING flag to [ContentValues].  */
    private fun setContentValuePending(values: ContentValues, isPending: Int) {
        if (isQPlus()) {
            values.put(MediaStore.Images.Media.IS_PENDING, isPending)
        }
    }

    /**
     * Copies temp file to [Uri].
     *
     * @return false if the [Uri] is not writable.
     */
    @Throws(IOException::class)
    private fun copyTempFileToUri(tempFile: File, uri: Uri): Boolean {
        contentResolver.openOutputStream(uri).use { outputStream ->
            if (outputStream == null) {
                // The URI is not writable.
                return false
            }
            copyTempFileToOutputStream(tempFile, outputStream)
        }
        return true
    }

    @Throws(IOException::class)
    private fun copyTempFileToOutputStream(tempFile: File, outputStream: OutputStream) {
        FileInputStream(tempFile).use { inputStream ->
            val buf = ByteArray(COPY_BUFFER_SIZE)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) {
                outputStream.write(buf, 0, len)
            }
        }
    }

    /** Type of error that occurred during save  */
    enum class SaveError {
        /** Failed to write to or close the file  */
        FILE_IO_FAILED,

        /** Failure when attempting to encode image  */
        ENCODE_FAILED,

        /** Failure when attempting to crop image  */
        CROP_FAILED, UNKNOWN
    }
}
