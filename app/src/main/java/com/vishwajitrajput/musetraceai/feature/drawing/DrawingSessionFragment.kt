package com.vishwajitrajput.musetraceai.feature.drawing

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.core.common.LegalCopy
import com.vishwajitrajput.musetraceai.core.permissions.PermissionManager
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.core.ui.SfBottomSheet
import com.vishwajitrajput.musetraceai.core.ui.SfSheetAction
import com.vishwajitrajput.musetraceai.core.ui.SfSheetActionStyle
import com.vishwajitrajput.musetraceai.databinding.FragmentDrawingSessionBinding
import com.vishwajitrajput.musetraceai.service.accessibility.AccessibilityBridge
import com.vishwajitrajput.musetraceai.service.overlay.OverlayService
import com.vishwajitrajput.musetraceai.service.session.DrawingSessionForegroundService
import com.vishwajitrajput.musetraceai.service.session.DrawingSessionStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DrawingSessionFragment : BaseFragment<FragmentDrawingSessionBinding>(FragmentDrawingSessionBinding::inflate) {
    private val viewModel: DrawingSessionViewModel by viewModels()
    private val projectId: Long by lazy { requireArguments().getLong("projectId") }

    @Inject
    lateinit var permissionManager: PermissionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DrawingSessionStore.initialize(requireContext())
        viewModel.load(projectId)
        binding.safetyText.text = LegalCopy.DRAWING_WARNING
        binding.previousLayerButton.setOnClickListener { viewModel.previousLayer() }
        binding.nextLayerButton.setOnClickListener { viewModel.nextLayer() }
        binding.startForegroundButton.setOnClickListener { showStartSessionChecklist() }
        binding.openOverlayButton.setOnClickListener { showStartSessionChecklist() }
        binding.confirmDrawButton.setOnClickListener {
            if (viewModel.session.value.resumeDecisionRequired) {
                showResumeDecisionSheet()
            } else {
                showStartSessionChecklist()
            }
        }
        collectStarted {
            launch {
                viewModel.session.collect { session ->
                    viewModel.autoSaveSessionSnapshot(session)
                    session.project?.let { project ->
                        binding.sessionCanvas.showProject(project, session.layerIndex)
                        val layer = project.layers.getOrNull(session.layerIndex)
                        binding.sessionStatus.text =
                            "${session.status}\nCurrent color: ${layer?.colorHex.orEmpty()}"
                    } ?: run {
                        binding.sessionStatus.text = session.status
                    }
                    binding.previousLayerButton.isEnabled = session.canReorderLayers
                    binding.nextLayerButton.isEnabled = session.canReorderLayers
                    binding.confirmDrawButton.text = if (session.resumeDecisionRequired) {
                        "Resolve resume warning"
                    } else {
                        "Start Instagram Drawing Session"
                    }
                }
            }
        }
    }

    private fun showStartSessionChecklist() {
        val state = viewModel.session.value
        if (state.resumeDecisionRequired) {
            showResumeDecisionSheet()
            return
        }
        SfBottomSheet(requireContext()).apply {
            setActionsContent(
                title = "Start Instagram Drawing Session",
                body = START_SESSION_CHECKLIST,
                actions = listOf(
                    SfSheetAction("Start Overlay Session", SfSheetActionStyle.Primary) {
                        startOverlaySessionFromChecklist()
                    },
                    SfSheetAction("Open Overlay Permission Settings") {
                        startActivity(permissionManager.overlaySettingsIntent())
                    },
                    SfSheetAction("Open Accessibility Settings") {
                        startActivity(permissionManager.accessibilitySettingsIntent())
                    },
                    SfSheetAction("Open Calibration") {
                        findNavController().navigate(R.id.calibrationFragment)
                    },
                    SfSheetAction("Cancel") {
                        DrawingSessionStore.markStopped("Start cancelled. No drawing gestures were started.")
                    },
                ),
            )
            show()
        }
    }

    private fun startOverlaySessionFromChecklist() {
        val state = viewModel.session.value
        when {
            state.project == null -> {
                DrawingSessionStore.markStopped("Open a sketch before starting a drawing session.")
            }
            state.resumeDecisionRequired -> {
                showResumeDecisionSheet()
            }
            !state.calibration.isUsable() -> {
                DrawingSessionStore.markStopped("Calibration is missing. Save the final Instagram Draw area before starting.")
                findNavController().navigate(R.id.calibrationFragment)
            }
            !permissionManager.isAccessibilityEnabled() -> {
                startActivity(permissionManager.accessibilitySettingsIntent())
            }
            !permissionManager.canDrawOverlay() -> {
                startActivity(permissionManager.overlaySettingsIntent())
            }
            else -> {
                DrawingSessionStore.prepareOverlaySession()
                ContextCompat.startForegroundService(
                    requireContext(),
                    DrawingSessionForegroundService.startIntent(requireContext()),
                )
                ContextCompat.startForegroundService(
                    requireContext(),
                    OverlayService.startIntent(requireContext()),
                )
            }
        }
    }

    private fun showResumeDecisionSheet() {
        SfBottomSheet(requireContext()).apply {
            setActionsContent(
                title = "Resume warning",
                body = "${DrawingSessionStore.INTERRUPTED_CANVAS_WARNING}\n\n${DrawingSessionStore.RESUME_EXPLANATION}",
                actions = listOf(
                    SfSheetAction("Restart from Layer 1", SfSheetActionStyle.Primary) {
                        AccessibilityBridge.cancelDrawing()
                        DrawingSessionStore.restartFromLayer1()
                        showStartSessionChecklist()
                    },
                    SfSheetAction("Continue from selected layer") {
                        DrawingSessionStore.confirmCanvasResume()
                        showStartSessionChecklist()
                    },
                    SfSheetAction("Recalibrate") {
                        AccessibilityBridge.cancelDrawing()
                        DrawingSessionStore.requestRecalibration()
                        findNavController().navigate(R.id.calibrationFragment)
                    },
                    SfSheetAction("Cancel session", SfSheetActionStyle.Danger) {
                        AccessibilityBridge.cancelDrawing()
                        DrawingSessionStore.cancelSession()
                    },
                ),
            )
            show()
        }
    }

    companion object {
        private val START_SESSION_CHECKLIST = """
            1. Open Instagram manually.
            2. Open the target chat manually.
            3. Open Draw mode manually.
            4. Use Add Space manually if needed.
            5. Do not press Back.
            6. Do not press Home.
            7. Do not switch apps.
            8. Do not lock phone.
            9. Keep Instagram Draw open until finished.
            10. Floating overlay will guide colors and Continue buttons.
        """.trimIndent()
    }
}
