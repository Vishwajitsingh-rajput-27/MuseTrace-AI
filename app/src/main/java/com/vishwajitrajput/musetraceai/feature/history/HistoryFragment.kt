package com.vishwajitrajput.musetraceai.feature.history

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.core.ui.SfButton
import com.vishwajitrajput.musetraceai.core.ui.SfCard
import com.vishwajitrajput.musetraceai.core.ui.SfDangerButton
import com.vishwajitrajput.musetraceai.core.ui.SfEmptyState
import com.vishwajitrajput.musetraceai.core.ui.SfGhostButton
import com.vishwajitrajput.musetraceai.databinding.FragmentHistoryBinding
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@AndroidEntryPoint
class HistoryFragment : BaseFragment<FragmentHistoryBinding>(FragmentHistoryBinding::inflate) {
    private val viewModel: HistoryViewModel by viewModels()
    private var statusText: TextView? = null

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restore(it.toString()) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectStarted {
            launch { viewModel.projects.collect(::renderProjects) }
            launch {
                viewModel.status.collect { status ->
                    statusText?.text = status
                }
            }
        }
    }

    private fun renderProjects(projects: List<TraceProject>) {
        binding.historyContainer.removeAllViews()
        binding.historyContainer.addView(actionsCard())
        if (projects.isEmpty()) {
            binding.historyContainer.addView(emptyState())
        } else {
            projects.forEach { project -> binding.historyContainer.addView(projectCard(project)) }
        }
    }

    private fun projectCard(project: TraceProject): SfCard {
        val context = requireContext()
        val padding = resources.getDimensionPixelSize(R.dimen.space_16)
        return SfCard(context).apply {
            val body = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding)
            }
            body.addView(TextView(context).apply {
                text = project.title
                setTextColor(context.getColor(R.color.mt_text))
                textSize = 17f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            body.addView(TextView(context).apply {
                text = "${project.colorCount} colors - ${project.layers.size} layers - ${project.strokeCount} strokes\n${DateFormat.getDateTimeInstance().format(Date(project.createdAtMillis))}"
                setTextColor(context.getColor(R.color.mt_text_secondary))
                textSize = 14f
            })
            body.addView(SfButton(context).apply {
                text = "Open"
                setOnClickListener {
                    findNavController().navigate(
                        R.id.action_history_to_sketch,
                        bundleOf("projectId" to project.id),
                    )
                }
            })
            body.addView(SfGhostButton(context).apply {
                text = "Export JSON"
                setOnClickListener { viewModel.exportJson(project.id) }
            })
            body.addView(SfGhostButton(context).apply {
                text = "Backup project file"
                setOnClickListener { viewModel.backup(project.id) }
            })
            body.addView(SfDangerButton(context).apply {
                text = "Delete"
                setOnClickListener { viewModel.delete(project.id) }
            })
            addView(body)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.space_12) }
        }
    }

    private fun actionsCard(): SfCard {
        val context = requireContext()
        val padding = resources.getDimensionPixelSize(R.dimen.space_16)
        return SfCard(context).apply {
            val body = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding)
            }
            body.addView(TextView(context).apply {
                text = "Project persistence"
                setTextColor(context.getColor(R.color.mt_text))
                textSize = 17f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            val statusView = TextView(context).apply {
                text = viewModel.status.value
                setTextColor(context.getColor(R.color.mt_text_secondary))
                textSize = 13f
            }
            statusText = statusView
            body.addView(statusView)
            body.addView(SfButton(context).apply {
                text = "Import or restore project file"
                setOnClickListener {
                    restoreLauncher.launch(
                        arrayOf(
                            "application/json",
                            "application/octet-stream",
                            "application/zip",
                            "*/*",
                        ),
                    )
                }
            })
            addView(body)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.space_12) }
        }
    }

    private fun emptyState(): SfEmptyState =
        SfEmptyState(requireContext()).apply {
            bind("No sketches saved yet.", "Create one from Home to see it here.")
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.sf_preview_height),
            )
        }
}
