package com.vishwajitrajput.musetraceai.feature.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishwajitrajput.musetraceai.core.errors.AppError
import com.vishwajitrajput.musetraceai.core.errors.readableMessage
import com.vishwajitrajput.musetraceai.domain.model.GeneratedImage
import com.vishwajitrajput.musetraceai.domain.model.GenerationRequest
import com.vishwajitrajput.musetraceai.domain.usecase.GenerationUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GeneratorUiState(
    val loading: Boolean = false,
    val status: String = "Gemini is the default provider. Add your key in Settings before generating.",
    val error: String? = null,
    val currentImage: GeneratedImage? = null,
    val history: List<GeneratedImage> = emptyList(),
    val editorUri: String? = null,
)

@HiltViewModel
class AIImageGeneratorViewModel @Inject constructor(
    private val generationUseCases: GenerationUseCases,
) : ViewModel() {
    private val mutableState = MutableStateFlow(GeneratorUiState())
    val state: StateFlow<GeneratorUiState> = mutableState
    private var generationJob: Job? = null
    private var lastRequest: GenerationRequest? = null

    init {
        viewModelScope.launch {
            generationUseCases.observeHistory().collect { history ->
                mutableState.update { it.copy(history = history) }
            }
        }
    }

    fun generate(prompt: String, negativePrompt: String, styleName: String, aspectRatioName: String) {
        val request = generationUseCases.request(prompt, negativePrompt, styleName, aspectRatioName)
        lastRequest = request
        startGeneration(request)
    }

    fun regenerate() {
        val request = lastRequest ?: mutableState.value.currentImage?.let {
            generationUseCases.request(it.prompt, it.negativePrompt, it.styleName, it.aspectRatioName)
        }
        if (request == null) {
            mutableState.update { it.copy(error = "Generate an image before regenerating.") }
        } else {
            startGeneration(request)
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        mutableState.update {
            it.copy(
                loading = false,
                error = AppError.GenerationCancelled().readableMessage(),
                status = "Generation cancelled.",
            )
        }
    }

    fun saveCurrentImage() {
        val image = mutableState.value.currentImage
        if (image == null) {
            mutableState.update { it.copy(error = "Generate an image before saving.") }
            return
        }
        viewModelScope.launch {
            val id = generationUseCases.save(image)
            mutableState.update {
                it.copy(
                    currentImage = image.copy(id = id),
                    status = "Generated image saved to history.",
                    error = null,
                )
            }
        }
    }

    fun useForDrawing() {
        val image = mutableState.value.currentImage
        if (image == null) {
            mutableState.update { it.copy(error = "Generate or select an image before opening the editor.") }
        } else {
            mutableState.update { it.copy(editorUri = image.imageUri) }
        }
    }

    fun selectHistory(image: GeneratedImage) {
        lastRequest = generationUseCases.request(
            image.prompt,
            image.negativePrompt,
            image.styleName,
            image.aspectRatioName,
        )
        mutableState.update {
            it.copy(
                currentImage = image,
                status = "Loaded ${image.styleName} from generation history.",
                error = null,
            )
        }
    }

    fun consumeEditorNavigation() {
        mutableState.update { it.copy(editorUri = null) }
    }

    private fun startGeneration(request: GenerationRequest) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    loading = true,
                    error = null,
                    status = "Generating with Gemini...",
                )
            }
            try {
                val image = generationUseCases.generate(request)
                mutableState.update {
                    it.copy(
                        loading = false,
                        currentImage = image,
                        status = "Generated ${image.styleName}. Use it for drawing to open Editor.",
                        error = null,
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update {
                    it.copy(
                        loading = false,
                        error = AppError.GenerationCancelled().readableMessage(),
                        status = "Generation cancelled.",
                    )
                }
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        loading = false,
                        error = error.readableMessage(),
                        status = "Generation failed.",
                    )
                }
            }
        }
    }
}
