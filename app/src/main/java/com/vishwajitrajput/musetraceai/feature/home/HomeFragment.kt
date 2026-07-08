package com.vishwajitrajput.musetraceai.feature.home

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
import com.vishwajitrajput.musetraceai.core.ui.menuButton
import com.vishwajitrajput.musetraceai.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {
    private val viewModel: HomeViewModel by viewModels()
    private var pendingCameraUri: Uri? = null

    @Inject
    lateinit var imageFileStore: ImageFileStore

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { openEditor(it.toString()) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { openEditor(it.toString()) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.openGalleryButton.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.openCameraButton.setOnClickListener { requestCameraCapture() }
        binding.openGeneratorButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_generator)
        }
        buildActionMenu()
        collectStarted {
            launch {
                viewModel.latestProject.collect { project ->
                    if (project == null) {
                        binding.lastProjectTitle.text = "No traced sketches yet"
                        binding.lastProjectMeta.text = "Create a layered sketch from gallery, camera, or Gemini."
                    } else {
                        binding.lastProjectTitle.text = project.title
                        binding.lastProjectMeta.text =
                            "${project.colorCount} colors - ${project.layers.size} layers - ${project.strokeCount} strokes"
                    }
                }
            }
        }
    }

    private fun buildActionMenu() {
        val nav = findNavController()
        val actions = listOf(
            "History" to R.id.action_home_to_history,
            "Calibration" to R.id.action_home_to_calibration,
            "Floating overlay" to R.id.action_home_to_overlay,
            "Settings" to R.id.action_home_to_settings,
            "Guide" to R.id.action_home_to_guide,
            "Troubleshooting" to R.id.action_home_to_troubleshooting,
            "About" to R.id.action_home_to_about,
        )
        binding.actionMenu.removeAllViews()
        actions.forEach { (label, action) ->
            binding.actionMenu.addView(requireContext().menuButton(label) { nav.navigate(action) })
        }
    }

    private fun openEditor(sourceUri: String) {
        findNavController().navigate(
            R.id.action_home_to_editor,
            bundleOf("sourceUri" to sourceUri),
        )
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
}
