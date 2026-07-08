package com.vishwajitrajput.musetraceai.domain.usecase

import com.vishwajitrajput.musetraceai.domain.ImageEditorRepository
import com.vishwajitrajput.musetraceai.domain.model.EditedImage
import com.vishwajitrajput.musetraceai.domain.model.EditableImage
import com.vishwajitrajput.musetraceai.domain.model.ImageEditSettings
import javax.inject.Inject

class ImportEditableImageUseCase @Inject constructor(
    private val imageEditorRepository: ImageEditorRepository,
) {
    suspend operator fun invoke(sourceUri: String): EditableImage =
        imageEditorRepository.importImage(sourceUri)
}

class ProcessEditableImageUseCase @Inject constructor(
    private val imageEditorRepository: ImageEditorRepository,
) {
    suspend operator fun invoke(sourceUri: String, settings: ImageEditSettings): EditedImage =
        imageEditorRepository.processImage(sourceUri, settings)
}

class ExportProcessedBitmapUseCase @Inject constructor(
    private val imageEditorRepository: ImageEditorRepository,
) {
    suspend operator fun invoke(sourceUri: String, settings: ImageEditSettings): EditedImage =
        imageEditorRepository.exportProcessedBitmap(sourceUri, settings)
}

data class ImageEditorUseCases @Inject constructor(
    val importImage: ImportEditableImageUseCase,
    val processImage: ProcessEditableImageUseCase,
    val exportProcessedBitmap: ExportProcessedBitmapUseCase,
)
