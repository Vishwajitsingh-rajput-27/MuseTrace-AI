package com.vishwajitrajput.musetraceai.feature.calibration

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.core.ui.SfButton
import com.vishwajitrajput.musetraceai.core.ui.SfGhostButton
import com.vishwajitrajput.musetraceai.databinding.FragmentCalibrationBinding
import com.vishwajitrajput.musetraceai.domain.model.AppSettings
import com.vishwajitrajput.musetraceai.domain.model.CalibrationCorner
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfileType
import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class CalibrationFragment : BaseFragment<FragmentCalibrationBinding>(FragmentCalibrationBinding::inflate) {
    private val viewModel: CalibrationViewModel by viewModels()
    private var selectedType = CalibrationProfileType.NormalDraw
    private var selectedCorner = CalibrationCorner.TopLeft
    private var editableProfile = CalibrationProfile()
    private var applyingState = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureSliders()
        binding.xSlider.addOnChangeListener { _, _, _ -> updatePointFromSliders() }
        binding.ySlider.addOnChangeListener { _, _, _ -> updatePointFromSliders() }
        binding.saveButton.setOnClickListener {
            val savedProfile = editableProfile.copy(
                type = selectedType,
                screenWidth = screenWidth(),
                screenHeight = screenHeight(),
                savedAtMillis = System.currentTimeMillis(),
            )
            editableProfile = savedProfile
            viewModel.save(savedProfile)
            bindProfile(savedProfile, saved = true)
        }
        collectStarted {
            launch {
                viewModel.settings.collect { settings ->
                    applySettings(settings)
                }
            }
        }
    }

    private fun applySettings(settings: AppSettings) {
        selectedType = settings.selectedCalibrationType
        editableProfile = (settings.calibrationProfiles[selectedType] ?: CalibrationProfile(type = selectedType))
            .copy(screenWidth = screenWidth(), screenHeight = screenHeight())
        buildProfileButtons(settings)
        buildCornerButtons()
        bindProfile(editableProfile)
    }

    private fun configureSliders() {
        binding.xSlider.valueFrom = 0f
        binding.ySlider.valueFrom = 0f
        binding.xSlider.valueTo = screenWidth().coerceAtLeast(1).toFloat()
        binding.ySlider.valueTo = screenHeight().coerceAtLeast(1).toFloat()
        binding.xSlider.stepSize = 1f
        binding.ySlider.stepSize = 1f
    }

    private fun buildProfileButtons(settings: AppSettings) {
        binding.profileContainer.removeAllViews()
        CalibrationProfileType.entries.forEach { type ->
            val profile = settings.calibrationProfiles[type] ?: CalibrationProfile(type = type)
            val button = if (type == selectedType) SfButton(requireContext()) else SfGhostButton(requireContext())
            button.text = if (profile.isUsable()) type.displayName else "${type.displayName} *"
            button.setOnClickListener {
                selectedType = type
                selectedCorner = CalibrationCorner.TopLeft
                editableProfile = profile.copy(screenWidth = screenWidth(), screenHeight = screenHeight())
                viewModel.selectProfile(type)
                buildProfileButtons(settings.copy(selectedCalibrationType = type))
                buildCornerButtons()
                bindProfile(editableProfile)
            }
            button.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = resources.getDimensionPixelSize(R.dimen.space_8) }
            binding.profileContainer.addView(button)
        }
    }

    private fun buildCornerButtons() {
        binding.cornerContainer.removeAllViews()
        CalibrationCorner.entries.chunked(2).forEach { rowCorners ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.space_8) }
            }
            rowCorners.forEach { corner ->
                val button = if (corner == selectedCorner) SfButton(requireContext()) else SfGhostButton(requireContext())
                button.text = corner.displayName
                button.setOnClickListener {
                    selectedCorner = corner
                    buildCornerButtons()
                    bindProfile(editableProfile)
                }
                row.addView(
                    button,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = resources.getDimensionPixelSize(R.dimen.space_8)
                    },
                )
            }
            binding.cornerContainer.addView(row)
        }
    }

    private fun bindProfile(profile: CalibrationProfile, saved: Boolean = false) {
        applyingState = true
        val point = profile.point(selectedCorner)
        val displayX = point.x.coerceIn(0f, binding.xSlider.valueTo)
        val displayY = point.y.coerceIn(0f, binding.ySlider.valueTo)
        binding.xSlider.value = displayX
        binding.ySlider.value = displayY
        applyingState = false
        binding.selectedPointLabel.text = "${profile.type.displayName}: ${selectedCorner.displayName}"
        binding.xLabel.text = "X: ${displayX.roundToInt()} px"
        binding.yLabel.text = "Y: ${displayY.roundToInt()} px"
        binding.calibrationPreview.bind(profile, selectedCorner)
        binding.calibrationSummary.text = if (profile.isUsable()) {
            "${profile.type.displayName} calibration ${if (saved) "saved" else "ready"}"
        } else {
            "${profile.type.displayName} calibration missing"
        }
        binding.missingCalibrationCard.visibility = if (profile.isUsable()) View.GONE else View.VISIBLE
        binding.missingCalibrationText.text =
            "Set top-left, top-right, bottom-left, and bottom-right after Instagram Draw is already open. Drawing is blocked until this profile is saved."
        binding.metricsText.text = metricsText(profile)
    }

    private fun updatePointFromSliders() {
        if (applyingState) return
        val point = TracePoint(binding.xSlider.value, binding.ySlider.value)
        editableProfile = editableProfile
            .copy(type = selectedType, screenWidth = screenWidth(), screenHeight = screenHeight())
            .withPoint(selectedCorner, point, screenWidth(), screenHeight())
        bindProfile(editableProfile)
    }

    private fun metricsText(profile: CalibrationProfile): String {
        val metrics = profile.metrics()
        val zones = metrics.overlaySafeZones.joinToString { it.name }.ifBlank { "none large enough" }
        return """
            Bounds: ${metrics.bounds.left.roundToInt()}, ${metrics.bounds.top.roundToInt()} - ${metrics.bounds.width.roundToInt()} x ${metrics.bounds.height.roundToInt()} px
            Scale: ${metrics.scaleX.roundToInt()} x ${metrics.scaleY.roundToInt()} px - offset ${metrics.offsetX.roundToInt()}, ${metrics.offsetY.roundToInt()}
            Rotation: ${metrics.rotationDegrees.roundToInt()} degrees - aspect ${"%.2f".format(metrics.aspectRatio)}
            Overlay-safe zones: $zones
        """.trimIndent()
    }

    private fun screenWidth(): Int = resources.displayMetrics.widthPixels.coerceAtLeast(1)

    private fun screenHeight(): Int = resources.displayMetrics.heightPixels.coerceAtLeast(1)
}
