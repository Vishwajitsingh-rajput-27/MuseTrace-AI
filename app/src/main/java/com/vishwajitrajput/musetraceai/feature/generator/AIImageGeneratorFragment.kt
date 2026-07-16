package com.vishwajitrajput.musetraceai.feature.generator

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.core.ui.SfButton
import com.vishwajitrajput.musetraceai.core.ui.SfCard
import com.vishwajitrajput.musetraceai.core.ui.SfGhostButton
import com.vishwajitrajput.musetraceai.databinding.FragmentGeneratorBinding
import com.vishwajitrajput.musetraceai.domain.model.GeneratedImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@AndroidEntryPoint
class AIImageGeneratorFragment : BaseFragment<FragmentGeneratorBinding>(FragmentGeneratorBinding::inflate) {
    private val viewModel: AIImageGeneratorViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loadingCard.setMessage("Gemini is creating a trace-ready image...")
        binding.generateButton.setOnClickListener { generateFromInputs() }
        binding.regenerateButton.setOnClickListener { viewModel.regenerate() }
        binding.cancelButton.setOnClickListener { viewModel.cancelGeneration() }
        binding.saveButton.setOnClickListener { viewModel.saveCurrentImage() }
        binding.useForDrawingButton.setOnClickListener { viewModel.useForDrawing() }

        collectStarted {
            launch {
                viewModel.state.collect { state ->
                    renderState(state)
                    state.editorUri?.let { uri ->
                        viewModel.consumeEditorNavigation()
                        findNavController().navigate(
                            R.id.action_generator_to_editor,
                            bundleOf("sourceUri" to uri),
                        )
                    }
                }
            }
        }
    }

    private fun generateFromInputs() {
        viewModel.generate(
            prompt = binding.promptInput.text?.toString().orEmpty(),
            negativePrompt = binding.negativePromptInput.text?.toString().orEmpty(),
            styleName = selectedChipText(binding.styleGroup.checkedChipId, "Semi-realistic sketch"),
            aspectRatioName = selectedChipText(binding.aspectRatioGroup.checkedChipId, "1:1"),
        )
    }

    private fun renderState(state: GeneratorUiState) {
        binding.loadingCard.visibility = if (state.loading) View.VISIBLE else View.GONE
        binding.errorCard.visibility = if (state.error == null) View.GONE else View.VISIBLE
        binding.errorText.text = state.error.orEmpty()
        binding.statusText.text = state.status
        binding.generateButton.isEnabled = !state.loading
        binding.regenerateButton.isEnabled = !state.loading
        binding.cancelButton.isEnabled = state.loading
        binding.saveButton.isEnabled = state.currentImage != null && !state.loading
        binding.useForDrawingButton.isEnabled = state.currentImage != null && !state.loading
        state.currentImage?.let { binding.generatedPreview.setImageURI(Uri.parse(it.imageUri)) }
        renderHistory(state.history)
    }

    private fun renderHistory(history: List<GeneratedImage>) {
        binding.generationHistoryContainer.removeAllViews()
        if (history.isEmpty()) {
            binding.generationHistoryContainer.addView(TextView(requireContext()).apply {
                text = "No generations yet."
                setTextColor(requireContext().getColor(R.color.mt_text_secondary))
                textSize = 14f
            })
            return
        }
        history.forEach { image ->
            binding.generationHistoryContainer.addView(historyCard(image))
        }
    }

    private fun historyCard(image: GeneratedImage): SfCard {
        val context = requireContext()
        val padding = resources.getDimensionPixelSize(R.dimen.space_12)
        return SfCard(context).apply {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(padding, padding, padding, padding)
            }
            row.addView(ImageView(context).apply {
                setImageURI(Uri.parse(image.imageUri))
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = context.getDrawable(R.drawable.bg_preview)
                layoutParams = LinearLayout.LayoutParams(96, 96).apply { marginEnd = padding }
            })
            row.addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(context).apply {
                    text = image.prompt
                    setTextColor(context.getColor(R.color.mt_text))
                    textSize = 15f
                    maxLines = 2
                })
                addView(TextView(context).apply {
                    text = "${image.styleName} - ${image.aspectRatioName}\n${DateFormat.getDateTimeInstance().format(Date(image.createdAtMillis))}"
                    setTextColor(context.getColor(R.color.mt_text_secondary))
                    textSize = 13f
                })
                addView(SfGhostButton(context).apply {
                    text = "Load"
                    setOnClickListener { viewModel.selectHistory(image) }
                })
                addView(SfButton(context).apply {
                    text = "Use for drawing"
                    setOnClickListener {
                        viewModel.selectHistory(image)
                        viewModel.useForDrawing()
                    }
                })
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(row)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.space_12) }
        }
    }

    private fun selectedChipText(checkedChipId: Int, fallback: String): String {
        val chip = binding.root.findViewById<Chip>(checkedChipId)
        return chip?.text?.toString() ?: fallback
    }
}
