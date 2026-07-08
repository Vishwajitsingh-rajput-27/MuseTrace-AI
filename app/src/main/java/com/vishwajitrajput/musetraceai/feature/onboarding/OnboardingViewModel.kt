package com.vishwajitrajput.musetraceai.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishwajitrajput.musetraceai.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val disclaimerAccepted: StateFlow<Boolean> = settingsRepository.settings
        .map { it.disclaimerAccepted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun acceptDisclaimer() {
        viewModelScope.launch {
            settingsRepository.saveDisclaimerAccepted(true)
        }
    }
}
