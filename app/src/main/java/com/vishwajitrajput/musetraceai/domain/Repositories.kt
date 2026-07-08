package com.vishwajitrajput.musetraceai.domain

import com.vishwajitrajput.musetraceai.domain.model.AppSettings
import com.vishwajitrajput.musetraceai.domain.model.AppTheme
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfileType
import com.vishwajitrajput.musetraceai.domain.model.CanvasQuality
import com.vishwajitrajput.musetraceai.domain.model.EditableImage
import com.vishwajitrajput.musetraceai.domain.model.EditedImage
import com.vishwajitrajput.musetraceai.domain.model.GeneratedImage
import com.vishwajitrajput.musetraceai.domain.model.ImageEditSettings
import com.vishwajitrajput.musetraceai.domain.model.GenerationRequest
import com.vishwajitrajput.musetraceai.domain.model.OverlayControllerState
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.WorkflowProgress
import kotlinx.coroutines.flow.Flow

interface TraceProjectRepository {
    fun observeProjects(): Flow<List<TraceProject>>
    suspend fun getProject(id: Long): TraceProject?
    suspend fun saveProject(project: TraceProject): Long
    suspend fun deleteProject(id: Long)
    suspend fun saveWorkflowProgress(projectId: Long, progress: WorkflowProgress)
    suspend fun saveOverlayState(projectId: Long, overlayState: OverlayControllerState)
    suspend fun exportProjectJson(projectId: Long): String
    suspend fun importProjectJson(json: String): Long
    suspend fun backupProject(projectId: Long): String
    suspend fun restoreProjectFile(uri: String): Long
}

interface ImageProcessorRepository {
    suspend fun createSketch(sourceUri: String, colorCount: Int): TraceProject
}

interface ImageEditorRepository {
    suspend fun importImage(sourceUri: String): EditableImage
    suspend fun processImage(sourceUri: String, settings: ImageEditSettings): EditedImage
    suspend fun exportProcessedBitmap(sourceUri: String, settings: ImageEditSettings): EditedImage
}

interface AiImageRepository {
    suspend fun generateImage(prompt: String, colorCount: Int): String
}

interface AIImageProvider {
    val providerName: String
    suspend fun generate(request: GenerationRequest, enhancedPrompt: String): GeneratedImage
    suspend fun testConnection(): Boolean
}

interface GenerationRepository {
    fun observeHistory(): Flow<List<GeneratedImage>>
    suspend fun generate(request: GenerationRequest): GeneratedImage
    suspend fun saveGeneratedImage(image: GeneratedImage): Long
    suspend fun testGeminiKey(): Boolean
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun saveDefaultColorCount(count: Int)
    suspend fun saveCalibration(left: Int, top: Int, width: Int, height: Int)
    suspend fun saveCalibrationProfile(profile: CalibrationProfile)
    suspend fun selectCalibrationProfile(type: CalibrationProfileType)
    suspend fun saveDisclaimerAccepted(accepted: Boolean)
    suspend fun saveDrawingSpeed(speed: Float)
    suspend fun saveSmoothingLevel(level: Float)
    suspend fun saveSimplificationLevel(level: Float)
    suspend fun saveMinimumStrokeLength(length: Float)
    suspend fun saveGestureDelay(delayMs: Long)
    suspend fun saveCanvasQuality(quality: CanvasQuality)
    suspend fun saveOverlayOpacity(opacity: Float)
    suspend fun saveOverlaySize(size: Float)
    suspend fun saveAppTheme(theme: AppTheme)
    suspend fun saveSimpleMode(simpleMode: Boolean)
    suspend fun saveCrashRecoveryEnabled(enabled: Boolean)
    suspend fun saveKeepScreenAwake(enabled: Boolean)
}
