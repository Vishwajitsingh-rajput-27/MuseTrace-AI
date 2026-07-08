package com.vishwajitrajput.musetraceai.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishwajitrajput.musetraceai.core.errors.AppError
import com.vishwajitrajput.musetraceai.core.errors.readableMessage
import com.vishwajitrajput.musetraceai.domain.model.ImageEditSettings
import com.vishwajitrajput.musetraceai.domain.usecase.CreateSketchUseCase
import com.vishwajitrajput.musetraceai.domain.usecase.ImageEditorUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

enum class EditorTool(
    val label: String,
    val min: Float,
    val max: Float,
    val step: Float,
) {
    CROP("Crop", 60f, 100f, 1f),
    RESIZE("Resize", 40f, 100f, 1f),
    BRIGHTNESS("Brightness", -60f, 60f, 1f),
    CONTRAST("Contrast", 0.5f, 1.8f, 0.05f),
    SATURATION("Saturation", 0f, 1.8f, 0.05f),
    SHARPNESS("Sharpness", 0f, 1f, 0.05f),
    AUTO_ENHANCE("Auto enhance", 0f, 1f, 1f),
    NOISE_REDUCTION("Noise reduction", 0f, 1f, 0.05f),
    EDGE_ENHANCE("Edge enhance", 0f, 1f, 0.05f),
    BACKGROUND_SIMPLIFICATION("Background simplification", 0f, 1f, 0.05f),
    PORTRAIT_ENHANCEMENT("Portrait enhancement", 0f, 1f, 0.05f),
    FACE_SAFE("Face-safe processing", 0f, 1f, 1f);

    fun formatValue(value: Float): String =
        when (this) {
            AUTO_ENHANCE, FACE_SAFE -> if (value >= 0.5f) "On" else "Off"
            CONTRAST, SATURATION -> String.format("%.2f", value)
            SHARPNESS,
            NOISE_REDUCTION,
            EDGE_ENHANCE,
            BACKGROUND_SIMPLIFICATION,
            PORTRAIT_ENHANCEMENT -> "${(value * 100f).roundToInt()}%"
            else -> value.roundToInt().toString()
        }
}

