package org.fossify.camera.stamp

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Draws QR codes as vector squares so they stay sharp at any photo size. */
object QrRenderer {

    private var cachedText: String? = null
    private var cachedMatrix: BitMatrix? = null

    @Synchronized
    private fun matrix(text: String): BitMatrix? {
        if (text == cachedText) return cachedMatrix
        val matrix = try {
            QRCodeWriter().encode(
                text, BarcodeFormat.QR_CODE, 0, 0,
                mapOf(
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN to QUIET_ZONE_MODULES,
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                )
            )
        } catch (_: WriterException) {
            null
        }
        cachedText = text
        cachedMatrix = matrix
        return matrix
    }

    /** Draws [text] as a QR code filling [rect] (white background, rounded by [radius]). */
    fun draw(canvas: Canvas, rect: RectF, text: String, radius: Float) {
        val m = matrix(text) ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(rect, radius, radius, paint)
        val cell = minOf(rect.width() / m.width, rect.height() / m.height)
        val left = rect.centerX() - cell * m.width / 2f
        val top = rect.centerY() - cell * m.height / 2f
        paint.color = Color.BLACK
        paint.isAntiAlias = false
        for (y in 0 until m.height) {
            for (x in 0 until m.width) {
                if (m[x, y]) {
                    // Overlap by a hair so no seams show between modules.
                    canvas.drawRect(
                        left + x * cell, top + y * cell,
                        left + (x + 1) * cell + SEAM, top + (y + 1) * cell + SEAM, paint
                    )
                }
            }
        }
    }

    private const val QUIET_ZONE_MODULES = 2
    private const val SEAM = 0.5f
}
