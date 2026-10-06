package org.fossify.camera.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import org.fossify.camera.BuildConfig
import org.fossify.camera.R
import org.fossify.camera.databinding.ActivitySettingsBinding
import org.fossify.camera.extensions.checkLocationPermission
import org.fossify.camera.extensions.config
import org.fossify.camera.helpers.CAMERA_UI_DARK
import org.fossify.camera.helpers.CAMERA_UI_LIGHT
import org.fossify.camera.helpers.CAMERA_UI_SYSTEM
import org.fossify.camera.models.CaptureMode
import org.fossify.camera.stamp.QrPlacement
import org.fossify.camera.stamp.StampSettings
import org.fossify.commons.dialogs.*
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.RadioItem
import org.fossify.commons.views.MyEditText
import java.util.*
import kotlin.reflect.KMutableProperty0
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            setContentView(root)
            setupOptionsMenu()
            refreshMenuItems()

            setupEdgeToEdge(padBottomSystem = listOf(settingsNestedScrollview))
            setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsAppbar)
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)

        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupSound()
        setupVolumeButtonsAsShutter()
        setupMaxBrightness()
        setupFlipPhotos()
        setupSavePhotoMetadata()
        setupSavePhotoVideoLocation()
        setupStamp()
        setupSavePhotosFolder()
        setupPhotoQuality()
        setupCaptureMode()
        updateTextColors(binding.settingsHolder)

        val properPrimaryColor = getProperPrimaryColor()
        binding.apply {
            arrayListOf(
                settingsColorCustomizationLabel,
                settingsGeneralSettingsLabel,
                settingsCameraLabel,
                settingsSavingLabel,
                settingsStampLabel,
            ).forEach {
                it.setTextColor(properPrimaryColor)
            }
        }
    }

    private fun refreshMenuItems() {
        binding.settingsToolbar.menu.apply {
            findItem(R.id.more_apps_from_us).isVisible =
                !resources.getBoolean(org.fossify.commons.R.bool.hide_google_relations)
        }
    }

    private fun setupOptionsMenu() {
        binding.settingsToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() = binding.apply {
        settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settingsUseEnglish.isChecked = config.useEnglish
        settingsUseEnglishHolder.setOnClickListener {
            settingsUseEnglish.toggle()
            config.useEnglish = settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() = binding.apply {
        settingsLanguage.text = Locale.getDefault().displayLanguage
        settingsLanguageHolder.beVisibleIf(isTiramisuPlus())

        listOf(settingsGeneralSettingsHolder, settingsGeneralSettingsLabel).forEach {
            it.beGoneIf(settingsUseEnglishHolder.isGone() && settingsPurchaseThankYouHolder.isGone() && settingsLanguageHolder.isGone())
        }

        settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun launchAbout() {
        startActivity(Intent(this, AboutAppActivity::class.java))
    }

    private fun getLastPart(path: String): String {
        val humanized = humanizePath(path)
        return humanized.substringAfterLast("/", humanized)
    }

    private fun setupSound() = binding.apply {
        settingsSound.isChecked = config.isSoundEnabled
        settingsSoundHolder.setOnClickListener {
            settingsSound.toggle()
            config.isSoundEnabled = settingsSound.isChecked
        }
    }

    private fun setupVolumeButtonsAsShutter() = binding.apply {
        settingsVolumeButtonsAsShutter.isChecked = config.volumeButtonsAsShutter
        settingsVolumeButtonsAsShutterHolder.setOnClickListener {
            settingsVolumeButtonsAsShutter.toggle()
            config.volumeButtonsAsShutter = settingsVolumeButtonsAsShutter.isChecked
        }
    }

    private fun setupMaxBrightness() = binding.apply {
        settingsMaxBrightness.isChecked = config.maxBrightness
        settingsMaxBrightnessHolder.setOnClickListener {
            settingsMaxBrightness.toggle()
            config.maxBrightness = settingsMaxBrightness.isChecked
        }
    }

    private fun setupFlipPhotos() = binding.apply {
        settingsFlipPhotos.isChecked = config.flipPhotos
        settingsFlipPhotosHolder.setOnClickListener {
            settingsFlipPhotos.toggle()
            config.flipPhotos = settingsFlipPhotos.isChecked
        }
    }

    private fun setupSavePhotoMetadata() = binding.apply {
        settingsSavePhotoMetadata.isChecked = config.savePhotoMetadata
        settingsSavePhotoMetadataHolder.setOnClickListener {
            settingsSavePhotoMetadata.toggle()
            config.savePhotoMetadata = settingsSavePhotoMetadata.isChecked
        }
    }

    private fun setupSavePhotoVideoLocation() = binding.apply {
        settingsSavePhotoVideoLocation.isChecked = config.savePhotoVideoLocation
        settingsSavePhotoVideoLocationHolder.setOnClickListener {
            val willEnableSavePhotoVideoLocation = !config.savePhotoVideoLocation

            if (willEnableSavePhotoVideoLocation) {
                if (checkLocationPermission()) {
                    updateSavePhotoVideoLocationConfig(true)
                } else {
                    handlePermission(PERMISSION_ACCESS_FINE_LOCATION) { _ ->
                        if (checkLocationPermission()) {
                            updateSavePhotoVideoLocationConfig(true)
                        } else {
                            OpenDeviceSettingsDialog(
                                activity = this@SettingsActivity,
                                message = getString(org.fossify.commons.R.string.allow_location_permission)
                            )
                        }
                    }
                }
            } else {
                updateSavePhotoVideoLocationConfig(false)
            }
        }
    }

    private fun updateSavePhotoVideoLocationConfig(enabled: Boolean) {
        binding.settingsSavePhotoVideoLocation.isChecked = enabled
        config.savePhotoVideoLocation = enabled
    }

    private fun setupSavePhotosFolder() = binding.apply {
        settingsSavePhotosLabel.text = addLockedLabelIfNeeded(R.string.save_photos)
        settingsSavePhotos.text = getLastPart(config.savePhotosFolder)
        settingsSavePhotosHolder.setOnClickListener {
            if (isOrWasThankYouInstalled()) {
                FilePickerDialog(
                    this@SettingsActivity,
                    config.savePhotosFolder,
                    false,
                    showFAB = true
                ) {
                    val path = it
                    handleSAFDialog(it) { success ->
                        if (success) {
                            config.savePhotosFolder = path
                            settingsSavePhotos.text = getLastPart(config.savePhotosFolder)
                        }
                    }
                }
            } else {
                FeatureLockedDialog(this@SettingsActivity) { }
            }
        }
    }

    private fun setupStamp() = binding.apply {
        val toggles = listOf(
            Triple(settingsStampEnabled, settingsStampEnabledHolder, config::stampEnabled),
            Triple(settingsStampShowMap, settingsStampShowMapHolder, config::stampShowMap),
            Triple(settingsStampShowAddress, settingsStampShowAddressHolder, config::stampShowAddress),
            Triple(settingsStampShowCoordinates, settingsStampShowCoordinatesHolder, config::stampShowCoordinates),
            Triple(settingsStampShowAltitude, settingsStampShowAltitudeHolder, config::stampShowAltitude),
            Triple(settingsStampShowCompass, settingsStampShowCompassHolder, config::stampShowCompass),
            Triple(settingsStampShowDateTime, settingsStampShowDateTimeHolder, config::stampShowDateTime),
            Triple(settingsStamp24Hour, settingsStamp24HourHolder, config::stamp24Hour),
            Triple(settingsStampDms, settingsStampDmsHolder, config::stampDms),
            Triple(settingsStampAtTop, settingsStampAtTopHolder, config::stampAtTop),
            Triple(settingsStampKeepOriginal, settingsStampKeepOriginalHolder, config::stampKeepOriginal),
            Triple(settingsStampShowWatermark, settingsStampShowWatermarkHolder, config::stampShowWatermark),
        )
        toggles.forEach { (switch, holder, prop) ->
            switch.isChecked = prop.get()
            holder.setOnClickListener {
                switch.toggle()
                prop.set(switch.isChecked)
            }
        }

        setupStampText(settingsStampOfficerNameHolder, settingsStampOfficerName, R.string.stamp_officer_name, config::stampOfficerName)
        setupStampText(settingsStampDesignationHolder, settingsStampDesignation, R.string.stamp_designation, config::stampDesignation)
        setupStampText(settingsStampOrganisationHolder, settingsStampOrganisation, R.string.stamp_organisation, config::stampOrganisation)

        updateQrPlacement()
        settingsStampQrHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(QrPlacement.OFF.ordinal, getString(R.string.stamp_qr_off)),
                RadioItem(QrPlacement.INSTEAD_OF_MAP.ordinal, getString(R.string.stamp_qr_instead_of_map)),
                RadioItem(QrPlacement.BESIDE_MAP.ordinal, getString(R.string.stamp_qr_beside_map)),
            )
            RadioGroupDialog(this@SettingsActivity, items, config.stampQrPlacement.ordinal) {
                config.stampQrPlacement = QrPlacement.entries[it as Int]
                updateQrPlacement()
            }
        }

        updateCameraUi()
        settingsCameraUiHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(CAMERA_UI_DARK, getString(R.string.camera_ui_dark)),
                RadioItem(CAMERA_UI_LIGHT, getString(R.string.camera_ui_light)),
                RadioItem(CAMERA_UI_SYSTEM, getString(R.string.camera_ui_system)),
            )
            RadioGroupDialog(this@SettingsActivity, items, config.cameraUiMode) {
                config.cameraUiMode = it as Int
                updateCameraUi()
            }
        }

        updateStampOpacity(config.stampOpacity)
        settingsStampOpacityHolder.setOnClickListener {
            val items = StampSettings.OPACITY_CHOICES.map { RadioItem(it, "$it%") }
            RadioGroupDialog(this@SettingsActivity, ArrayList(items), config.stampOpacity) {
                config.stampOpacity = it as Int
                updateStampOpacity(it)
            }
        }
    }

    private fun setupStampText(
        holder: View,
        value: TextView,
        @StringRes title: Int,
        prop: KMutableProperty0<String>,
    ) {
        fun show() {
            value.text = prop.get().ifBlank { getString(R.string.stamp_not_set) }
        }
        show()
        holder.setOnClickListener {
            val input = MyEditText(this).apply {
                setText(prop.get())
                setSingleLine()
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setSelection(text?.length ?: 0)
            }
            val container = FrameLayout(this).apply {
                val pad = resources.getDimensionPixelSize(org.fossify.commons.R.dimen.activity_margin)
                setPadding(pad, pad / 2, pad, 0)
                addView(input)
            }
            getAlertDialogBuilder()
                .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ ->
                    prop.set(input.text.toString().trim())
                    show()
                }
                .setNegativeButton(org.fossify.commons.R.string.cancel, null)
                .apply { setupDialogStuff(container, this, title) { input.requestFocus() } }
        }
    }

    private fun updateQrPlacement() {
        binding.settingsStampQr.setText(
            when (config.stampQrPlacement) {
                QrPlacement.OFF -> R.string.stamp_qr_off
                QrPlacement.INSTEAD_OF_MAP -> R.string.stamp_qr_instead_of_map
                QrPlacement.BESIDE_MAP -> R.string.stamp_qr_beside_map
            }
        )
    }

    private fun updateCameraUi() {
        binding.settingsCameraUi.setText(
            when (config.cameraUiMode) {
                CAMERA_UI_LIGHT -> R.string.camera_ui_light
                CAMERA_UI_SYSTEM -> R.string.camera_ui_system
                else -> R.string.camera_ui_dark
            }
        )
    }

    @SuppressLint("SetTextI18n")
    private fun updateStampOpacity(opacity: Int) {
        binding.settingsStampOpacity.text = "$opacity%"
    }

    private fun setupPhotoQuality() {
        updatePhotoQuality(config.photoQuality)
        binding.settingsPhotoQualityHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(100, "100%"),
                RadioItem(95, "95%"),
                RadioItem(90, "90%"),
                RadioItem(85, "85%"),
                RadioItem(80, "80%"),
                RadioItem(75, "75%"),
                RadioItem(70, "70%"),
                RadioItem(65, "65%"),
                RadioItem(60, "60%"),
                RadioItem(55, "55%"),
                RadioItem(50, "50%")
            )

            RadioGroupDialog(this@SettingsActivity, items, config.photoQuality) {
                config.photoQuality = it as Int
                updatePhotoQuality(it)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePhotoQuality(quality: Int) {
        binding.settingsPhotoQuality.text = "$quality%"
    }

    private fun setupCaptureMode() {
        updateCaptureMode(config.captureMode)
        binding.settingsCaptureModeHolder.setOnClickListener {
            val items = CaptureMode.values().mapIndexed { index, captureMode ->
                RadioItem(index, getString(captureMode.stringResId), captureMode)
            }

            RadioGroupDialog(this@SettingsActivity, ArrayList(items), config.captureMode.ordinal) {
                config.captureMode = it as CaptureMode
                updateCaptureMode(it)
            }
        }
    }

    private fun updateCaptureMode(captureMode: CaptureMode) {
        binding.settingsCaptureMode.text = getString(captureMode.stringResId)
    }
}
