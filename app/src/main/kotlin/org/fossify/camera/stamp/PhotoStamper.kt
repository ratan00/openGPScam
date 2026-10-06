package org.fossify.camera.stamp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

/** Draws the GPS strip. [stamp] is used for photos, [drawStrip] also drives the live overlay. */
object PhotoStamper {

    /** Returns a bitmap with the strip burned in. Mutates and returns [src] when it is mutable. */
    fun stamp(
        src: Bitmap,
        data: StampData,
        minimap: Bitmap?,
        settings: StampSettings,
    ): Bitmap {
        val target = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
        drawStrip(Canvas(target), target.width, target.height, data, minimap, settings)
        return target
    }

    /** Draws the strip into [canvas] covering a [width] x [height] area at its origin. */
    fun drawStrip(
        canvas: Canvas,
        width: Int,
        height: Int,
        data: StampData,
        minimap: Bitmap?,
        settings: StampSettings,
    ) {
        val lines = StampFormatter.lines(data, settings)
        val showMap = settings.showMap
        if (lines.isEmpty && !showMap) return

        val layout = StampLayout.forImage(width, height)
        val pad = layout.padding

        val boldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = layout.textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val normalPaint = TextPaint(boldPaint).apply {
            textSize = layout.smallTextSize
            typeface = Typeface.DEFAULT
        }

        // First pass with the maximum map width to measure the text height, then size the map.
        val maxMap = (width * StampLayout.MAX_MAP_WIDTH_FRACTION).toInt()
        fun textWidth(mapSize: Int) =
            (width - pad * 2 - if (showMap) mapSize + pad else 0f).toInt().coerceAtLeast(1)

        fun build(w: Int): List<StaticLayout> = listOfNotNull(
            lines.address?.let { text(it, boldPaint, w, maxLines = 3) },
            lines.coordinates?.let { text(it, normalPaint, w, maxLines = 2) },
            lines.dateTime?.let { text(it, normalPaint, w, maxLines = 1) },
        )

        var layouts = build(textWidth(maxMap))
        val spacing = pad * 0.25f
        var contentHeight = layouts.sumOf { it.height.toDouble() }.toFloat() +
            spacing * (layouts.size - 1).coerceAtLeast(0)
        val mapSize = if (showMap) layout.mapSize(contentHeight, width) else 0
        if (showMap && mapSize != maxMap) {
            layouts = build(textWidth(mapSize))
            contentHeight = layouts.sumOf { it.height.toDouble() }.toFloat() +
                spacing * (layouts.size - 1).coerceAtLeast(0)
        }

        val stripHeight = maxOf(contentHeight, mapSize.toFloat()) + pad * 2
        val top = if (settings.atTop) 0f else height - stripHeight

        val bg = Paint().apply {
            color = Color.BLACK
            alpha = (settings.opacityPercent.coerceIn(0, 100) * 255 / 100)
        }
        canvas.drawRect(0f, top, width.toFloat(), top + stripHeight, bg)

        var textLeft = pad
        if (showMap) {
            val mapRect = RectF(pad, top + pad, pad + mapSize, top + pad + mapSize)
            drawMinimap(canvas, mapRect, minimap ?: MinimapRenderer.fallbackCard(mapSize), layout)
            textLeft = mapRect.right + pad
        }

        var y = top + (stripHeight - contentHeight) / 2f
        layouts.forEach { l ->
            canvas.save()
            canvas.translate(textLeft, y)
            l.draw(canvas)
            canvas.restore()
            y += l.height + spacing
        }

        if (showMap && minimap != null) {
            val attribution = TextPaint(normalPaint).apply {
                textSize = layout.attributionSize
                alpha = 200
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(
                "© OpenStreetMap",
                width - pad * 0.6f,
                top + stripHeight - pad * 0.3f,
                attribution
            )
        }
    }

    private fun text(s: CharSequence, paint: TextPaint, width: Int, maxLines: Int): StaticLayout =
        StaticLayout.Builder.obtain(s, 0, s.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(maxLines)
            .build()

    private fun drawMinimap(canvas: Canvas, rect: RectF, map: Bitmap, layout: StampLayout) {
        val save = canvas.save()
        val clip = Path().apply { addRoundRect(rect, layout.cornerRadius, layout.cornerRadius, Path.Direction.CW) }
        canvas.clipPath(clip)
        canvas.drawBitmap(map, null, rect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        canvas.restoreToCount(save)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = layout.cornerRadius * 0.4f
        }
        canvas.drawRoundRect(rect, layout.cornerRadius, layout.cornerRadius, border)
    }
}
