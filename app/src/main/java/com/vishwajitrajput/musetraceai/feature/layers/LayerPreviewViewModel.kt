package com.vishwajitrajput.musetraceai.feature.layers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishwajitrajput.musetraceai.core.errors.readableMessage
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.usecase.GetProjectUseCase
import com.vishwajitrajput.musetraceai.domain.usecase.SaveProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LayerPreviewViewModel @Inject constructor(
    private val getProjectUseCase: GetProjectUseCase,
    private val saveProjectUseCase: SaveProjectUseCase,
) : ViewModel() {
    private val mutableProject = MutableStateFlow<TraceProject?>(null)
    val project: StateFlow<TraceProject?> = mutableProject
    private val mutableStatus = MutableStateFlow("")
    val status: StateFlow<String> = mutableStatus

    fun load(projectId: Long) {
        viewModelScope.launch { mutableProject.value = getProjectUseCase(projectId) }
    }

    fun moveLayer(fromIndex: Int, delta: Int) {
        val project = mutableProject.value ?: return
        val toIndex = (fromIndex + delta).coerceIn(0, project.layers.lastIndex)
        if (fromIndex == toIndex) return
        val reordered = project.layers.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }.mapIndexed { index, layer ->
            layer.copy(
                index = index,
                title = "Layer ${index + 1} - ${layer.colorName}",
            )
        }
        val updated = project.copy(layers = reordered)
        mutableProject.value = updated
        viewModelScope.launch {
            runCatching { saveProjectUseCase(updated) }
                .onSuccess {
                    mutableStatus.value = "Layer order saved. Manual order now controls drawing sequence."
                }
                .onFailure { error ->
                    mutableStatus.value = error.readableMessage()
                    mutableProject.update { project }
                }
        }
    }
}
