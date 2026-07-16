package com.vishwajitrajput.musetraceai.feature.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.core.common.LegalCopy
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.core.ui.SfCard
import com.vishwajitrajput.musetraceai.core.ui.SfChip
import com.vishwajitrajput.musetraceai.core.ui.SfGhostButton
import com.vishwajitrajput.musetraceai.core.ui.SfSlider
import com.vishwajitrajput.musetraceai.databinding.FragmentSettingsBinding
import com.vishwajitrajput.musetraceai.domain.model.AppSettings
import com.vishwajitrajput.musetraceai.domain.model.AppTheme
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfileType
import com.vishwajitrajput.musetraceai.domain.model.CanvasQuality
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {
    private val viewModel: SettingsViewModel by viewModels()
    private var applyingState = false
    private lateinit var advancedControlsCard: View
    private lateinit var simpleModeGroup: ChipGroup
    private lateinit var simpleChip: SfChip
    private lateinit var advancedChip: SfChip
    private lateinit var drawingSpeedSlider: SfSlider
    private lateinit var drawingSpeedValue: TextView
    private lateinit var smoothingSlider: SfSlider
    private lateinit var smoothingValue: TextView
    private lateinit var simplificationSlider: SfSlider
    private lateinit var simplificationValue: TextView
    private lateinit var minimumStrokeSlider: SfSlider
    private lateinit var minimumStrokeValue: TextView
    private lateinit var gestureDelaySlider: SfSlider
    private lateinit var gestureDelayValue: TextView
    private lateinit var canvasQualityGroup: ChipGroup
    private lateinit var performanceChip: SfChip
    private lateinit var balancedChip: SfChip
    private lateinit var highQualityChip: SfChip
    private lateinit var calibrationGroup: ChipGroup
    private val calibrationChips = LinkedHashMap<CalibrationProfileType, SfChip>()
    private lateinit var overlayOpacitySlider: SfSlider
    private lateinit var overlayOpacityValue: TextView
    private lateinit var overlaySizeSlider: SfSlider
    private lateinit var overlaySizeValue: TextView
    private lateinit var themeGroup: ChipGroup
    private lateinit var systemThemeChip: SfChip
    private lateinit var darkThemeChip: SfChip
    private lateinit var lightThemeChip: SfChip
    private lateinit var crashRecoverySwitch: MaterialSwitch
    private lateinit var keepAwakeSwitch: MaterialSwitch

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsDisclaimerText.text = LegalCopy.DISCLAIMER
        binding.settingsDrawingWarningText.text = """
            Instagram Draw availability depends on account, app version, region, and device.
            MuseTrace AI cannot enable Instagram Draw.
            Leaving Instagram Draw may delete your current drawing.

            ${LegalCopy.DRAWING_WARNING}

            ${LegalCopy.RESUME_WARNING}

            ${LegalCopy.RESUME_EXPLANATION}
        """.trimIndent()
        buildProductionSettingsUi()
        listOf(binding.chip16, binding.chip24, binding.chip32).forEach { it.isCheckable = true }
        binding.saveKeyButton.setOnClickListener {
            viewModel.saveKey(binding.apiKeyInput.text?.toString().orEmpty())
            binding.apiKeyInput.text?.clear()
        }
        binding.testKeyButton.setOnClickListener { viewModel.testKey() }
        binding.clearKeyButton.setOnClickListener { viewModel.clearKey() }
        binding.defaultColorGroup.setOnCheckedStateChangeListener { _, _ ->
            if (!applyingState) viewModel.saveDefaultColorCount(selectedColorCount())
        }
        collectStarted {
            launch {
                viewModel.settings.collect { settings ->
                    applyingState = true
                    renderSettings(settings)
                    applyingState = false
                }
            }
            launch {
                viewModel.status.collect { status ->
                    binding.settingsStatus.text = when {
                        status.isNotBlank() -> status
                        viewModel.hasKey() -> "A Gemini key is stored securely."
                        else -> "No Gemini key is stored."
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.settingsStatus.text = if (viewModel.hasKey()) {
            "A Gemini key is stored securely."
        } else {
            "No Gemini key is stored."
        }
    }

    private fun selectedColorCount(): Int = when (binding.defaultColorGroup.checkedChipId) {
        R.id.chip24 -> 24
        R.id.chip32 -> 32
        else -> 16
    }

    private fun buildProductionSettingsUi() {
        val container = binding.settingsContainer
        container.removeView(binding.settingsStatus)
        container.addView(modeCard())
        advancedControlsCard = buildAdvancedControlsCard()
        container.addView(advancedControlsCard)
        container.addView(calibrationCard())
        container.addView(overlayCard())
        container.addView(appBehaviorCard())
        container.addView(binding.settingsStatus)
    }

    private fun modeCard(): SfCard =
        sectionCard("Mode", "Simple Mode uses safe drawing defaults. Advanced Mode shows technical drawing controls.") {
            simpleModeGroup = chipGroup().also { group ->
                simpleChip = chip("Simple Mode")
                advancedChip = chip("Advanced Mode")
                group.addView(simpleChip)
                group.addView(advancedChip)
                group.setOnCheckedStateChangeListener { _, checkedIds ->
                    if (!applyingState) {
                        val checked = checkedIds.firstOrNull()
                        viewModel.setSimpleMode(checked != advancedChip.id)
                    }
                }
                addView(group)
            }
        }

    private fun buildAdvancedControlsCard(): SfCard =
        sectionCard("Drawing engine", "Tune gesture generation for complex sketches.") {
            drawingSpeedSlider = settingSlider(
                title = "Drawing speed",
                valueFrom = 0.35f,
                valueTo = 2.5f,
                step = 0.05f,
                formatter = { "${String.format("%.2f", it)}x" },
                onChanged = viewModel::saveDrawingSpeed,
            ).also { drawingSpeedValue = it.second }.first
            smoothingSlider = settingSlider(
                title = "Smoothing level",
                valueFrom = 0f,
                valueTo = 1f,
                step = 0.05f,
                formatter = { "${(it * 100f).roundToInt()}%" },
                onChanged = viewModel::saveSmoothingLevel,
            ).also { smoothingValue = it.second }.first
            simplificationSlider = settingSlider(
                title = "Simplification level",
                valueFrom = 0f,
                valueTo = 1f,
                step = 0.05f,
                formatter = { "${(it * 100f).roundToInt()}%" },
                onChanged = viewModel::saveSimplificationLevel,
            ).also { simplificationValue = it.second }.first
            minimumStrokeSlider = settingSlider(
                title = "Minimum stroke length",
                valueFrom = 1f,
                valueTo = 40f,
                step = 1f,
                formatter = { "${it.roundToInt()} px" },
                onChanged = viewModel::saveMinimumStrokeLength,
            ).also { minimumStrokeValue = it.second }.first
            gestureDelaySlider = settingSlider(
                title = "Gesture delay",
                valueFrom = 0f,
                valueTo = 800f,
                step = 20f,
                formatter = { "${it.roundToInt()} ms" },
                onChanged = { viewModel.saveGestureDelay(it.roundToInt().toLong()) },
            ).also { gestureDelayValue = it.second }.first
            addLabel("Canvas quality")
            canvasQualityGroup = chipGroup().also { group ->
                performanceChip = chip(CanvasQuality.Performance.displayName)
                balancedChip = chip(CanvasQuality.Balanced.displayName)
                highQualityChip = chip(CanvasQuality.High.displayName)
                group.addView(performanceChip)
                group.addView(balancedChip)
                group.addView(highQualityChip)
                group.setOnCheckedStateChangeListener { _, checkedIds ->
                    if (!applyingState) {
                        when (checkedIds.firstOrNull()) {
                            performanceChip.id -> viewModel.saveCanvasQuality(CanvasQuality.Performance)
                            highQualityChip.id -> viewModel.saveCanvasQuality(CanvasQuality.High)
                            else -> viewModel.saveCanvasQuality(CanvasQuality.Balanced)
                        }
                    }
                }
                addView(group)
            }
        }

    private fun calibrationCard(): SfCard =
        sectionCard("Calibration", "Choose which manually calibrated Instagram Draw area is active.") {
            calibrationGroup = chipGroup().also { group ->
                CalibrationProfileType.entries.forEach { type ->
                    val item = chip(type.displayName)
                    calibrationChips[type] = item
                    group.addView(item)
                }
                group.setOnCheckedStateChangeListener { _, checkedIds ->
                    if (!applyingState) {
                        val selected = calibrationChips.entries.firstOrNull { it.value.id == checkedIds.firstOrNull() }?.key
                        selected?.let(viewModel::selectCalibrationProfile)
                    }
                }
                addView(group)
            }
        }

    private fun overlayCard(): SfCard =
        sectionCard("Floating overlay", "Adjust the controller that stays above Instagram Draw.") {
            overlayOpacitySlider = settingSlider(
                title = "Overlay opacity",
                valueFrom = 0.48f,
                valueTo = 1f,
                step = 0.02f,
                formatter = { "${(it * 100f).roundToInt()}%" },
                onChanged = viewModel::saveOverlayOpacity,
            ).also { overlayOpacityValue = it.second }.first
            overlaySizeSlider = settingSlider(
                title = "Overlay size",
                valueFrom = 0.8f,
                valueTo = 1.35f,
                step = 0.05f,
                formatter = { "${(it * 100f).roundToInt()}%" },
                onChanged = viewModel::saveOverlaySize,
            ).also { overlaySizeValue = it.second }.first
            addView(SfGhostButton(requireContext()).apply {
                text = "Reset overlay position"
                setOnClickListener { viewModel.resetOverlayPosition() }
            }, matchWrap().apply { topMargin = space(8) })
        }

    private fun appBehaviorCard(): SfCard =
        sectionCard("App behavior", "Theme, recovery, and screen behavior.") {
            addLabel("Theme")
            themeGroup = chipGroup().also { group ->
                systemThemeChip = chip(AppTheme.System.displayName)
                darkThemeChip = chip(AppTheme.Dark.displayName)
                lightThemeChip = chip(AppTheme.Light.displayName)
                group.addView(systemThemeChip)
                group.addView(darkThemeChip)
                group.addView(lightThemeChip)
                group.setOnCheckedStateChangeListener { _, checkedIds ->
                    if (!applyingState) {
                        when (checkedIds.firstOrNull()) {
                            darkThemeChip.id -> viewModel.saveTheme(AppTheme.Dark)
                            lightThemeChip.id -> viewModel.saveTheme(AppTheme.Light)
                            else -> viewModel.saveTheme(AppTheme.System)
                        }
                    }
                }
                addView(group)
            }
            crashRecoverySwitch = switchRow(
                title = "Crash recovery",
                body = "Autosave workflow progress and show resume warnings after interruptions.",
                onChanged = viewModel::saveCrashRecoveryEnabled,
            )
            keepAwakeSwitch = switchRow(
                title = "Keep screen awake",
                body = "Keep MuseTrace AI awake while screens are open.",
                onChanged = viewModel::saveKeepScreenAwake,
            )
        }

    private fun renderSettings(settings: AppSettings) {
        binding.defaultColorGroup.check(
            when (settings.defaultColorCount) {
                24 -> R.id.chip24
                32 -> R.id.chip32
                else -> R.id.chip16
            },
        )
        simpleModeGroup.check(if (settings.simpleMode) simpleChip.id else advancedChip.id)
        advancedControlsCard.visibility = if (settings.simpleMode) View.GONE else View.VISIBLE
        updateSlider(drawingSpeedSlider, drawingSpeedValue, settings.drawingSpeed, "${String.format("%.2f", settings.drawingSpeed)}x")
        updateSlider(smoothingSlider, smoothingValue, settings.smoothingLevel, "${(settings.smoothingLevel * 100f).roundToInt()}%")
        updateSlider(simplificationSlider, simplificationValue, settings.simplificationLevel, "${(settings.simplificationLevel * 100f).roundToInt()}%")
        updateSlider(minimumStrokeSlider, minimumStrokeValue, settings.minimumStrokeLength, "${settings.minimumStrokeLength.roundToInt()} px")
        updateSlider(gestureDelaySlider, gestureDelayValue, settings.gestureDelayMs.toFloat(), "${settings.gestureDelayMs} ms")
        canvasQualityGroup.check(
            when (settings.canvasQuality) {
                CanvasQuality.Performance -> performanceChip.id
                CanvasQuality.High -> highQualityChip.id
                CanvasQuality.Balanced -> balancedChip.id
            },
        )
        calibrationGroup.check(calibrationChips[settings.selectedCalibrationType]?.id ?: calibrationChips.getValue(CalibrationProfileType.NormalDraw).id)
        updateSlider(overlayOpacitySlider, overlayOpacityValue, settings.overlayOpacity, "${(settings.overlayOpacity * 100f).roundToInt()}%")
        updateSlider(overlaySizeSlider, overlaySizeValue, settings.overlaySize, "${(settings.overlaySize * 100f).roundToInt()}%")
        themeGroup.check(
            when (settings.appTheme) {
                AppTheme.Dark -> darkThemeChip.id
                AppTheme.Light -> lightThemeChip.id
                AppTheme.System -> systemThemeChip.id
            },
        )
        crashRecoverySwitch.isChecked = settings.crashRecoveryEnabled
        keepAwakeSwitch.isChecked = settings.keepScreenAwake
    }

    private fun sectionCard(title: String, body: String, content: LinearLayout.() -> Unit): SfCard {
        val context = requireContext()
        val padding = space(16)
        val card = SfCard(context)
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        column.addView(TextView(context).apply {
            text = title
            setTextColor(context.getColor(R.color.mt_text))
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        column.addView(TextView(context).apply {
            text = body
            setTextColor(context.getColor(R.color.mt_text_secondary))
            textSize = 13f
        }, matchWrap().apply { topMargin = space(4) })
        column.content()
        card.addView(column)
        card.layoutParams = matchWrap().apply { topMargin = space(12) }
        return card
    }

    private fun LinearLayout.settingSlider(
        title: String,
        valueFrom: Float,
        valueTo: Float,
        step: Float,
        formatter: (Float) -> String,
        onChanged: (Float) -> Unit,
    ): Pair<SfSlider, TextView> {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val titleView = TextView(context).apply {
            text = title
            setTextColor(context.getColor(R.color.mt_text))
            textSize = 14f
        }
        val valueView = TextView(context).apply {
            text = formatter(valueFrom)
            setTextColor(context.getColor(R.color.mt_text_secondary))
            textSize = 13f
            gravity = android.view.Gravity.END
        }
        row.addView(titleView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(valueView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(row, matchWrap().apply { topMargin = space(14) })
        val slider = SfSlider(context).apply {
            this.valueFrom = valueFrom
            this.valueTo = valueTo
            stepSize = step
            value = valueFrom
            addOnChangeListener { _, value, fromUser ->
                valueView.text = formatter(value)
                if (fromUser && !applyingState) onChanged(value)
            }
        }
        addView(slider, matchWrap())
        return slider to valueView
    }

    private fun LinearLayout.switchRow(
        title: String,
        body: String,
        onChanged: (Boolean) -> Unit,
    ): MaterialSwitch {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        textColumn.addView(TextView(context).apply {
            text = title
            setTextColor(context.getColor(R.color.mt_text))
            textSize = 14f
        })
        textColumn.addView(TextView(context).apply {
            text = body
            setTextColor(context.getColor(R.color.mt_text_secondary))
            textSize = 12f
        })
        val toggle = MaterialSwitch(context).apply {
            setOnCheckedChangeListener { _, checked ->
                if (!applyingState) onChanged(checked)
            }
        }
        row.addView(textColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(toggle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(row, matchWrap().apply { topMargin = space(12) })
        return toggle
    }

    private fun LinearLayout.addLabel(textValue: String) {
        addView(TextView(requireContext()).apply {
            text = textValue
            setTextColor(requireContext().getColor(R.color.mt_text_secondary))
            textSize = 13f
        }, matchWrap().apply { topMargin = space(14) })
    }

    private fun chipGroup(): ChipGroup =
        ChipGroup(requireContext()).apply {
            isSingleSelection = true
            isSelectionRequired = true
            layoutParams = matchWrap().apply { topMargin = space(8) }
        }

    private fun chip(textValue: String): SfChip =
        SfChip(requireContext()).apply {
            id = View.generateViewId()
            text = textValue
            isCheckable = true
        }

    private fun updateSlider(slider: SfSlider, valueView: TextView, nextValue: Float, label: String) {
        val bounded = nextValue.coerceIn(slider.valueFrom, slider.valueTo)
        val snapped = if (slider.stepSize > 0f) {
            val steps = ((bounded - slider.valueFrom) / slider.stepSize).roundToInt()
            (slider.valueFrom + steps * slider.stepSize).coerceIn(slider.valueFrom, slider.valueTo)
        } else {
            bounded
        }
        slider.value = snapped
        valueView.text = label
    }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun space(dpRes: Int): Int =
        if (dpRes == 4) {
            resources.getDimensionPixelSize(R.dimen.space_4)
        } else if (dpRes == 8) {
            resources.getDimensionPixelSize(R.dimen.space_8)
        } else if (dpRes == 12) {
            resources.getDimensionPixelSize(R.dimen.space_12)
        } else {
            resources.getDimensionPixelSize(R.dimen.space_16)
        }
}
