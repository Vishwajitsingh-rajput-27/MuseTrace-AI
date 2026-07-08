package com.vishwajitrajput.musetraceai.feature.strokes

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
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
import com.vishwajitrajput.musetraceai.databinding.FragmentStrokePreviewBinding
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class StrokePreviewFragment : BaseFragment<FragmentStrokePreviewBinding>(FragmentStrokePreviewBinding::inflate) {
    private val viewModel: StrokePreviewViewModel by viewModels()
    private val projectId: Long by lazy { requireArguments().getLong("projectId") }
    private var currentProject: TraceProject? = null
    private var layerIndex = 0
    private var animator: ValueAnimator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.load(projectId)
        binding.strokeLimitSlider.addOnChangeListener { _, value, _ ->
            renderSelectedLayer(value.toInt())
        }
        binding.previousLayerButton.setOnClickListener {
            cancelAnimation()
            showLayer(layerIndex - 1)
        }
        binding.nextLayerButton.setOnClickListener {
            cancelAnimation()
            showLayer(layerIndex + 1)
        }
        binding.animateButton.setOnClickListener {
            if (animator?.isRunning == true) {
                cancelAnimation()
            } else {
                animateSelectedLayer()
            }
        }
        binding.drawingButton.setOnClickListener {
            findNavController().navigate(R.id.action_strokes_to_drawing, bundleOf("projectId" to projectId))
        }
        collectStarted {
            launch {
                viewModel.project.collect { project ->
                    currentProject = project
                    if (project == null) {
                        binding.strokeSubtitle.text = "Sketch is not available."
                        binding.strokeStats.text = "Create or import an image first, then return to stroke preview."
                        binding.warningCard.visibility = View.GONE
                    } else {
                        binding.strokeSubtitle.text = projectSummary(project)
                        buildLayerList(project)
                        showLayer(layerIndex.coerceIn(0, project.layers.lastIndex.coerceAtLeast(0)))
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        animator?.removeAllListeners()
        animator?.removeAllUpdateListeners()
        animator?.cancel()
        animator = null
        super.onDestroyView()
    }

    private fun showLayer(index: Int, strokeLimit: Int? = null) {
        val project = currentProject ?: return
        if (project.layers.isEmpty()) {
            binding.selectedLayerTitle.text = "No drawable layers"
            binding.strokeStats.text = "This project does not contain color masks with drawable strokes."
            binding.warningCard.visibility = View.GONE
            binding.strokeRasterPreview.setImageDrawable(null)
            return
        }
        layerIndex = index.coerceIn(0, project.layers.lastIndex)
        val layer = project.layers[layerIndex]
        val maxStrokes = layer.strokes.size
        val sliderMax = maxStrokes.coerceAtLeast(1)
        binding.strokeLimitSlider.valueFrom = 0f
        if (binding.strokeLimitSlider.value != 0f) {
            binding.strokeLimitSlider.value = 0f
        }
        binding.strokeLimitSlider.valueTo = sliderMax.toFloat()
        binding.strokeLimitSlider.stepSize = 1f
        val visible = (strokeLimit ?: maxStrokes).coerceIn(0, maxStrokes)
        if (binding.strokeLimitSlider.value != visible.toFloat()) {
            binding.strokeLimitSlider.value = visible.toFloat()
        }
        renderSelectedLayer(visible)
        binding.previousLayerButton.isEnabled = layerIndex > 0
        binding.nextLayerButton.isEnabled = layerIndex < project.layers.lastIndex
        binding.animateButton.isEnabled = layer.strokes.isNotEmpty()
    }

    private fun renderSelectedLayer(strokeLimit: Int) {
        val project = currentProject ?: return
        val layer = project.layers.getOrNull(layerIndex) ?: return
        val visible = strokeLimit.coerceIn(0, layer.strokes.size)
        binding.selectedLayerTitle.text = "${layer.title} (${layerIndex + 1}/${project.layers.size})"
        binding.strokeCanvas.showProject(project, layerIndex, visible)
        binding.strokeLimitLabel.text = "Showing $visible of ${layer.strokes.size} optimized strokes"
        binding.strokeStats.text = layerStats(project, layer)
        setImage(binding.strokeRasterPreview, layer.strokePreviewUri ?: layer.layerBitmapUri)
        bindWarnings(layer)
    }

    private fun animateSelectedLayer() {
        val project = currentProject ?: return
        val layer = project.layers.getOrNull(layerIndex) ?: return
        val maxStrokes = layer.strokes.size
        if (maxStrokes == 0) return
        animator?.cancel()
        binding.animateButton.text = "Stop"
        animator = ValueAnimator.ofInt(0, maxStrokes).apply {
            duration = (maxStrokes * 18L).coerceIn(900L, 12_000L)
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val visible = animation.animatedValue as Int
                if (visible.toFloat() <= binding.strokeLimitSlider.valueTo) {
                    binding.strokeLimitSlider.value = visible.toFloat()
                }
                renderSelectedLayer(visible)
            }
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.animateButton.text = "Animate"
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        binding.animateButton.text = "Animate"
                    }
                },
            )
        }
        animator?.start()
    }

    private fun cancelAnimation() {
        animator?.cancel()
        animator = null
        binding.animateButton.text = "Animate"
    }

    private fun bindWarnings(layer: TraceLayer) {
        val warnings = layer.strokeComplexityWarnings.ifEmpty { layer.qualityWarnings }
        if (warnings.isEmpty()) {
            binding.warningCard.visibility = View.GONE
        } else {
            binding.warningCard.visibility = View.VISIBLE
            binding.complexityWarnings.text = warnings.joinToString("\n") { "- $it" }
        }
    }

    private fun buildLayerList(project: TraceProject) {
        binding.strokeLayerList.removeAllViews()
        project.layers.forEachIndexed { index, layer ->
            binding.strokeLayerList.addView(layerChip(index, layer))
        }
    }

    private fun layerChip(index: Int, layer: TraceLayer): SfCard {
        val context = requireContext()
        val padding = resources.getDimensionPixelSize(R.dimen.space_8)
        return SfCard(context).apply {
            setOnClickListener {
                cancelAnimation()
                showLayer(index)
            }
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(padding, padding, padding, padding)
            }
            row.addView(
                SfColorSwatch(context).apply {
                    setColorHex(layer.colorHex)
                    layoutParams = LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.space_32),
                        resources.getDimensionPixelSize(R.dimen.space_32),
                    ).apply { marginEnd = padding }
                },
            )
            row.addView(
                TextView(context).apply {
                    text = "L${index + 1}\n${layer.strokes.size} strokes\n${layer.gestureCount} gestures"
                    setTextColor(context.getColor(R.color.mt_text))
                    textSize = 12f
                },
                LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.space_32) * 3,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(row)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = resources.getDimensionPixelSize(R.dimen.space_8) }
        }
    }

    private fun projectSummary(project: TraceProject): String {
        val gestures = project.layers.sumOf { it.gestureCount }
        val time = project.layers.sumOf { it.estimatedDrawingTimeMs }
        return "${project.colorCount}-color sketch - ${project.layers.size} layers - ${project.strokeCount} strokes - $gestures gestures - ${formatDuration(time)} estimated"
    }

    private fun layerStats(project: TraceProject, layer: TraceLayer): String {
        val coverage = "${(layer.coveragePercent * 10f).roundToInt() / 10f}%"
        return """
            ${layer.colorName} ${layer.colorHex} - RGB(${layer.red}, ${layer.green}, ${layer.blue})
            Layer ${layerIndex + 1} of ${project.layers.size} - recommended order ${layer.recommendedOrder}
            ${layer.strokes.size} strokes - ${layer.gestureCount} gestures - ${formatDuration(layer.estimatedDrawingTimeMs)}
            Quality ${layer.strokeQualityScore}/100 - difficulty ${layer.difficultyScore}/10 - coverage $coverage
        """.trimIndent()
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
}
