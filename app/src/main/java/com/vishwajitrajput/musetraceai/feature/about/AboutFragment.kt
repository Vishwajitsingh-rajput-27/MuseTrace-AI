package com.vishwajitrajput.musetraceai.feature.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.vishwajitrajput.musetraceai.core.common.AppConstants
import com.vishwajitrajput.musetraceai.core.common.LegalCopy
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.databinding.FragmentAboutBinding

class AboutFragment : BaseFragment<FragmentAboutBinding>(FragmentAboutBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.aboutBody.text = """
            MuseTrace AI is an Android-only drawing-assistance app for converting gallery, camera, and Gemini-generated images into semi-realistic layered sketches.

            Package: ${AppConstants.PACKAGE_NAME}
            Repository: ${AppConstants.GITHUB_USER}/${AppConstants.GITHUB_REPO}

            ${LegalCopy.DISCLAIMER}
        """.trimIndent()
        binding.aboutWarningText.text = "${LegalCopy.DRAWING_WARNING}\n\n${LegalCopy.RESUME_WARNING}\n\n${LegalCopy.RESUME_EXPLANATION}"
        binding.releaseButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.RELEASE_URL)))
        }
    }
}
