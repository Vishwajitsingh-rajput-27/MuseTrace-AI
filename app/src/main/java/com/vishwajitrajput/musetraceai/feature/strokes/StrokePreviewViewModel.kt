package com.vishwajitrajput.musetraceai.feature.strokes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.usecase.GetProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StrokePreviewViewModel @Inject constructor(
    private val getProjectUseCase: GetProjectUseCase,
) : ViewModel() {
    private val mutableProject = MutableStateFlow<TraceProject?>(null)
    val project: StateFlow<TraceProject?> = mutableProject

    fun load(projectId: Long) {
        viewModelScope.launch { mutableProject.value = getProjectUseCase(projectId) }
    }
}
