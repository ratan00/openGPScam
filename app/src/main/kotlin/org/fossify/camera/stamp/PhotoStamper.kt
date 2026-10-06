@file:Suppress("MagicNumber") // layout ratios and unit conversions

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

/**
 * Draws the label: one rounded card floating above the bottom edge with the minimap inset on its
 * left, a small app watermark above the card and an optional officer card in the top-left corner.
 * [stamp] is used for photos, [drawStrip] also drives the live overlay.
 */
object PhotoStamper {

    /** Returns a bitmap with the label burned in. Mutates and returns [src] when it is mutable. */
    fun stamp(
        src: Bitmap,
        data: StampData,
        minimap: Bitmap?,
        settings: StampSettings,
        watermarkIcon: Bitmap? = null,
    ): Bitmap {
        val target = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
        drawStrip(Canvas(target), target.width, target.height, data, minimap, settings, watermarkIcon)
        return target
    }

    /**
     * Draws the label into [canvas] covering a [width] x [height] area at its origin.
     * Returns where things were drawn, so the live overlay can react to taps and place controls.
     */
    @Suppress("LongParameterList")
    fun drawStrip(
        canvas: Canvas,
        width: Int,
        height: Int,
        data: StampData,
        minimap: Bitmap?,
        settings: StampSettings,
        watermarkIcon: Bitmap? = null,
    ): StampBounds {
        val layout = StampLayout.forImage(width, height)
        val label = drawLocationCard(canvas, width, height, data, minimap, settings, watermarkIcon, layout)
        val officerTop = if (settings.atTop && label != null) label.bottom + layout.gap else layout.margin
        val officer = drawOfficerCard(canvas, width, settings, layout, officerTop)
        return StampBounds(label, officer)
    }

    /** Returns the area covered by the card and its watermark, or null when nothing was drawn. */
    @Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
    private fun drawLocationCard(
        canvas: Canvas,
        width: Int,
        height: Int,
        data: StampData,
        minimap: Bitmap?,
        settings: StampSettings,
        watermarkIcon: Bitmap?,
        layout: StampLayout,
    ): RectF? {
        val lines = StampFormatter.lines(data, settings)
        // "Instead of map" puts the QR where the map would be.
        val showMap = settings.showMap && settings.qrPlacement != QrPlacement.INSTEAD_OF_MAP
        if (lines.isEmpty && !showMap && settings.qrPlacement == QrPlacement.OFF) return null

        val pad = layout.padding
        val margin = layout.margin

        val headlinePaint = textPaint(layout.headlineSize, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
        val bodyPaint = textPaint(layout.textSize, Typeface.DEFAULT)
        val smallPaint = textPaint(layout.smallTextSize, Typeface.DEFAULT)
        val datePaint = textPaint(layout.textSize * DATE_SCALE, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))

        // One rounded card: map tile on the left, text, QR tile on the right, all inset by the
        // padding. Measure with the largest tiles first, then size them to the text and re-measure.
        val showQr = settings.qrPlacement != QrPlacement.OFF
        val qrOnLeft = settings.qrPlacement == QrPlacement.INSTEAD_OF_MAP
        val tiles = (if (showMap) 1 else 0) + (if (showQr) 1 else 0)
        val maxTileFraction = if (tiles > 1) StampLayout.MAX_TWO_TILE_FRACTION else StampLayout.MAX_MAP_WIDTH_FRACTION
        val maxMap = (width * maxTileFraction).toInt()
        fun textWidth(mapSize: Int) =
            (width - margin * 2 - pad * 2 - tiles * (mapSize + pad)).toInt().coerceAtLeast(1)

        fun build(w: Int): List<StaticLayout> = listOfNotNull(
            lines.headline?.let { text(it, headlinePaint, w, maxLines = 2) },
            lines.address?.let { text(it, bodyPaint, w, maxLines = 3) },
            lines.coordinates?.let { text(it, smallPaint, w, maxLines = 2) },
            lines.dateTime?.let { text(it, datePaint, w, maxLines = 2) },
            lines.remarks?.let { text(it, bodyPaint, w, maxLines = 2) },
        )

        val spacing = pad * 0.2f
        fun heightOf(ls: List<StaticLayout>) =
            ls.sumOf { it.height.toDouble() }.toFloat() + spacing * (ls.size - 1).coerceAtLeast(0)

        var layouts = build(textWidth(maxMap))
        var contentHeight = heightOf(layouts)
        val mapSize = if (tiles > 0) layout.mapSize(contentHeight, width).coerceAtMost(maxMap) else 0
        if (tiles > 0 && mapSize != maxMap) {
            layouts = build(textWidth(mapSize))
            contentHeight = heightOf(layouts)
        }

        val cardHeight = maxOf(contentHeight, mapSize.toFloat()) + pad * 2
        val top = if (settings.atTop) margin else height - margin - cardHeight
        val bottom = top + cardHeight
        val radius = layout.cornerRadius

        val cardRect = RectF(margin, top, width - margin, bottom)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = settings.opacityPercent.coerceIn(0, 100) * 255 / 100
        }
        canvas.drawRoundRect(cardRect, radius, radius, bg)

