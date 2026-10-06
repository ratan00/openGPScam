package org.fossify.camera.stamp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Live preview of the strip over the viewfinder, drawn with the same code as the saved photo. */
class StampOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs), StampController.Listener {

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
        PhotoStamper.drawStrip(canvas, width, height, data, c.currentMinimap(), s)
        if (!data.hasFix) drawWaitingPill(canvas)
    }

    private fun drawWaitingPill(canvas: Canvas) {
        val short = minOf(width, height).toFloat()
        pillText.textSize = short * 0.035f
        val text = "Waiting for GPS…"
        val w = pillText.measureText(text) + short * 0.06f
        val h = pillText.textSize * 1.9f
        val rect = RectF(width / 2f - w / 2, short * 0.03f, width / 2f + w / 2, short * 0.03f + h)
        canvas.drawRoundRect(rect, h / 2, h / 2, pillPaint)
        canvas.drawText(text, rect.centerX(), rect.centerY() + pillText.textSize * 0.35f, pillText)
    }

    private companion object {
        const val TICK_MS = 5000L
    }
}
