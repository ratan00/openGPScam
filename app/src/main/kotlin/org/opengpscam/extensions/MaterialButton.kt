package org.opengpscam.extensions

import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import org.opengpscam.R
import org.opengpscam.views.ShadowDrawable

fun MaterialButton.setShadowIcon(@DrawableRes drawableResId: Int) {
    // Light controls are tinted dark; the shadow bitmap would ignore the tint.
    icon = if (context.isLightCameraUi()) {
        ContextCompat.getDrawable(context, drawableResId)
    } else {
        ShadowDrawable(context, drawableResId, R.style.TopIconShadow)
    }
}