        var textLeft = cardRect.left + pad
        val tileRadius = (radius - pad).coerceAtLeast(radius * 0.45f)
        if (showQr) {
            val qrTop = top + (cardHeight - mapSize) / 2f
            val qrLeft = if (qrOnLeft) textLeft else cardRect.right - pad - mapSize
            val qrRect = RectF(qrLeft, qrTop, qrLeft + mapSize, qrTop + mapSize)
            StampFormatter.qrText(data)?.let { QrRenderer.draw(canvas, qrRect, it, tileRadius) }
            if (qrOnLeft) textLeft = qrRect.right + pad
        }
        if (showMap) {
            val mapTop = top + (cardHeight - mapSize) / 2f
            val mapRect = RectF(textLeft, mapTop, textLeft + mapSize, mapTop + mapSize)
            // Concentric corners: inner radius = outer radius - inset.
            val mapRadius = tileRadius
            drawMinimap(canvas, mapRect, minimap ?: MinimapRenderer.fallbackCard(mapSize), mapRadius)
            if (minimap != null) drawAttribution(canvas, mapRect, mapRadius, layout)
            textLeft = mapRect.right + pad
        }

        var y = top + (cardHeight - contentHeight) / 2f
        layouts.forEach { l ->
            canvas.save()
            canvas.translate(textLeft, y)
            l.draw(canvas)
            canvas.restore()
            y += l.height + spacing
        }

