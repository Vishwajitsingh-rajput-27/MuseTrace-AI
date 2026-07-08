package com.vishwajitrajput.musetraceai.feature.layers

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
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.core.ui.SfCard
import com.vishwajitrajput.musetraceai.core.ui.SfColorSwatch
import com.vishwajitrajput.musetraceai.databinding.FragmentLayerPreviewBinding
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class LayerPreviewFragment : BaseFragment<FragmentLayerPreviewBinding>(FragmentLayerPreviewBinding::inflate) {
    private val viewModel: LayerPreviewViewModel by viewModels()
    private val projectId: Long by lazy { requireArguments().getLong("projectId") }
    private var currentProject: TraceProject? = null
    private var layerIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.load(projectId)
        binding.previousLayerButton.setOnClickListener { showLayer(layerIndex - 1) }
        binding.nextLayerButton.setOnClickListener { showLayer(layerIndex + 1) }
        binding.moveLayerUpButton.setOnClickListener {
            val newIndex = (layerIndex - 1).coerceAtLeast(0)
            viewModel.moveLayer(layerIndex, -1)
            layerIndex = newIndex
        }
        binding.moveLayerDownButton.setOnClickListener {
            val project = currentProject ?: return@setOnClickListener
            val newIndex = (layerIndex + 1).coerceAtMost(project.layers.lastIndex)
            viewModel.moveLayer(layerIndex, 1)
            layerIndex = newIndex
        }
        binding.drawingButton.setOnClickListener {
            findNavController().navigate(R.id.action_layers_to_drawing, bundleOf("projectId" to projectId))
        }
        collectStarted {
            launch {
                viewModel.project.collect { project ->
                    currentProject = project
                    if (project != null) {
                        binding.finalPreview.setImageURI(Uri.parse(project.previewPath))
                        binding.projectSummary.text =
                            "${project.colorCount}-color target - ${project.layers.size} active layers - ${project.strokeCount} strokes"
                        buildPalette(project)
                        buildLayerList(project)
                        showLayer(layerIndex.coerceIn(0, project.layers.lastIndex.coerceAtLeast(0)))
                    } else {
                        binding.layerInfo.text = "Sketch is not available."
                    }
                }
            }
            launch {
                viewModel.status.collect { status ->
                    binding.reorderStatus.text = status
                }
            }
        }
    }

    private fun showLayer(index: Int) {
        val project = currentProject ?: return
        if (project.layers.isEmpty()) return
        layerIndex = index.coerceIn(0, project.layers.lastIndex)
        val layer = project.layers[layerIndex]
        binding.layerCanvas.showProject(project, layerIndex)
        binding.layerInfo.text = layerDetails(layer, project)
        binding.layerWarnings.text = if (layer.qualityWarnings.isEmpty()) {
            "Quality: clean layer. Recommended order ${layer.recommendedOrder}."
        } else {
            "Warnings:\n${layer.qualityWarnings.joinToString("\n") { "- $it" }}"
        }
        setImage(binding.layerBitmapPreview, layer.layerBitmapUri)
        binding.previousLayerButton.isEnabled = layerIndex > 0
        binding.nextLayerButton.isEnabled = layerIndex < project.layers.lastIndex
        binding.moveLayerUpButton.isEnabled = layerIndex > 0
        binding.moveLayerDownButton.isEnabled = layerIndex < project.layers.lastIndex
    }

    private fun layerDetails(layer: TraceLayer, project: TraceProject): String {
        val rgb = "RGB(${layer.red}, ${layer.green}, ${layer.blue})"
        val time = formatDuration(layer.estimatedDrawingTimeMs)
        return """
            ${layer.title}
            ${layer.colorName} - ${layer.colorHex} - $rgb
            Coverage ${formatPercent(layer.coveragePercent)} - ${layer.pixelCount} pixels
            Difficulty ${layer.difficultyScore}/10 - estimated $time
            Recommended order ${layer.recommendedOrder} of ${project.layers.size}
            ${layer.strokes.size} strokes. Manually select this color before drawing.
        """.trimIndent()
    }

    private fun buildPalette(project: TraceProject) {
        binding.paletteContainer.removeAllViews()
        project.layers.forEachIndexed { index, layer ->
            binding.paletteContainer.addView(paletteChip(index, layer))
        }
    }

    private fun paletteChip(index: Int, layer: TraceLayer): SfCard {
        val context = requireContext()
        val padding = resources.getDimensionPixelSize(R.dimen.space_8)
        return SfCard(context).apply {
            setOnClickListener { showLayer(index) }
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding)
            }
            column.addView(SfColorSwatch(context).apply {
                setColorHex(layer.colorHex)
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.space_32),
                    resources.getDimensionPixelSize(R.dimen.space_32),
                )
            })
            column.addView(TextView(context).apply {
                text = layer.colorHex
                setTextColor(context.getColor(R.color.mt_text))
                textSize = 11f
            })
            addView(column)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = resources.getDimensionPixelSize(R.dimen.space_8) }
        }
    }

    private fun buildLayerList(project: TraceProject) {
        binding.layersContainer.removeAllViews()
        project.layers.forEachIndexed { index, layer ->
            binding.layersContainer.addView(layerCard(index, layer))
        }
    }

    private fun layerCard(index: Int, layer: TraceLayer): SfCard {
        val context = requireContext()
        val padding = resources.getDimensionPixelSize(R.dimen.space_12)
        return SfCard(context).apply {
            setOnClickListener { showLayer(index) }
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(padding, padding, padding, padding)
            }
            row.addView(SfColorSwatch(context).apply {
                setColorHex(layer.colorHex)
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.space_32),
                    resources.getDimensionPixelSize(R.dimen.space_32),
                ).apply { marginEnd = padding }
            })
            row.addView(TextView(context).apply {
                text = "${layer.title}\n${layer.colorHex} - ${layer.colorName} - ${formatDuration(layer.estimatedDrawingTimeMs)} - difficulty ${layer.difficultyScore}/10"
                setTextColor(context.getColor(R.color.mt_text))
                textSize = 14f
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(row)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.space_8) }
        }
    }

    private fun setImage(imageView: ImageView, uri: String?) {
        if (uri.isNullOrBlank()) {
            imageView.setImageDrawable(null)
        } else {
            imageView.setImageURI(Uri.parse(uri))
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(1L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    private fun formatPercent(value: Float): String =
        "${(value * 10f).roundToInt() / 10f}%"
}
