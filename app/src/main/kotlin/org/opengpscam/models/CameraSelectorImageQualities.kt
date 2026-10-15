package org.opengpscam.models

import androidx.camera.core.CameraSelector

data class CameraSelectorImageQualities(
    val camSelector: CameraSelector,
    val qualities: List<MySize>,
)
