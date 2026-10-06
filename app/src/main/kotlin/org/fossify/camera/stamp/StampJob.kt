package org.fossify.camera.stamp

import android.graphics.Bitmap

/** Everything the image saver needs to burn the strip into one photo. */
class StampJob(
    val data: StampData,
    val minimap: Bitmap?,
    val settings: StampSettings,
    val watermarkIcon: Bitmap? = null,
)
