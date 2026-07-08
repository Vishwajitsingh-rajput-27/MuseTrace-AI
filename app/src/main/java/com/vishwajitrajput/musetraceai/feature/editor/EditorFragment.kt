package com.vishwajitrajput.musetraceai.feature.editor

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.core.storage.ImageFileStore
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.databinding.FragmentEditorBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class EditorFragment : BaseFragment<FragmentEditorBinding>(FragmentEditorBinding::inflate) {
    private val viewModel: EditorViewModel by viewModels()
    private var pendingCameraUri: Uri? = null
    private var syncingSlider = false

    @Inject
    lateinit var imageFileStore: ImageFileStore

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.loadSource(it.toString()) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let { viewModel.loadSource(it.toString()) }
        } else {
            viewModel.showStatus("Camera capture cancelled.")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            viewModel.showStatus("Camera permission is required for capture.")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindActions()
        bindToolCarousel()
        val sourceUri = arguments?.getString("sourceUri")
        if (viewModel.state.value.sourceUri == null && !sourceUri.isNullOrBlank()) {
            viewModel.loadSource(sourceUri)
        }
        collectStarted {
            launch {
                viewModel.state.collect { state ->
                    renderState(state)
                    state.projectId?.let { id ->
                        viewModel.consumeNavigation()
                        findNavController().navigate(
                            R.id.action_editor_to_sketch,
                            bundleOf("projectId" to id),
                        )
                    }
                }
            }
        }
    }

    private fun bindActions() {
        binding.progressCard.setMessage("Processing image...")
        binding.importGalleryButton.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.importCameraButton.setOnClickListener { requestCameraCapture() }
        binding.resetButton.setOnClickListener { viewModel.resetEdits() }
        binding.applyButton.setOnClickListener { viewModel.applyEdits() }
        binding.cancelButton.setOnClickListener { viewModel.cancelWork() }
        binding.exportButton.setOnClickListener { viewModel.exportProcessedBitmap() }
        binding.createSketchButton.setOnClickListener { viewModel.createSketch(selectedColorCount()) }
        binding.toolSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !syncingSlider) {
                viewModel.updateActiveTool(value)
            }
        }
        binding.colorCountGroup.setOnCheckedStateChangeListener { _, _ ->
            val colors = selectedColorCount()
            viewModel.showStatus("Sketch output will use $colors colors.")
        }
    }

    private fun bindToolCarousel() {
        binding.toolCropButton.setOnClickListener { viewModel.selectTool(EditorTool.CROP) }
        binding.toolResizeButton.setOnClickListener { viewModel.selectTool(EditorTool.RESIZE) }
        binding.toolBrightnessButton.setOnClickListener { viewModel.selectTool(EditorTool.BRIGHTNESS) }
        binding.toolContrastButton.setOnClickListener { viewModel.selectTool(EditorTool.CONTRAST) }
        binding.toolSaturationButton.setOnClickListener { viewModel.selectTool(EditorTool.SATURATION) }
        binding.toolSharpnessButton.setOnClickListener { viewModel.selectTool(EditorTool.SHARPNESS) }
        binding.toolAutoButton.setOnClickListener { viewModel.selectTool(EditorTool.AUTO_ENHANCE) }
        binding.toolNoiseButton.setOnClickListener { viewModel.selectTool(EditorTool.NOISE_REDUCTION) }
        binding.toolEdgeButton.setOnClickListener { viewModel.selectTool(EditorTool.EDGE_ENHANCE) }
        binding.toolBackgroundButton.setOnClickListener { viewModel.selectTool(EditorTool.BACKGROUND_SIMPLIFICATION) }
        binding.toolPortraitButton.setOnClickListener { viewModel.selectTool(EditorTool.PORTRAIT_ENHANCEMENT) }
        binding.toolFaceSafeButton.setOnClickListener { viewModel.selectTool(EditorTool.FACE_SAFE) }
        binding.rotateLeftButton.setOnClickListener { viewModel.rotateLeft() }
        binding.rotateRightButton.setOnClickListener { viewModel.rotateRight() }
        binding.flipHorizontalButton.setOnClickListener { viewModel.flipHorizontal() }
        binding.flipVerticalButton.setOnClickListener { viewModel.flipVertical() }
    }

    private fun renderState(state: EditorUiState) {
        binding.progressCard.visibility = if (state.loading) View.VISIBLE else View.GONE
        if (state.loadingMessage.isNotBlank()) binding.progressCard.setMessage(state.loadingMessage)
        binding.statusText.text = state.status
        binding.sourceLabel.text = if (state.sourceUri == null) {
            "Import an image and prepare it for layered drawing."
        } else {
            "Editing ${state.width} x ${state.height} bitmap for ${selectedColorCount()}-color layered conversion."
        }
        binding.beforeLabel.text = "Before preview"
        binding.afterLabel.text = if (state.afterPreviewUri == null) {
            "After preview - apply edits to update"
        } else {
            "After preview - processed bitmap"
        }
        setImage(binding.beforePreview, state.beforePreviewUri)
        setImage(binding.afterPreview, state.afterPreviewUri ?: state.beforePreviewUri)
        renderSlider(state)
        val hasImage = state.sourceUri != null
        binding.applyButton.isEnabled = hasImage && !state.loading
        binding.resetButton.isEnabled = hasImage && !state.loading
        binding.exportButton.isEnabled = hasImage && !state.loading
        binding.createSketchButton.isEnabled = hasImage && !state.loading
        binding.cancelButton.isEnabled = state.loading
        binding.importGalleryButton.isEnabled = !state.loading
        binding.importCameraButton.isEnabled = !state.loading
    }

    private fun renderSlider(state: EditorUiState) {
        val tool = state.activeTool
        val value = normalizedSliderValue(tool, viewModel.toolValue(tool, state.settings))
        syncingSlider = true
        binding.sliderLabel.text = tool.label
        binding.sliderValue.text = tool.formatValue(value)
        binding.toolSlider.valueFrom = tool.min
        binding.toolSlider.valueTo = tool.max
        binding.toolSlider.stepSize = tool.step
        binding.toolSlider.value = value
        syncingSlider = false
    }

    private fun normalizedSliderValue(tool: EditorTool, rawValue: Float): Float {
        val clipped = rawValue.coerceIn(tool.min, tool.max)
        val steps = ((clipped - tool.min) / tool.step).roundToInt()
        return (tool.min + steps * tool.step).coerceIn(tool.min, tool.max)
    }

    private fun setImage(imageView: android.widget.ImageView, uri: String?) {
        if (uri.isNullOrBlank()) {
            imageView.setImageDrawable(null)
        } else {
            imageView.setImageURI(Uri.parse(uri))
        }
    }

    private fun requestCameraCapture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        pendingCameraUri = imageFileStore.createCameraImageUri()
        pendingCameraUri?.let { cameraLauncher.launch(it) }
    }

    private fun selectedColorCount(): Int = when (binding.colorCountGroup.checkedChipId) {
        R.id.chip24 -> 24
        R.id.chip32 -> 32
        else -> 16
    }
}