        if (settings.showWatermark) {
            drawWatermark(canvas, settings.appName, watermarkIcon, layout, cardRect.right, top, bottom, settings.atTop)
                .let(cardRect::union)
        }
        return cardRect
    }

    /** Small "icon + app name" pill just outside the card's top-right corner. */
    @Suppress("LongParameterList")
    private fun drawWatermark(
        canvas: Canvas,
        appName: String,
        icon: Bitmap?,
        layout: StampLayout,
        right: Float,
        cardTop: Float,
        cardBottom: Float,
        below: Boolean,
    ): RectF {
        val paint = textPaint(layout.watermarkSize, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        val hPad = layout.watermarkSize * 0.5f
        val iconSize = layout.watermarkSize * 1.35f
        val pillHeight = iconSize + hPad
        val textWidth = paint.measureText(appName)
        val pillWidth = hPad + (if (icon != null) iconSize + hPad * 0.6f else 0f) + textWidth + hPad
        val pillTop = if (below) cardBottom + layout.gap * 0.6f else cardTop - layout.gap * 0.6f - pillHeight
        val pill = RectF(right - pillWidth, pillTop, right, pillTop + pillHeight)

        paint.alpha = 220
        paint.setShadowLayer(layout.watermarkSize * 0.25f, 0f, 0f, Color.argb(160, 0, 0, 0))

        var x = pill.left + hPad
        if (icon != null) {
            val iconRect = RectF(x, pill.centerY() - iconSize / 2, x + iconSize, pill.centerY() + iconSize / 2)
            canvas.drawBitmap(icon, null, iconRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            x = iconRect.right + hPad * 0.6f
        }
        val baseline = pill.centerY() - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(appName, x, baseline, paint)
        return pill
    }

    /**
     * Officer name, designation and organisation in the top-left corner. When all are blank nothing is drawn,
     * except the dimmed [StampSettings.officerPlaceholder] used by the live preview.
     */
    @Suppress("LongMethod")
    private fun drawOfficerCard(
        canvas: Canvas,
        width: Int,
        settings: StampSettings,
        layout: StampLayout,
        top: Float,
    ): RectF? {
        var name = settings.officerName.trim()
        val designation = settings.designation.trim()
        val org = settings.organisation.trim()
        val hasText = name.isNotEmpty() || designation.isNotEmpty() || org.isNotEmpty()
        val isPlaceholder = !hasText
        if (isPlaceholder) name = settings.officerPlaceholder ?: return null

        val pad = layout.padding * 0.8f
        val namePaint = if (isPlaceholder) {
            textPaint(layout.textSize, Typeface.DEFAULT).apply { alpha = 190 }
        } else {
            textPaint(layout.textSize * 1.15f, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        }
        val orgPaint = textPaint(layout.textSize, Typeface.DEFAULT)

        val texts = listOfNotNull(
            name.takeIf { it.isNotEmpty() }?.let { it to namePaint },
            designation.takeIf { it.isNotEmpty() }?.let { it to orgPaint },
            org.takeIf { it.isNotEmpty() }?.let { it to orgPaint },
        )
        val maxCard = width * StampLayout.MAX_OFFICER_WIDTH_FRACTION
        val spacing = pad * 0.2f
        fun build(maxText: Int): List<StaticLayout> {
            if (texts.isEmpty()) return emptyList()
            val wanted = texts.maxOf { (t, p) -> p.measureText(t) }.toInt() + 1
            return texts.map { (t, p) -> text(t, p, wanted.coerceAtMost(maxText.coerceAtLeast(1)), maxLines = 2) }
        }
        fun heightOf(ls: List<StaticLayout>) =
            ls.sumOf { it.height.toDouble() }.toFloat() + spacing * (ls.size - 1).coerceAtLeast(0)

        val layouts = build((maxCard - pad * 2).toInt())
        val contentHeight = heightOf(layouts)
        val textWidth = layouts.maxOfOrNull { l -> (0 until l.lineCount).maxOf { l.getLineWidth(it) } } ?: 0f

        val rect = RectF(layout.margin, top, layout.margin + textWidth + pad * 2, top + contentHeight + pad * 2)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = settings.opacityPercent.coerceIn(0, 100) * 255 / 100 / if (isPlaceholder) 2 else 1
        }
        val radius = layout.cornerRadius * 0.7f
        canvas.drawRoundRect(rect, radius, radius, bg)

        var y = rect.top + pad + (contentHeight - heightOf(layouts)) / 2f
        layouts.forEach { l ->
            canvas.save()
            canvas.translate(rect.left + pad, y)
            l.draw(canvas)
            canvas.restore()
            y += l.height + spacing
        }
        return rect
    }

    /** "© OpenStreetMap" on a small light pill in the map's bottom-right corner. */
    private fun drawAttribution(canvas: Canvas, mapRect: RectF, mapRadius: Float, layout: StampLayout) {
        val paint = textPaint(layout.attributionSize, Typeface.create(Typeface.DEFAULT, Typeface.BOLD)).apply {
            color = Color.argb(230, 40, 40, 40)
        }
        val text = "© OpenStreetMap"
        val hPad = layout.attributionSize * 0.45f
        val w = paint.measureText(text) + hPad * 2
        val h = layout.attributionSize * 1.45f
        // Keep it inside the rounded corner.
        val inset = mapRadius * 0.3f
        val pill = RectF(mapRect.right - inset - w, mapRect.bottom - inset - h, mapRect.right - inset, mapRect.bottom - inset)
        canvas.drawRoundRect(pill, h / 2, h / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 255, 255, 255) })
        canvas.drawText(text, pill.left + hPad, pill.centerY() - (paint.descent() + paint.ascent()) / 2, paint)
    }

    private fun textPaint(size: Float, face: Typeface) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = size
        typeface = face
    }

    private fun text(s: CharSequence, paint: TextPaint, width: Int, maxLines: Int): StaticLayout =
        StaticLayout.Builder.obtain(s, 0, s.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(maxLines)
            .build()

    private fun drawMinimap(canvas: Canvas, rect: RectF, map: Bitmap, radius: Float) {
        val save = canvas.save()
        val clip = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        canvas.clipPath(clip)
        canvas.drawBitmap(map, null, rect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        canvas.restoreToCount(save)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(180, 255, 255, 255)
            strokeWidth = radius * 0.08f
        }
        canvas.drawRoundRect(rect, radius, radius, border)
    }
}

private const val DATE_SCALE = 1.08f

/** [label] covers the location card and watermark; [officer] the officer card. */
class StampBounds(val label: RectF?, val officer: RectF?)
