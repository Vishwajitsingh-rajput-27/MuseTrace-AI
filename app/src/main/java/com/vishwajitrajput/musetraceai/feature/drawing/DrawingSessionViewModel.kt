package com.vishwajitrajput.musetraceai.feature.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishwajitrajput.musetraceai.domain.SettingsRepository
import com.vishwajitrajput.musetraceai.domain.model.DrawingSessionLifecycle
import com.vishwajitrajput.musetraceai.domain.usecase.GetProjectUseCase
import com.vishwajitrajput.musetraceai.domain.usecase.SaveProjectUseCase
import com.vishwajitrajput.musetraceai.service.session.DrawingSessionState
import com.vishwajitrajput.musetraceai.service.session.DrawingSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawingSessionViewModel @Inject constructor(
    private val getProjectUseCase: GetProjectUseCase,
    private val saveProjectUseCase: SaveProjectUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val session: StateFlow<DrawingSessionState> = DrawingSessionStore.state
    private var autoSaveJob: Job? = null

    fun load(projectId: Long) {
        viewModelScope.launch {
            val project = getProjectUseCase(projectId)
            val settings = settingsRepository.settings.first()
            DrawingSessionStore.setCrashRecoveryEnabled(settings.crashRecoveryEnabled)
            DrawingSessionStore.setRuntimeSettings(settings)
            if (project == null) {
                DrawingSessionStore.markStopped("Sketch is not available.")
            } else {
                DrawingSessionStore.load(project, settings.calibration)
            }
        }
    }

    fun nextLayer() = DrawingSessionStore.nextLayer()

    fun previousLayer() = DrawingSessionStore.previousLayer()

    fun autoSaveSessionSnapshot(state: DrawingSessionState) {
        val project = state.project ?: return
        if (project.id <= 0L) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            if (!settingsRepository.settings.first().crashRecoveryEnabled) return@launch
            saveProjectUseCase(
                project.copy(
                    calibrationProfile = state.calibration,
                    workflowProgress = state.toWorkflowProgress(),
                    updatedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun DrawingSessionState.toWorkflowProgress(): com.vishwajitrajput.musetraceai.domain.model.WorkflowProgress {
        val lifecycle = when {
            finished -> DrawingSessionLifecycle.Completed
            resumeDecisionRequired -> DrawingSessionLifecycle.Interrupted
            paused -> DrawingSessionLifecycle.Paused
            running -> DrawingSessionLifecycle.Running
            sessionStarted -> DrawingSessionLifecycle.Ready
            else -> DrawingSessionLifecycle.NotStarted
        }
        return com.vishwajitrajput.musetraceai.domain.model.WorkflowProgress(
            currentLayerIndex = layerIndex,
            completedStrokes = completedStrokes,
            totalStrokes = totalStrokes,
            completedLayerIndexes = completedLayerIndexes,
            skippedLayerIndexes = skippedLayerIndexes,
            sessionState = lifecycle,
            resumeDecisionRequired = resumeDecisionRequired,
            resumeWarning = resumeWarning,
            status = status,
            autosavedAtMillis = System.currentTimeMillis(),
        )
    }

    private companion object {
        const val AUTO_SAVE_DEBOUNCE_MS = 250L
    }
}
