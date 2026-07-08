package com.vishwajitrajput.musetraceai.feature.overlay

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.vishwajitrajput.musetraceai.core.permissions.PermissionManager
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.core.ui.SfBottomSheet
import com.vishwajitrajput.musetraceai.core.ui.SfSheetAction
import com.vishwajitrajput.musetraceai.core.ui.SfSheetActionStyle
import com.vishwajitrajput.musetraceai.databinding.FragmentOverlayControllerBinding
import com.vishwajitrajput.musetraceai.service.overlay.OverlayService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FloatingOverlayControllerFragment : BaseFragment<FragmentOverlayControllerBinding>(FragmentOverlayControllerBinding::inflate) {
    @Inject
    lateinit var permissionManager: PermissionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.requestOverlayButton.setOnClickListener {
            startActivity(permissionManager.overlaySettingsIntent())
        }
        binding.startOverlayButton.setOnClickListener {
            if (!permissionManager.canDrawOverlay()) {
                startActivity(permissionManager.overlaySettingsIntent())
            } else {
                showStartFromDrawingSessionSheet()
            }
        }
        binding.stopOverlayButton.setOnClickListener {
            requireContext().stopService(Intent(requireContext(), OverlayService::class.java))
            updateStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val granted = permissionManager.canDrawOverlay()
        binding.overlayStatus.text = if (granted) {
            "Overlay permission enabled"
        } else {
            "Overlay permission required"
        }
        binding.requestOverlayButton.text = if (granted) {
            "Open Android overlay settings"
        } else {
            "Grant overlay permission"
        }
        binding.startOverlayButton.text = if (granted) {
            "Use drawing session checklist"
        } else {
            "Grant overlay permission first"
        }
        binding.startOverlayButton.isEnabled = true
    }

    private fun showStartFromDrawingSessionSheet() {
        SfBottomSheet(requireContext()).apply {
            setActionsContent(
                title = "Start from drawing session",
                body = "Open a prepared sketch and tap Start Instagram Drawing Session. The checklist saves the active project, prepares Color 1, starts the foreground service, and shows the floating overlay only after calibration, AccessibilityService, and overlay permission are ready.",
                actions = listOf(
                    SfSheetAction("Open Android overlay settings") {
                        startActivity(permissionManager.overlaySettingsIntent())
                    },
                    SfSheetAction("Close", SfSheetActionStyle.Primary) {
                        updateStatus()
                    },
                ),
            )
            show()
        }
    }
}
