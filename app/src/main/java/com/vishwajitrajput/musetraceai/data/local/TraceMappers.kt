package com.vishwajitrajput.musetraceai.data.local

import com.vishwajitrajput.musetraceai.domain.model.TraceProject

fun TraceProject.toEntity(): TraceProjectEntity = TraceProjectEntity(
    id = id,
    title = title,
    sourceUri = sourceUri,
    previewPath = previewPath,
    colorCount = colorCount,
    width = width,
    height = height,
    layersJson = TraceJsonCodec.encode(layers),
    createdAtMillis = createdAtMillis,
    originalImageUri = resolvedAssets.originalImageUri,
    geminiGeneratedImageUri = resolvedAssets.geminiGeneratedImageUri,
    processedImageUri = resolvedAssets.processedImageUri,
    previewImageUri = resolvedAssets.previewImageUri,
    paletteJson = TraceJsonCodec.encodePalette(resolvedPalette),
    calibrationJson = TraceJsonCodec.encodeCalibration(calibrationProfile),
    overlayJson = TraceJsonCodec.encodeOverlayState(overlayState),
    drawingSettingsJson = TraceJsonCodec.encodeDrawingSettings(drawingSettings.copy(colorCount = colorCount)),
    workflowJson = TraceJsonCodec.encodeWorkflow(workflowProgress),
    updatedAtMillis = updatedAtMillis,
)

fun TraceProjectEntity.toDomain(): TraceProject = TraceProject(
    id = id,
    title = title,
    sourceUri = sourceUri,
    previewPath = previewPath,
    colorCount = colorCount,
    width = width,
    height = height,
    layers = TraceJsonCodec.decode(layersJson),
    createdAtMillis = createdAtMillis,
    assets = com.vishwajitrajput.musetraceai.domain.model.TraceProjectAssets(
        originalImageUri = originalImageUri ?: sourceUri,
        geminiGeneratedImageUri = geminiGeneratedImageUri,
        processedImageUri = processedImageUri ?: sourceUri,
        previewImageUri = previewImageUri ?: previewPath,
    ),
    palette = TraceJsonCodec.decodePalette(paletteJson),
    calibrationProfile = TraceJsonCodec.decodeCalibration(calibrationJson),
    overlayState = TraceJsonCodec.decodeOverlayState(overlayJson),
    drawingSettings = TraceJsonCodec.decodeDrawingSettings(drawingSettingsJson).copy(colorCount = colorCount),
    workflowProgress = TraceJsonCodec.decodeWorkflow(workflowJson),
    updatedAtMillis = updatedAtMillis,
)
