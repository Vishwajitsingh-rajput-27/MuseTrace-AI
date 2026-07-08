package com.vishwajitrajput.musetraceai.domain.model

data class ImageEditSettings(
    val cropPercent: Float = 100f,
    val resizePercent: Float = 100f,
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val sharpness: Float = 0f,
    val autoEnhance: Boolean = false,
    val noiseReduction: Float = 0f,
    val edgeEnhance: Float = 0f,
    val backgroundSimplification: Float = 0f,
    val portraitEnhancement: Float = 0f,
    val faceSafeProcessing: Boolean = true,
)

data class EditableImage(
    val sourceUri: String,
    val previewUri: String,
    val width: Int,
    val height: Int,
)

data class EditedImage(
    val processedUri: String,
    val previewUri: String,
    val width: Int,
    val height: Int,
)