data class EditorUiState(
    val loading: Boolean = false,
    val loadingMessage: String = "",
    val status: String = "Import an image from gallery, camera, or Gemini.",
    val sourceUri: String? = null,
    val geminiGeneratedUri: String? = null,
    val beforePreviewUri: String? = null,
    val processedUri: String? = null,
    val afterPreviewUri: String? = null,
    val exportedUri: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val settings: ImageEditSettings = ImageEditSettings(),
    val activeTool: EditorTool = EditorTool.BRIGHTNESS,
    val dirty: Boolean = false,
    val projectId: Long? = null,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val imageEditorUseCases: ImageEditorUseCases,
    private val createSketchUseCase: CreateSketchUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = mutableState
    private var activeJob: Job? = null

    fun loadSource(sourceUri: String?) {
        if (sourceUri.isNullOrBlank()) {
            mutableState.update { it.copy(status = "Choose an image before editing.") }
            return
        }
        activeJob?.cancel()
        val sourceLooksGeminiGenerated = sourceUri.contains("gemini", ignoreCase = true)
        activeJob = viewModelScope.launch {
            mutableState.value = EditorUiState(
                loading = true,
                loadingMessage = "Importing image safely...",
                status = "Downsampling preview and preparing full-quality processing copy.",
            )
            try {
                val editable = imageEditorUseCases.importImage(sourceUri)
                mutableState.value = EditorUiState(
                    status = "Image imported: ${editable.width} x ${editable.height}. Adjust tools, then apply.",
                    sourceUri = editable.sourceUri,
                    geminiGeneratedUri = if (sourceLooksGeminiGenerated) editable.sourceUri else null,
                    beforePreviewUri = editable.previewUri,
                    width = editable.width,
                    height = editable.height,
                )
            } catch (error: CancellationException) {
                mutableState.value = EditorUiState(status = AppError.GenerationCancelled().readableMessage())
            } catch (error: Throwable) {
                mutableState.value = EditorUiState(status = error.readableMessage())
            }
        }
    }

    fun selectTool(tool: EditorTool) {
        mutableState.update { it.copy(activeTool = tool) }
    }

    fun updateActiveTool(value: Float) {
        mutableState.update { state ->
            state.copy(
                settings = state.settings.withToolValue(state.activeTool, value),
                dirty = true,
                status = "${state.activeTool.label} set to ${state.activeTool.formatValue(value)}. Apply to refresh the after preview.",
            )
        }
    }

    fun rotateLeft() {
        rotateBy(-90)
    }

    fun rotateRight() {
        rotateBy(90)
    }

    fun flipHorizontal() {
        mutableState.update {
            it.copy(
                settings = it.settings.copy(flipHorizontal = !it.settings.flipHorizontal),
                dirty = true,
                status = "Horizontal flip ${if (!it.settings.flipHorizontal) "enabled" else "disabled"}. Apply to refresh.",
            )
        }
    }

    fun flipVertical() {
        mutableState.update {
            it.copy(
                settings = it.settings.copy(flipVertical = !it.settings.flipVertical),
                dirty = true,
                status = "Vertical flip ${if (!it.settings.flipVertical) "enabled" else "disabled"}. Apply to refresh.",
            )
        }
    }

    fun resetEdits() {
        activeJob?.cancel()
        mutableState.update {
            it.copy(
                loading = false,
                loadingMessage = "",
                processedUri = null,
                afterPreviewUri = null,
                exportedUri = null,
                settings = ImageEditSettings(),
                activeTool = EditorTool.BRIGHTNESS,
                dirty = false,
                status = "Editor reset to the imported image.",
            )
        }
    }

    fun applyEdits() {
        val sourceUri = mutableState.value.sourceUri
        if (sourceUri.isNullOrBlank()) {
            mutableState.update { it.copy(status = "Import an image before applying edits.") }
            return
        }
        processCurrentImage(
            loadingMessage = "Applying editor tools...",
            successStatus = "Processed bitmap ready for color quantization.",
            export = false,
        )
    }

    fun exportProcessedBitmap() {
        val sourceUri = mutableState.value.sourceUri
        if (sourceUri.isNullOrBlank()) {
            mutableState.update { it.copy(status = "Import an image before exporting.") }
            return
        }
        processCurrentImage(
            loadingMessage = "Exporting processed bitmap...",
            successStatus = "Processed bitmap exported and ready for sketch conversion.",
            export = true,
        )
    }

    fun createSketch(colorCount: Int) {
        val state = mutableState.value
        val sourceUri = state.sourceUri
        if (sourceUri.isNullOrBlank()) {
            mutableState.update { it.copy(status = "Import an image before creating a sketch.") }
            return
        }
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    loading = true,
                    loadingMessage = "Preparing processed bitmap for sketch...",
                    status = "Applying pending edits before layered conversion.",
                )
            }
            try {
                val sketchSource = if (mutableState.value.dirty || mutableState.value.processedUri == null) {
                    val processed = imageEditorUseCases.processImage(sourceUri, mutableState.value.settings)
                    mutableState.update {
                        it.copy(
                            processedUri = processed.processedUri,
                            afterPreviewUri = processed.previewUri,
                            exportedUri = processed.processedUri,
                            dirty = false,
                        )
                    }
                    processed.processedUri
                } else {
                    mutableState.value.processedUri ?: sourceUri
                }
                mutableState.update {
                    it.copy(
                        loading = true,
                        loadingMessage = "Building layered sketch...",
                        status = "Quantizing processed bitmap into $colorCount drawing colors.",
                    )
                }
                val id = createSketchUseCase(
                    sourceUri = sketchSource,
                    colorCount = colorCount,
                    originalImageUri = sourceUri,
                    geminiGeneratedImageUri = mutableState.value.geminiGeneratedUri,
                    processedImageUri = sketchSource,
                    editSettings = mutableState.value.settings,
                )
                mutableState.update {
                    it.copy(
                        loading = false,
                        loadingMessage = "",
                        status = "Sketch saved from processed bitmap.",
                        projectId = id,
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update {
                    it.copy(
                        loading = false,
                        loadingMessage = "",
                        status = "Editor work cancelled.",
                    )
                }
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        loading = false,
                        loadingMessage = "",
                        status = error.readableMessage(),
                    )
                }
            }
        }
    }

    fun cancelWork() {
        activeJob?.cancel()
        activeJob = null
        mutableState.update {
            it.copy(
                loading = false,
                loadingMessage = "",
                status = "Editor work cancelled.",
            )
        }
    }

    fun showStatus(message: String) {
        mutableState.update { it.copy(status = message) }
    }

    fun consumeNavigation() {
        mutableState.update { it.copy(projectId = null) }
    }

    private fun rotateBy(delta: Int) {
        mutableState.update {
            val rotation = (((it.settings.rotationDegrees + delta) % 360) + 360) % 360
            it.copy(
                settings = it.settings.copy(rotationDegrees = rotation),
                dirty = true,
                status = "Rotation set to $rotation degrees. Apply to refresh.",
            )
        }
    }

    private fun processCurrentImage(
        loadingMessage: String,
        successStatus: String,
        export: Boolean,
    ) {
        val sourceUri = mutableState.value.sourceUri ?: return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    loading = true,
                    loadingMessage = loadingMessage,
                    status = loadingMessage,
                )
            }
            try {
                val edited = if (export) {
                    imageEditorUseCases.exportProcessedBitmap(sourceUri, mutableState.value.settings)
                } else {
                    imageEditorUseCases.processImage(sourceUri, mutableState.value.settings)
                }
                mutableState.update {
                    it.copy(
                        loading = false,
                        loadingMessage = "",
                        processedUri = edited.processedUri,
                        afterPreviewUri = edited.previewUri,
                        exportedUri = if (export) edited.processedUri else it.exportedUri,
                        width = edited.width,
                        height = edited.height,
                        dirty = false,
                        status = successStatus,
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update {
                    it.copy(
                        loading = false,
                        loadingMessage = "",
                        status = "Editor work cancelled.",
                    )
                }
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        loading = false,
                        loadingMessage = "",
                        status = error.readableMessage(),
                    )
                }
            }
        }
    }

    private fun ImageEditSettings.withToolValue(tool: EditorTool, value: Float): ImageEditSettings =
        when (tool) {
            EditorTool.CROP -> copy(cropPercent = value)
            EditorTool.RESIZE -> copy(resizePercent = value)
            EditorTool.BRIGHTNESS -> copy(brightness = value)
            EditorTool.CONTRAST -> copy(contrast = value)
            EditorTool.SATURATION -> copy(saturation = value)
            EditorTool.SHARPNESS -> copy(sharpness = value)
            EditorTool.AUTO_ENHANCE -> copy(autoEnhance = value >= 0.5f)
            EditorTool.NOISE_REDUCTION -> copy(noiseReduction = value)
            EditorTool.EDGE_ENHANCE -> copy(edgeEnhance = value)
            EditorTool.BACKGROUND_SIMPLIFICATION -> copy(backgroundSimplification = value)
            EditorTool.PORTRAIT_ENHANCEMENT -> copy(portraitEnhancement = value)
            EditorTool.FACE_SAFE -> copy(faceSafeProcessing = value >= 0.5f)
        }

    fun toolValue(tool: EditorTool, settings: ImageEditSettings): Float =
        when (tool) {
            EditorTool.CROP -> settings.cropPercent
            EditorTool.RESIZE -> settings.resizePercent
            EditorTool.BRIGHTNESS -> settings.brightness
            EditorTool.CONTRAST -> settings.contrast
            EditorTool.SATURATION -> settings.saturation
            EditorTool.SHARPNESS -> settings.sharpness
            EditorTool.AUTO_ENHANCE -> if (settings.autoEnhance) 1f else 0f
            EditorTool.NOISE_REDUCTION -> settings.noiseReduction
            EditorTool.EDGE_ENHANCE -> settings.edgeEnhance
            EditorTool.BACKGROUND_SIMPLIFICATION -> settings.backgroundSimplification
            EditorTool.PORTRAIT_ENHANCEMENT -> settings.portraitEnhancement
            EditorTool.FACE_SAFE -> if (settings.faceSafeProcessing) 1f else 0f
        }

}
