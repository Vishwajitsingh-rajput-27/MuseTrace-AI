package com.vishwajitrajput.musetraceai.domain.usecase

import com.vishwajitrajput.musetraceai.core.common.AppConstants
import com.vishwajitrajput.musetraceai.domain.AiImageRepository
import com.vishwajitrajput.musetraceai.domain.ImageProcessorRepository
import com.vishwajitrajput.musetraceai.domain.TraceProjectRepository
import com.vishwajitrajput.musetraceai.domain.model.OverlayControllerState
import com.vishwajitrajput.musetraceai.domain.model.ImageEditSettings
import com.vishwajitrajput.musetraceai.domain.model.ProjectDrawingSettings
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.TraceProjectAssets
import com.vishwajitrajput.musetraceai.domain.model.WorkflowProgress
import javax.inject.Inject

class CreateSketchUseCase @Inject constructor(
    private val imageProcessorRepository: ImageProcessorRepository,
    private val traceProjectRepository: TraceProjectRepository,
) {
    suspend operator fun invoke(
        sourceUri: String,
        colorCount: Int,
        originalImageUri: String? = null,
        geminiGeneratedImageUri: String? = null,
        processedImageUri: String? = null,
        editSettings: ImageEditSettings = ImageEditSettings(),
    ): Long {
        require(colorCount in AppConstants.ALLOWED_COLOR_COUNTS) {
            "MuseTrace supports 16, 24, or 32 colors."
        }
        val project = imageProcessorRepository.createSketch(sourceUri, colorCount)
        return traceProjectRepository.saveProject(
            project.copy(
                assets = TraceProjectAssets(
                    originalImageUri = originalImageUri ?: sourceUri,
                    geminiGeneratedImageUri = geminiGeneratedImageUri,
                    processedImageUri = processedImageUri ?: sourceUri,
                    previewImageUri = project.previewPath,
                ),
                drawingSettings = ProjectDrawingSettings(
                    colorCount = colorCount,
                    editSettings = editSettings,
                ),
            ),
        )
    }
}

class GenerateAiImageUseCase @Inject constructor(
    private val aiImageRepository: AiImageRepository,
) {
    suspend operator fun invoke(prompt: String, colorCount: Int): String {
        require(prompt.isNotBlank()) { "Enter a prompt first." }
        require(colorCount in AppConstants.ALLOWED_COLOR_COUNTS) {
            "MuseTrace supports 16, 24, or 32 colors."
        }
        return aiImageRepository.generateImage(prompt.trim(), colorCount)
    }
}

class ObserveHistoryUseCase @Inject constructor(
    private val traceProjectRepository: TraceProjectRepository,
) {
    operator fun invoke() = traceProjectRepository.observeProjects()
}

class GetProjectUseCase @Inject constructor(
    private val traceProjectRepository: TraceProjectRepository,
) {
    suspend operator fun invoke(projectId: Long): TraceProject? = traceProjectRepository.getProject(projectId)
}

class SaveProjectUseCase @Inject constructor(
    private val traceProjectRepository: TraceProjectRepository,
) {
    suspend operator fun invoke(project: TraceProject): Long = traceProjectRepository.saveProject(project)
}

class DeleteProjectUseCase @Inject constructor(
    private val traceProjectRepository: TraceProjectRepository,
) {
    suspend operator fun invoke(projectId: Long) = traceProjectRepository.deleteProject(projectId)
}

class SaveWorkflowProgressUseCase @Inject constructor(
    private val traceProjectRepository: TraceProjectRepository,
) {
    suspend operator fun invoke(projectId: Long, progress: WorkflowProgress) =
        traceProjectRepository.saveWorkflowProgress(projectId, progress)
}

class SaveOverlayStateUseCase @Inject constructor(
    private val traceProjectRepository: TraceProjectRepository,
) {
    suspend operator fun invoke(projectId: Long, overlayState: OverlayControllerState) =
        traceProjectRepository.saveOverlayState(projectId, overlayState)
}

class ExportProjectJsonUseCase @Inject constructor(
    private val traceProjectRepository: TraceProjectRepository,
) {
    suspend operator fun invoke(projectId: Long): String = traceProjectRepository.exportProjectJson(projectId)
}

class ImportProjectJsonUseCase @Inject constructor(
    private val traceProjectRepository: TraceProjectRepository,
) {
    suspend operator fun invoke(json: String): Long = traceProjectRepository.importProjectJson(json)
}

class BackupProjectUseCase @Inject constructor(
    private val traceProjectRepository: TraceProjectRepository,
) {
    suspend operator fun invoke(projectId: Long): String = traceProjectRepository.backupProject(projectId)
}

class RestoreProjectFileUseCase @Inject constructor(
    private val traceProjectRepository: TraceProjectRepository,
) {
    suspend operator fun invoke(uri: String): Long = traceProjectRepository.restoreProjectFile(uri)
}
