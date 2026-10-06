package org.fossify.camera.stamp

import android.app.Activity
import android.graphics.Color
import android.text.InputType
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import com.google.android.material.color.MaterialColors
import org.fossify.camera.R
import org.fossify.camera.extensions.config
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.views.MyEditText
import org.fossify.commons.views.MyTextView

/** Popup for the officer name, designation and organisation printed in the photo's top-left corner. */
fun Activity.showOfficerDialog(onSaved: () -> Unit) {
    val pad = resources.getDimensionPixelSize(org.fossify.commons.R.dimen.activity_margin)

    fun field(hint: Int, value: String) = MyEditText(this).apply {
        setHint(hint)
        setText(value)
        setSingleLine()
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }

    val name = field(R.string.stamp_officer_name, config.stampOfficerName)
    val designation = field(R.string.stamp_designation, config.stampDesignation)
    val organisation = field(R.string.stamp_organisation, config.stampOrganisation)
    val note = MyTextView(this).apply {
        setText(R.string.stamp_officer_dialog_hint)
        alpha = NOTE_ALPHA
    }
    val content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(pad, pad / 2, pad, 0)
        listOf(name, designation, organisation, note).forEach {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    getAlertDialogBuilder()
        .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ ->
            config.stampOfficerName = name.text.toString().trim()
            config.stampDesignation = designation.text.toString().trim()
            config.stampOrganisation = organisation.text.toString().trim()
            onSaved()
        }
        .setNeutralButton(R.string.stamp_reset) { _, _ ->
            config.stampOfficerName = ""
            config.stampDesignation = ""
            config.stampOrganisation = ""
            onSaved()
        }
        .setNegativeButton(org.fossify.commons.R.string.cancel, null)
        .apply {
            setupDialogStuff(content, this, R.string.stamp_officer_dialog_title) { dialog ->
                matchDialogColors(dialog, content)
                name.requestFocus()
                name.setSelection(name.text?.length ?: 0)
            }
        }
}

private const val NOTE_ALPHA = 0.7f

/** Popup for the remarks line (case no., site ID, …) printed on every photo until cleared. */
fun Activity.showRemarksDialog(onSaved: () -> Unit) {
    val pad = resources.getDimensionPixelSize(org.fossify.commons.R.dimen.activity_margin)
    val input = MyEditText(this).apply {
        setHint(R.string.stamp_remarks)
        setText(config.stampRemarks)
        maxLines = REMARKS_MAX_LINES
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE
    }
    val note = MyTextView(this).apply {
        setText(R.string.stamp_remarks_hint)
        alpha = NOTE_ALPHA
    }
    val content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(pad, pad / 2, pad, 0)
        listOf(input, note).forEach {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }
    getAlertDialogBuilder()
        .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ ->
            config.stampRemarks = input.text.toString().trim()
            onSaved()
        }
        .setNeutralButton(R.string.stamp_clear) { _, _ ->
            config.stampRemarks = ""
            onSaved()
        }
        .setNegativeButton(org.fossify.commons.R.string.cancel, null)
        .apply {
            setupDialogStuff(content, this, R.string.stamp_remarks) { dialog ->
                matchDialogColors(dialog, content)
                input.requestFocus()
                input.setSelection(input.text?.length ?: 0)
            }
        }
}

private const val REMARKS_MAX_LINES = 3

/**
 * The camera screen keeps a dark theme while the app's text colours follow the system theme, so a
 * popup opened from it could end up with dark text on a dark surface. Pick text colours that
 * contrast with the surface the popup is actually drawn on.
 */
private fun Activity.matchDialogColors(dialog: AlertDialog, content: ViewGroup) {
    val surface = MaterialColors.getColor(dialog.context, com.google.android.material.R.attr.colorSurface, Color.WHITE)
    val isDarkSurface = ColorUtils.calculateLuminance(surface) < DARK_SURFACE_LUMINANCE
    val text = if (isDarkSurface) Color.WHITE else Color.BLACK
    val accent = getProperPrimaryColor()
    content.children.forEach { view ->
        when (view) {
            is MyEditText -> {
                view.setColors(text, accent, surface)
                view.setHintTextColor(ColorUtils.setAlphaComponent(text, HINT_ALPHA))
            }
            is MyTextView -> view.setTextColor(text)
        }
    }
}

private const val DARK_SURFACE_LUMINANCE = 0.5
private const val HINT_ALPHA = 0x80
