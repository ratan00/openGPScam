package org.fossify.camera.activities

import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import org.fossify.camera.BuildConfig
import org.fossify.camera.R
import org.fossify.camera.databinding.ActivityAboutAppBinding
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.launchViewIntent
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.views.MyTextView

/**
 * OpenGPS Cam's own About screen. Links point to this project; Fossify, the upstream project,
 * keeps its GitHub page and donation link.
 */
class AboutAppActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityAboutAppBinding::inflate)

    private class Link(@StringRes val title: Int, val subtitle: String, val url: String)

    private val links by lazy {
        listOf(
            Link(R.string.about_source_code, REPO_URL.removePrefix("https://"), REPO_URL),
            Link(R.string.about_report_issue, "$REPO_URL/issues".removePrefix("https://"), "$REPO_URL/issues"),
            Link(R.string.about_developer, "github.com/$DEVELOPER_GITHUB", "https://github.com/$DEVELOPER_GITHUB"),
            Link(R.string.about_contact, DEVELOPER_EMAIL, "mailto:$DEVELOPER_EMAIL?subject=${getString(R.string.app_name)}"),
            Link(R.string.about_fossify_github, getString(R.string.about_fossify_github_sub), FOSSIFY_GITHUB_URL),
            Link(R.string.about_fossify_donate, getString(R.string.about_fossify_donate_sub), FOSSIFY_DONATE_URL),
            Link(R.string.about_license, "GNU GPL v3.0", LICENSE_URL),
            Link(R.string.about_map_data, "© OpenStreetMap contributors", OSM_COPYRIGHT_URL),
            Link(R.string.about_qr_library, "ZXing, Apache License 2.0", ZXING_URL),
            Link(R.string.about_privacy, getString(R.string.about_privacy_sub), "$REPO_URL#privacy"),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(padBottomSystem = listOf(binding.aboutScrollview))
        setupMaterialScrollListener(binding.aboutScrollview, binding.aboutAppbar)

        binding.aboutIcon.setImageDrawable(packageManager.getApplicationIcon(applicationInfo))
        binding.aboutName.text = getString(R.string.about_name_version, getString(R.string.app_name), BuildConfig.VERSION_NAME)
        links.forEach { addLink(it) }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.aboutAppbar, NavigationIcon.Arrow)
        updateTextColors(binding.aboutHolder)
    }

    private fun addLink(link: Link) {
        val pad = resources.getDimensionPixelSize(org.fossify.commons.R.dimen.activity_margin)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, pad / 2 + pad / 4, pad, pad / 2 + pad / 4)
            isClickable = true
            isFocusable = true
            val ripple = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)
            setBackgroundResource(ripple.resourceId)
            setOnClickListener { launchViewIntent(link.url) }
        }
        row.addView(MyTextView(this).apply {
            setText(link.title)
            textSize = TITLE_SP
        })
        row.addView(MyTextView(this).apply {
            text = link.subtitle
            alpha = SUBTITLE_ALPHA
            setTextColor(getProperPrimaryColor())
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 2
        })
        binding.aboutLinks.addView(
            row,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }

    private companion object {
        const val REPO_URL = "https://github.com/ratan00/OpenGPSCam"
        const val DEVELOPER_GITHUB = "ratan00"
        const val DEVELOPER_EMAIL = "abhishekrat123@gmail.com"
        const val FOSSIFY_GITHUB_URL = "https://github.com/FossifyOrg"
        const val FOSSIFY_DONATE_URL = "https://www.fossify.org/donate/"
        const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
        const val OSM_COPYRIGHT_URL = "https://www.openstreetmap.org/copyright"
        const val ZXING_URL = "https://github.com/zxing/zxing"
        const val TITLE_SP = 16f
        const val SUBTITLE_ALPHA = 0.85f
    }
}
