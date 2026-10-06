@file:Suppress("MagicNumber") // layout ratios and unit conversions

package org.fossify.camera.stamp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * Renders a small map centred on a fix from OpenStreetMap raster tiles, with a pin.
 * Follows the OSM tile usage policy: identifying User-Agent, on-disk cache, only the 3x3 tiles
 * around the fix, no prefetching. Callers should render off the shutter path and fall back to
 * [fallbackCard] when no map is available.
 */
class MinimapRenderer(context: Context, private val userAgent: String) {

    private val cacheDir = File(context.applicationContext.cacheDir, "osm_tiles").apply { mkdirs() }

    /** Blocking; call from a background thread. Returns null when tiles are unavailable. */
    fun render(latitude: Double, longitude: Double, sizePx: Int = MAP_PX): Bitmap? {
        val n = 1 shl ZOOM
        val worldX = (longitude + 180.0) / 360.0 * n
        val latRad = Math.toRadians(latitude.coerceIn(-85.05, 85.05))
        val worldY = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
        val tileX = floor(worldX).toInt()
        val tileY = floor(worldY).toInt()

        val stitched = Bitmap.createBitmap(TILE * 3, TILE * 3, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(stitched)
        var loaded = 0
        // Fetch the tiles in parallel (a few connections, as the OSM policy asks); draw on this thread.
        val pool = Executors.newFixedThreadPool(TILE_THREADS)
        try {
            val jobs = (-1..1).flatMap { dy ->
                (-1..1).map { dx ->
                    Triple(dx, dy, pool.submit<Bitmap?> { loadTile(Math.floorMod(tileX + dx, n), tileY + dy) })
                }
            }
            for ((dx, dy, job) in jobs) {
                val tile = job.get() ?: continue
                canvas.drawBitmap(tile, ((dx + 1) * TILE).toFloat(), ((dy + 1) * TILE).toFloat(), null)
                tile.recycle()
                loaded++
            }
        } finally {
            pool.shutdown()
        }
        if (loaded == 0) {
            stitched.recycle()
            return null
        }

        val cx = ((worldX - tileX + 1) * TILE).toInt()
        val cy = ((worldY - tileY + 1) * TILE).toInt()
        val half = TILE / 2
        val crop = Bitmap.createBitmap(stitched, cx - half, cy - half, TILE, TILE)
        stitched.recycle()
        val out = Bitmap.createScaledBitmap(crop, sizePx, sizePx, true)
        if (out !== crop) crop.recycle()
        val result = if (out.isMutable) out else out.copy(Bitmap.Config.ARGB_8888, true)
        drawPin(Canvas(result), sizePx / 2f, sizePx / 2f, sizePx * 0.16f)
        return result
    }

    private fun loadTile(x: Int, y: Int): Bitmap? {
        val n = 1 shl ZOOM
        if (y !in 0 until n) return null
        val file = File(cacheDir, "$ZOOM-$x-$y.png")
        if (file.exists() && System.currentTimeMillis() - file.lastModified() < MAX_AGE_MS) {
            BitmapFactory.decodeFile(file.path)?.let { return it }
        }
        return try {
            val url = URL("https://tile.openstreetmap.org/$ZOOM/$x/$y.png")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", userAgent)
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return staleOrNull(file)
                val bytes = conn.inputStream.use { it.readBytes() }
                file.writeBytes(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } finally {
                conn.disconnect()
            }
        } catch (_: java.io.IOException) {
            staleOrNull(file)
        }
    }

    // An expired cached tile is still better than nothing when offline.
    private fun staleOrNull(file: File): Bitmap? =
        if (file.exists()) BitmapFactory.decodeFile(file.path) else null

    companion object {
        private const val ZOOM = 17
        private const val TILE = 256
        private const val TILE_THREADS = 3
        private const val MAP_PX = 384
        private const val TIMEOUT_MS = 4000
        private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

        /** A light grid with a pin, used when no tiles are available. */
        fun fallbackCard(sizePx: Int): Bitmap {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.rgb(0xE8, 0xEC, 0xE4))
            val line = Paint().apply {
                color = Color.rgb(0xC9, 0xD1, 0xC2)
                strokeWidth = (sizePx / 64f).coerceAtLeast(1f)
            }
            val step = sizePx / 4f
            for (i in 1..3) {
                c.drawLine(i * step, 0f, i * step, sizePx.toFloat(), line)
                c.drawLine(0f, i * step, sizePx.toFloat(), i * step, line)
            }
            drawPin(c, sizePx / 2f, sizePx / 2f, sizePx * 0.16f)
            return bmp
        }

        /** Pin whose tip is at ([x], [y]). */
        fun drawPin(canvas: Canvas, x: Float, y: Float, size: Float) {
            val r = size / 2f
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x - r * 0.8f, y - size * 0.75f)
                lineTo(x + r * 0.8f, y - size * 0.75f)
                close()
            }
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0xD9, 0x30, 0x25) }
            canvas.drawPath(path, fill)
            canvas.drawCircle(x, y - size * 0.8f, r, fill)
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawCircle(x, y - size * 0.8f, r * 0.4f, dot)
        }
    }
}
