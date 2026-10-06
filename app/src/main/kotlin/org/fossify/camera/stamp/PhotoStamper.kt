package org.fossify.camera.stamp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

data class StampStyle(
    val use24Hour: Boolean = true,
    val stripAlpha: Int = 140,
)

/**
 * Draws the label strip onto a bitmap. Sizes are relative to the short edge of the image
 * so the stamp looks the same at 12 MP and 50 MP.
 */
object PhotoStamper {

    /** Draws onto [bitmap], which must be mutable, and returns it. */
    fun stamp(
        bitmap: Bitmap,
        data: StampData,
        minimap: Bitmap? = null,
        style: StampStyle = StampStyle(),
    ): Bitmap {
        val canvas = Canvas(bitmap)
        val shortEdge = minOf(bitmap.width, bitmap.height).toFloat()
        val margin = shortEdge * 0.025f
        val textSize = shortEdge * 0.028f

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(textSize * 0.12f, 0f, 0f, Color.BLACK)
        }

        val mapSize = if (minimap != null) shortEdge * 0.2f else 0f
        val textLeft = margin + if (minimap != null) mapSize + margin else 0f
        val textWidth = (bitmap.width - textLeft - margin).toInt().coerceAtLeast(1)

        val text = StampFormatter.lines(data, style.use24Hour).joinToString("\n")
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(5)
            .build()

        val contentHeight = maxOf(layout.height.toFloat(), mapSize)
        val stripHeight = contentHeight + margin * 2
        val top = bitmap.height - stripHeight

        canvas.drawRect(
            0f, top, bitmap.width.toFloat(), bitmap.height.toFloat(),
            Paint().apply { color = Color.argb(style.stripAlpha, 0, 0, 0) }
        )

        if (minimap != null) {
            val dest = android.graphics.RectF(margin, top + margin, margin + mapSize, top + margin + mapSize)
            canvas.drawBitmap(minimap, null, dest, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        canvas.save()
        canvas.translate(textLeft, top + margin + (contentHeight - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()

        return bitmap
    }
}
