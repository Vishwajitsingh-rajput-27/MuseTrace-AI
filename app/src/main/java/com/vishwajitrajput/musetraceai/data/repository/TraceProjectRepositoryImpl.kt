package com.vishwajitrajput.musetraceai.data.repository

import com.vishwajitrajput.musetraceai.core.common.DispatchersProvider
import com.vishwajitrajput.musetraceai.core.errors.AppError
import com.vishwajitrajput.musetraceai.core.storage.ImageFileStore
import com.vishwajitrajput.musetraceai.data.local.TraceProjectDao
import com.vishwajitrajput.musetraceai.data.local.TraceJsonCodec
import com.vishwajitrajput.musetraceai.data.local.toDomain
import com.vishwajitrajput.musetraceai.data.local.toEntity
import com.vishwajitrajput.musetraceai.domain.TraceProjectRepository
import com.vishwajitrajput.musetraceai.domain.model.DrawingSessionLifecycle
import com.vishwajitrajput.musetraceai.domain.model.OverlayControllerState
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.TraceProjectAssets
import com.vishwajitrajput.musetraceai.domain.model.WorkflowProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraceProjectRepositoryImpl @Inject constructor(
    private val dao: TraceProjectDao,
    private val imageFileStore: ImageFileStore,
    private val dispatchers: DispatchersProvider,
) : TraceProjectRepository {
    override fun observeProjects(): Flow<List<TraceProject>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getProject(id: Long): TraceProject? = withContext(dispatchers.io) {
        dao.findById(id)?.toDomain()
    }

    override suspend fun saveProject(project: TraceProject): Long = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val persisted = project.copy(
            assets = project.resolvedAssets,
            palette = project.resolvedPalette,
            drawingSettings = project.drawingSettings.copy(colorCount = project.colorCount),
            updatedAtMillis = now,
        )
        val id = dao.insert(persisted.toEntity())
        if (project.id == 0L) {
            dao.findById(id)?.toDomain()?.copy(id = id, updatedAtMillis = now)?.let { dao.insert(it.toEntity()) }
        }
        id
    }

    override suspend fun deleteProject(id: Long) = withContext(dispatchers.io) {
        dao.findById(id)?.toDomain()?.let { project -> deleteProjectFiles(project) }
        dao.deleteById(id)
    }

    override suspend fun saveWorkflowProgress(projectId: Long, progress: WorkflowProgress) = withContext(dispatchers.io) {
        val project = dao.findById(projectId)?.toDomain() ?: return@withContext
        dao.insert(project.copy(workflowProgress = progress, updatedAtMillis = System.currentTimeMillis()).toEntity())
    }

    override suspend fun saveOverlayState(projectId: Long, overlayState: OverlayControllerState) = withContext(dispatchers.io) {
        val project = dao.findById(projectId)?.toDomain() ?: return@withContext
        dao.insert(project.copy(overlayState = overlayState, updatedAtMillis = System.currentTimeMillis()).toEntity())
    }

    override suspend fun exportProjectJson(projectId: Long): String = withContext(dispatchers.io) {
        val project = dao.findById(projectId)?.toDomain()
            ?: throw AppError.ImageLoad("MuseTrace could not find this project to export.")
        imageFileStore.saveProjectJson(project.id, project.title, TraceJsonCodec.encodeProject(project))
    }

    override suspend fun importProjectJson(json: String): Long = withContext(dispatchers.io) {
        val project = TraceJsonCodec.decodeProject(json)
        saveImportedProject(project)
    }

    override suspend fun backupProject(projectId: Long): String = withContext(dispatchers.io) {
        val project = dao.findById(projectId)?.toDomain()
            ?: throw AppError.ImageLoad("MuseTrace could not find this project to back up.")
        val backupFile = imageFileStore.createProjectExportFile(project.id, project.title, "mtrace")
        val uriToEntry = LinkedHashMap<String, String>()
        ZipOutputStream(FileOutputStream(backupFile)).use { zip ->
            collectProjectUris(project).forEachIndexed { index, uri ->
                val entryName = "assets/asset_${index}.${uri.fileExtension()}"
                imageFileStore.openInputStream(uri)?.use { input ->
                    zip.putNextEntry(ZipEntry(entryName))
                    input.copyTo(zip)
                    zip.closeEntry()
                    uriToEntry[uri] = entryName
                }
            }
            val portable = project.withPortableAssetPaths(uriToEntry)
            zip.putNextEntry(ZipEntry(PROJECT_JSON_ENTRY))
            zip.write(TraceJsonCodec.encodeProject(portable).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        imageFileStore.fileUri(backupFile)
    }

    override suspend fun restoreProjectFile(uri: String): Long = withContext(dispatchers.io) {
        try {
            restoreZipProject(uri)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            saveImportedProject(TraceJsonCodec.decodeProject(imageFileStore.readText(uri)))
        }
    }

    private suspend fun saveImportedProject(project: TraceProject): Long {
        val now = System.currentTimeMillis()
        val copied = project.withCopiedAssets().copy(
            id = 0L,
            workflowProgress = project.workflowProgress.withRestoredCanvasWarning(now),
            updatedAtMillis = now,
        )
        return dao.insert(copied.toEntity())
    }

    private suspend fun restoreZipProject(uri: String): Long {
        val assetMap = LinkedHashMap<String, String>()
        var projectJson: String? = null
        val input = imageFileStore.openInputStream(uri)
            ?: throw AppError.ImageLoad("MuseTrace could not open this project backup.")
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    if (entry.name == PROJECT_JSON_ENTRY) {
                        projectJson = zip.readBytes().toString(Charsets.UTF_8)
                    } else {
                        val extension = entry.name.fileExtension()
                        val restoredUri = imageFileStore.writeImageFile("restored_project_asset", extension, zip)
                        assetMap[entry.name] = restoredUri
                    }
                }
                zip.closeEntry()
            }
        }
        val json = projectJson ?: throw AppError.ImageLoad("This backup does not contain a MuseTrace project.")
        return saveImportedProject(TraceJsonCodec.decodeProject(json).withRestoredAssetPaths(assetMap))
    }

    private fun deleteProjectFiles(project: TraceProject) {
        collectProjectUris(project).forEach { imageFileStore.deleteStoredFile(it) }
    }

    private fun collectProjectUris(project: TraceProject): List<String> =
        buildList {
            add(project.sourceUri)
            add(project.previewPath)
            project.resolvedAssets.originalImageUri?.let(::add)
            project.resolvedAssets.geminiGeneratedImageUri?.let(::add)
            project.resolvedAssets.processedImageUri?.let(::add)
            project.resolvedAssets.previewImageUri?.let(::add)
            project.layers.forEach { layer ->
                layer.maskUri?.let(::add)
                layer.layerBitmapUri?.let(::add)
                layer.strokePreviewUri?.let(::add)
            }
        }.filter { it.isNotBlank() }.distinct()

    private fun TraceProject.withCopiedAssets(): TraceProject {
        val copiedAssets = resolvedAssets.copy(
            originalImageUri = imageFileStore.copyIntoImageStorage("project_original", resolvedAssets.originalImageUri),
            geminiGeneratedImageUri = imageFileStore.copyIntoImageStorage("project_gemini", resolvedAssets.geminiGeneratedImageUri),
            processedImageUri = imageFileStore.copyIntoImageStorage("project_processed", resolvedAssets.processedImageUri),
            previewImageUri = imageFileStore.copyIntoImageStorage("project_preview", resolvedAssets.previewImageUri),
        )
        return copy(
            sourceUri = imageFileStore.copyIntoImageStorage("project_source", sourceUri) ?: sourceUri,
            previewPath = copiedAssets.previewImageUri ?: imageFileStore.copyIntoImageStorage("project_preview", previewPath) ?: previewPath,
            assets = copiedAssets,
            layers = layers.map { it.withCopiedFiles() },
            palette = resolvedPalette,
        )
    }

    private fun TraceLayer.withCopiedFiles(): TraceLayer =
        copy(
            maskUri = imageFileStore.copyIntoImageStorage("layer_mask_$index", maskUri),
            layerBitmapUri = imageFileStore.copyIntoImageStorage("layer_bitmap_$index", layerBitmapUri),
            strokePreviewUri = imageFileStore.copyIntoImageStorage("stroke_preview_$index", strokePreviewUri),
        )

    private fun TraceProject.withPortableAssetPaths(uriToEntry: Map<String, String>): TraceProject {
        fun portable(uri: String?): String? = uri?.let { uriToEntry[it] ?: it }
        val portableAssets = resolvedAssets.copy(
            originalImageUri = portable(resolvedAssets.originalImageUri),
            geminiGeneratedImageUri = portable(resolvedAssets.geminiGeneratedImageUri),
            processedImageUri = portable(resolvedAssets.processedImageUri),
            previewImageUri = portable(resolvedAssets.previewImageUri),
        )
        return copy(
            sourceUri = portable(sourceUri) ?: sourceUri,
            previewPath = portable(previewPath) ?: previewPath,
            assets = portableAssets,
            layers = layers.map { layer ->
                layer.copy(
                    maskUri = portable(layer.maskUri),
                    layerBitmapUri = portable(layer.layerBitmapUri),
                    strokePreviewUri = portable(layer.strokePreviewUri),
                )
            },
        )
    }

    private fun TraceProject.withRestoredAssetPaths(assetMap: Map<String, String>): TraceProject {
        fun restored(uri: String?): String? = uri?.let { assetMap[it] ?: it }
        val restoredAssets = TraceProjectAssets(
            originalImageUri = restored(assets.originalImageUri),
            geminiGeneratedImageUri = restored(assets.geminiGeneratedImageUri),
            processedImageUri = restored(assets.processedImageUri),
            previewImageUri = restored(assets.previewImageUri),
        )
        return copy(
            sourceUri = restored(sourceUri) ?: sourceUri,
            previewPath = restored(previewPath) ?: previewPath,
            assets = restoredAssets,
            layers = layers.map { layer ->
                layer.copy(
                    maskUri = restored(layer.maskUri),
                    layerBitmapUri = restored(layer.layerBitmapUri),
                    strokePreviewUri = restored(layer.strokePreviewUri),
                )
            },
        )
    }

    private fun WorkflowProgress.withRestoredCanvasWarning(now: Long): WorkflowProgress {
        if (sessionState == DrawingSessionLifecycle.NotStarted || sessionState == DrawingSessionLifecycle.Completed) return this
        return copy(
            resumeDecisionRequired = true,
            resumeWarning = CANVAS_NOT_RESTORED_WARNING,
            status = CANVAS_NOT_RESTORED_WARNING,
            autosavedAtMillis = now,
        )
    }

    private fun String.fileExtension(): String =
        substringBefore('?')
            .substringAfterLast('/', this)
            .substringAfterLast('.', "bin")
            .lowercase()
            .takeIf { it.matches(Regex("[a-z0-9]{1,5}")) }
            ?: "bin"

    private companion object {
        const val PROJECT_JSON_ENTRY = "project.json"
        const val CANVAS_NOT_RESTORED_WARNING =
            "MuseTrace AI restored the saved project workflow, but it cannot restore an Instagram canvas that Instagram deleted. For best results, restart from Layer 1."
    }
}
