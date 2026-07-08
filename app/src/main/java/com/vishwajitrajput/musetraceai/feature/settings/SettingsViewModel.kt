package com.vishwajitrajput.musetraceai.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishwajitrajput.musetraceai.core.errors.readableMessage
import com.vishwajitrajput.musetraceai.core.security.GeminiKeyStore
import com.vishwajitrajput.musetraceai.core.storage.OverlayPositionStore
import com.vishwajitrajput.musetraceai.domain.SettingsRepository
import com.vishwajitrajput.musetraceai.domain.model.AppSettings
import com.vishwajitrajput.musetraceai.domain.model.AppTheme
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfileType
import com.vishwajitrajput.musetraceai.domain.model.CanvasQuality
import com.vishwajitrajput.musetraceai.domain.usecase.TestGeminiKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyStore: GeminiKeyStore,
    private val settingsRepository: SettingsRepository,
    private val testGeminiKeyUseCase: TestGeminiKeyUseCase,
    private val overlayPositionStore: OverlayPositionStore,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    private val mutableStatus = MutableStateFlow("")
    val status: StateFlow<String> = mutableStatus

    fun hasKey(): Boolean = keyStore.hasGeminiKey()

    fun saveKey(key: String) {
        if (key.isBlank()) {
            mutableStatus.value = "Enter a Gemini API key before saving."
        } else {
            keyStore.saveGeminiKey(key)
            mutableStatus.value = "Gemini key saved with encrypted storage."
        }
    }

    fun clearKey() {
        keyStore.clearGeminiKey()
        mutableStatus.value = "Gemini key removed."
    }

    fun testKey() {
        viewModelScope.launch {
            mutableStatus.value = "Testing Gemini key..."
            runCatching { testGeminiKeyUseCase() }
                .onSuccess { ok ->
                    mutableStatus.value = if (ok) {
                        "Gemini key is valid."
                    } else {
                        "Gemini key test did not succeed."
                    }
                }
                .onFailure { error ->
                    mutableStatus.value = error.readableMessage()
                }
        }
    }

    fun saveDefaultColorCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.saveDefaultColorCount(count)
            mutableStatus.value = "Default palette saved: $count colors."
        }
    }

    fun saveDrawingSpeed(speed: Float) = saveSetting("Drawing speed saved.") {
        settingsRepository.saveDrawingSpeed(speed)
    }

    fun saveSmoothingLevel(level: Float) = saveSetting("Smoothing level saved.") {
        settingsRepository.saveSmoothingLevel(level)
    }

    fun saveSimplificationLevel(level: Float) = saveSetting("Simplification level saved.") {
        settingsRepository.saveSimplificationLevel(level)
    }

    fun saveMinimumStrokeLength(length: Float) = saveSetting("Minimum stroke length saved.") {
        settingsRepository.saveMinimumStrokeLength(length)
    }

    fun saveGestureDelay(delayMs: Long) = saveSetting("Gesture delay saved.") {
        settingsRepository.saveGestureDelay(delayMs)
    }

    fun saveCanvasQuality(quality: CanvasQuality) = saveSetting("Canvas quality saved: ${quality.displayName}.") {
        settingsRepository.saveCanvasQuality(quality)
    }

    fun selectCalibrationProfile(type: CalibrationProfileType) =
        saveSetting("Active calibration profile saved: ${type.displayName}.") {
            settingsRepository.selectCalibrationProfile(type)
        }

    fun saveOverlayOpacity(opacity: Float) = saveSetting("Overlay opacity saved.") {
        settingsRepository.saveOverlayOpacity(opacity)
        overlayPositionStore.saveOpacity(opacity)
    }

    fun saveOverlaySize(size: Float) = saveSetting("Overlay size saved.") {
        settingsRepository.saveOverlaySize(size)
        overlayPositionStore.saveSize(size)
    }

    fun resetOverlayPosition() = saveSetting("Overlay position reset. It will avoid the calibrated drawing area when possible.") {
        overlayPositionStore.resetPosition()
    }

    fun saveTheme(theme: AppTheme) = saveSetting("Theme saved: ${theme.displayName}.") {
        settingsRepository.saveAppTheme(theme)
    }

    fun setSimpleMode(simpleMode: Boolean) = saveSetting(
        if (simpleMode) {
            "Simple Mode enabled with safe drawing defaults."
        } else {
            "Advanced Mode enabled."
        },
    ) {
        settingsRepository.saveSimpleMode(simpleMode)
        if (simpleMode) {
            settingsRepository.saveDrawingSpeed(1f)
            settingsRepository.saveSmoothingLevel(0.65f)
            settingsRepository.saveSimplificationLevel(0.55f)
            settingsRepository.saveMinimumStrokeLength(6f)
            settingsRepository.saveGestureDelay(120L)
            settingsRepository.saveCanvasQuality(CanvasQuality.Balanced)
        }
    }

    fun saveCrashRecoveryEnabled(enabled: Boolean) =
        saveSetting(if (enabled) "Crash recovery enabled." else "Crash recovery disabled.") {
            settingsRepository.saveCrashRecoveryEnabled(enabled)
        }

    fun saveKeepScreenAwake(enabled: Boolean) =
        saveSetting(if (enabled) "Keep screen awake enabled." else "Keep screen awake disabled.") {
            settingsRepository.saveKeepScreenAwake(enabled)
        }

    private fun saveSetting(success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { mutableStatus.value = success }
                .onFailure { mutableStatus.value = it.readableMessage() }
        }
    }
}
