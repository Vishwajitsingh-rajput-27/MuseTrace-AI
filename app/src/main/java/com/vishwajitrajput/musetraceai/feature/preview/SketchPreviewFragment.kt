package com.vishwajitrajput.musetraceai.feature.preview

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.databinding.FragmentSketchPreviewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SketchPreviewFragment : BaseFragment<FragmentSketchPreviewBinding>(FragmentSketchPreviewBinding::inflate) {
    private val viewModel: SketchPreviewViewModel by viewModels()
    private val projectId: Long by lazy { requireArguments().getLong("projectId") }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.load(projectId)
        setupTabs()
        binding.layersButton.setOnClickListener {
            findNavController().navigate(R.id.action_sketch_to_layers, bundleOf("projectId" to projectId))
        }
        binding.strokesButton.setOnClickListener {
            findNavController().navigate(R.id.action_sketch_to_strokes, bundleOf("projectId" to projectId))
        }
        binding.calibrationButton.setOnClickListener {
            findNavController().navigate(R.id.action_sketch_to_calibration)
        }
        binding.drawingButton.setOnClickListener {
            findNavController().navigate(R.id.action_sketch_to_drawing, bundleOf("projectId" to projectId))
        }
        collectStarted {
            launch {
                viewModel.project.collect { project ->
                    if (project == null) {
                        binding.projectStats.text = "Sketch is not available."
                    } else {
                        binding.sketchCanvas.showProject(project)
                        binding.projectStats.text =
                            "${project.title}\n${project.colorCount} colors - ${project.layers.size} layers - ${project.strokeCount} strokes"
                    }
                }
            }
        }
    }

    private fun setupTabs() {
        binding.previewTabs.addTab(binding.previewTabs.newTab().setText("Sketch"))
        binding.previewTabs.addTab(binding.previewTabs.newTab().setText("Layers"))
        binding.previewTabs.addTab(binding.previewTabs.newTab().setText("Strokes"))
        binding.previewTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    1 -> findNavController().navigate(R.id.action_sketch_to_layers, bundleOf("projectId" to projectId))
                    2 -> findNavController().navigate(R.id.action_sketch_to_strokes, bundleOf("projectId" to projectId))
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }
}
