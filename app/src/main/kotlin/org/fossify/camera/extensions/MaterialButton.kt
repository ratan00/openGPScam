package org.fossify.camera.extensions

import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import org.fossify.camera.R
import org.fossify.camera.views.ShadowDrawable

fun MaterialButton.setShadowIcon(@DrawableRes drawableResId: Int) {
    // Light controls are tinted dark; the shadow bitmap would ignore the tint.
    icon = if (context.isLightCameraUi()) {
        ContextCompat.getDrawable(context, drawableResId)
    } else {
        ShadowDrawable(context, drawableResId, R.style.TopIconShadow)
    }
}
