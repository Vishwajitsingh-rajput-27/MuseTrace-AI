package com.vishwajitrajput.musetraceai.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishwajitrajput.musetraceai.core.errors.readableMessage
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.usecase.BackupProjectUseCase
import com.vishwajitrajput.musetraceai.domain.usecase.DeleteProjectUseCase
import com.vishwajitrajput.musetraceai.domain.usecase.ExportProjectJsonUseCase
import com.vishwajitrajput.musetraceai.domain.usecase.ObserveHistoryUseCase
import com.vishwajitrajput.musetraceai.domain.usecase.RestoreProjectFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeHistoryUseCase: ObserveHistoryUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val exportProjectJsonUseCase: ExportProjectJsonUseCase,
    private val backupProjectUseCase: BackupProjectUseCase,
    private val restoreProjectFileUseCase: RestoreProjectFileUseCase,
) : ViewModel() {
    val projects: StateFlow<List<TraceProject>> = observeHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val mutableStatus = MutableStateFlow("Projects autosave locally with workflow progress. MuseTrace AI cannot restore an Instagram canvas that Instagram cleared.")
    val status: StateFlow<String> = mutableStatus

    fun delete(projectId: Long) {
        viewModelScope.launch {
            runCatching { deleteProjectUseCase(projectId) }
                .onSuccess { mutableStatus.value = "Project deleted." }
                .onFailure { mutableStatus.value = it.readableMessage() }
        }
    }

    fun exportJson(projectId: Long) {
        viewModelScope.launch {
            mutableStatus.value = "Exporting project JSON..."
            runCatching { exportProjectJsonUseCase(projectId) }
                .onSuccess { uri -> mutableStatus.value = "Project JSON exported: $uri" }
                .onFailure { mutableStatus.value = it.readableMessage() }
        }
    }

    fun backup(projectId: Long) {
        viewModelScope.launch {
            mutableStatus.value = "Creating project backup..."
            runCatching { backupProjectUseCase(projectId) }
                .onSuccess { uri -> mutableStatus.value = "Project backup created: $uri" }
                .onFailure { mutableStatus.value = it.readableMessage() }
        }
    }

    fun restore(uri: String) {
        viewModelScope.launch {
            mutableStatus.value = "Restoring MuseTrace project..."
            runCatching { restoreProjectFileUseCase(uri) }
                .onSuccess { id ->
                    mutableStatus.value = "Project restored. Workflow progress was restored, but any deleted Instagram canvas cannot be recovered. Project id: $id"
                }
                .onFailure { mutableStatus.value = it.readableMessage() }
        }
    }
}
