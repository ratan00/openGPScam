@file:Suppress("MagicNumber") // layout ratios and unit conversions

package org.fossify.camera.stamp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import org.fossify.camera.R

/** Live preview of the strip over the viewfinder, drawn with the same code as the saved photo. */
class StampOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs), StampController.Listener {

    /** Called when the officer card (or its placeholder) is tapped. */
    var onOfficerClick: (() -> Unit)? = null

    /** Receives the y (in this view) above which floating controls won't cover the label. */
    var onFreeBottomChanged: ((Float) -> Unit)? = null

    private var officerRect: RectF? = null
    private var contentAspect: Float? = null
    private var lastFreeBottom = Float.NaN
    private var officerPressed = false

    private var controller: StampController? = null
    private var settings: (() -> StampSettings)? = null
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(160, 0, 0, 0) }
    private val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            postDelayed(this, TICK_MS)
        }
    }

    fun bind(controller: StampController, settings: () -> StampSettings) {
        this.controller?.listener = null
        this.controller = controller
        this.settings = settings
        controller.listener = this
        visibility = VISIBLE
        removeCallbacks(tick)
        post(tick)
    }

    fun unbind() {
        removeCallbacks(tick)
        controller?.listener = null
        controller = null
        settings = null
        visibility = GONE
    }

    override fun onStampInputsChanged() {
        postInvalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val c = controller ?: return
        val s = settings?.invoke() ?: return
        val data = c.currentData()
        val area = contentRect()
        if (area.width() <= 0 || area.height() <= 0) return
        canvas.save()
        canvas.translate(area.left, area.top)
        val preview = s.copy(officerPlaceholder = context.getString(R.string.stamp_officer_placeholder))
        val bounds = PhotoStamper.drawStrip(
            canvas, area.width().toInt(), area.height().toInt(), data, c.currentMinimap(), preview, c.watermarkIcon
        )
        canvas.restore()
        officerRect = bounds.officer?.apply { offset(area.left, area.top) }
        val label = bounds.label?.apply { offset(area.left, area.top) }
        // Space above a bottom label (or the image bottom) for floating controls like the zoom pill.
        val freeBottom = if (label != null && !s.atTop) label.top else area.bottom
        if (freeBottom != lastFreeBottom) {
            lastFreeBottom = freeBottom
            onFreeBottomChanged?.invoke(freeBottom)
        }
        // Below the officer card so the two never overlap.
        val pillTop = (officerRect?.bottom ?: area.top).toInt()
        when {
            data.isMock -> drawWaitingPill(canvas, pillTop, context.getString(R.string.mock_location_warning_short))
            !data.hasFix -> drawWaitingPill(canvas, pillTop, "Waiting for GPS…")
        }
    }

    /**
     * Where the camera image actually is inside this view (the preview letterboxes non-matching
     * aspect ratios), minus the parts hidden behind the camera controls.
     */
    private fun contentRect(): RectF {
        val aspect = contentAspect
        var w = width.toFloat()
        var h = height.toFloat()
        if (aspect != null) {
            h = w * aspect
            if (h > height) {
                h = height.toFloat()
                w = h / aspect
            }
        }
        val left = (width - w) / 2f
        val top = (height - h) / 2f
        return RectF(
            left,
            maxOf(top, coveredTop().toFloat()),
            left + w,
            minOf(top + h, (height - coveredBottom()).toFloat()),
        )
    }

    /** Sets the photo's size so the label sits on the image, not on the letterbox bars. */
    fun setContentSize(photoWidth: Int, photoHeight: Int) {
        val long = maxOf(photoWidth, photoHeight).toFloat()
        val short = minOf(photoWidth, photoHeight).toFloat()
        // The activity is always portrait, so the image is shown tall.
        contentAspect = if (short > 0) long / short else null
        invalidate()
    }

    /** Only taps on the officer card are handled; everything else reaches the preview below. */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rect = officerRect
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                officerPressed = rect != null && onOfficerClick != null && rect.contains(event.x, event.y)
                return officerPressed
            }
            MotionEvent.ACTION_UP -> {
                if (officerPressed && rect?.contains(event.x, event.y) == true) {
                    playSoundEffect(SoundEffectConstants.CLICK)
                    onOfficerClick?.invoke()
                }
                officerPressed = false
            }
            MotionEvent.ACTION_CANCEL -> officerPressed = false
        }
        return officerPressed || event.actionMasked == MotionEvent.ACTION_UP
    }

    private fun sibling(id: Int): View? =
        (parent as? View)?.findViewById<View>(id)?.takeIf { it.visibility == VISIBLE && it.height > 0 }

    /** How far the top controls reach into this view. */
    private fun coveredTop(): Int = sibling(R.id.top_options)?.let { (it.bottom - top).coerceAtLeast(0) } ?: 0

    /** How far the bottom controls reach into this view. */
    private fun coveredBottom(): Int =
        sibling(R.id.bottom_overlay)?.let { (bottom - it.top).coerceAtLeast(0) } ?: 0

    private fun drawWaitingPill(canvas: Canvas, topOffset: Int, text: String) {
        val short = minOf(width, height).toFloat()
        pillText.textSize = short * 0.035f
        val w = pillText.measureText(text) + short * 0.06f
        val h = pillText.textSize * 1.9f
        val y = topOffset + short * 0.03f
        val rect = RectF(width / 2f - w / 2, y, width / 2f + w / 2, y + h)
        canvas.drawRoundRect(rect, h / 2, h / 2, pillPaint)
        canvas.drawText(text, rect.centerX(), rect.centerY() + pillText.textSize * 0.35f, pillText)
    }

    private companion object {
        const val TICK_MS = 5000L
    }
}
