package com.vishwajitrajput.musetraceai.feature.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.core.common.LegalCopy
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.databinding.FragmentOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(FragmentOnboardingBinding::inflate) {
    private val viewModel: OnboardingViewModel by viewModels()
    private var navigating = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.disclaimerText.text = LegalCopy.DISCLAIMER
        binding.drawingWarningText.text = "${LegalCopy.DRAWING_WARNING}\n\n${LegalCopy.RESUME_WARNING}\n\n${LegalCopy.RESUME_EXPLANATION}"
        binding.understandButton.setOnClickListener {
            binding.understandButton.isEnabled = false
            viewModel.acceptDisclaimer()
        }
        collectStarted {
            launch {
                viewModel.disclaimerAccepted.collect { accepted ->
                    if (accepted && !navigating) {
                        navigating = true
                        findNavController().navigate(
                            R.id.action_onboarding_to_home,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.onboardingFragment, true)
                                .setLaunchSingleTop(true)
                                .build(),
                        )
                    }
                }
            }
        }
    }
}
