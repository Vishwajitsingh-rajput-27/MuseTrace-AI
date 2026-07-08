package com.vishwajitrajput.musetraceai.data.local

import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfileType
import com.vishwajitrajput.musetraceai.domain.model.DrawingSessionLifecycle
import com.vishwajitrajput.musetraceai.domain.model.ImageEditSettings
import com.vishwajitrajput.musetraceai.domain.model.OverlayControllerState
import com.vishwajitrajput.musetraceai.domain.model.ProjectDrawingSettings
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.model.TracePaletteColor
import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.TraceProjectAssets
import com.vishwajitrajput.musetraceai.domain.model.TraceStroke
import com.vishwajitrajput.musetraceai.domain.model.WorkflowProgress
import org.json.JSONArray
import org.json.JSONObject

object TraceJsonCodec {
    private const val EXPORT_SCHEMA_VERSION = 1

    fun encode(layers: List<TraceLayer>): String {
        val array = JSONArray()
        layers.forEach { layer ->
            val strokes = JSONArray()
            layer.strokes.forEach { stroke ->
                val points = JSONArray()
                stroke.points.forEach { point ->
                    points.put(JSONArray().put(point.x.toDouble()).put(point.y.toDouble()))
                }
                strokes.put(
                    JSONObject()
                        .put("durationMs", stroke.durationMs)
                        .put("points", points),
                )
            }
            array.put(
                JSONObject()
                    .put("index", layer.index)
                    .put("title", layer.title)
                    .put("colorHex", layer.colorHex)
                    .put("red", layer.red)
                    .put("green", layer.green)
                    .put("blue", layer.blue)
                    .put("colorName", layer.colorName)
                    .put("difficultyScore", layer.difficultyScore)
                    .put("estimatedDrawingTimeMs", layer.estimatedDrawingTimeMs)
                    .put("recommendedOrder", layer.recommendedOrder)
                    .put("coveragePercent", layer.coveragePercent.toDouble())
                    .put("pixelCount", layer.pixelCount)
                    .put("maskUri", layer.maskUri)
                    .put("layerBitmapUri", layer.layerBitmapUri)
                    .put("qualityWarnings", JSONArray().apply {
                        layer.qualityWarnings.forEach { warning -> put(warning) }
                    })
                    .put("gestureCount", layer.gestureCount)
                    .put("strokeQualityScore", layer.strokeQualityScore)
                    .put("strokePreviewUri", layer.strokePreviewUri)
                    .put("strokeComplexityWarnings", JSONArray().apply {
                        layer.strokeComplexityWarnings.forEach { warning -> put(warning) }
                    })
                    .put("strokes", strokes),
            )
        }
        return array.toString()
    }

    fun decode(json: String): List<TraceLayer> {
        val array = JSONArray(json)
        return List(array.length()) { layerIndex ->
            val layerObject = array.getJSONObject(layerIndex)
            val strokesArray = layerObject.getJSONArray("strokes")
            val strokes = List(strokesArray.length()) { strokeIndex ->
                val strokeObject = strokesArray.getJSONObject(strokeIndex)
                val pointsArray = strokeObject.getJSONArray("points")
                val points = List(pointsArray.length()) { pointIndex ->
                    val point = pointsArray.getJSONArray(pointIndex)
                    TracePoint(
                        x = point.getDouble(0).toFloat(),
                        y = point.getDouble(1).toFloat(),
                    )
                }
                TraceStroke(
                    points = points,
                    durationMs = strokeObject.optLong("durationMs", 180L),
                )
            }
            TraceLayer(
                index = layerObject.optInt("index", layerIndex),
                title = layerObject.optString("title", "Layer ${layerIndex + 1}"),
                colorHex = layerObject.getString("colorHex"),
                strokes = strokes,
                red = layerObject.optInt("red", layerObject.getString("colorHex").redFromHex()),
                green = layerObject.optInt("green", layerObject.getString("colorHex").greenFromHex()),
                blue = layerObject.optInt("blue", layerObject.getString("colorHex").blueFromHex()),
                colorName = layerObject.optString("colorName", "Custom color"),
                difficultyScore = layerObject.optInt("difficultyScore", 1),
                estimatedDrawingTimeMs = layerObject.optLong("estimatedDrawingTimeMs", 0L),
                recommendedOrder = layerObject.optInt("recommendedOrder", layerIndex + 1),
                coveragePercent = layerObject.optDouble("coveragePercent", 0.0).toFloat(),
                pixelCount = layerObject.optInt("pixelCount", 0),
                maskUri = layerObject.optNullableString("maskUri"),
                layerBitmapUri = layerObject.optNullableString("layerBitmapUri"),
                qualityWarnings = layerObject.optJSONArray("qualityWarnings")?.let { warnings ->
                    List(warnings.length()) { warningIndex -> warnings.optString(warningIndex) }
                        .filter { it.isNotBlank() }
                }.orEmpty(),
                gestureCount = layerObject.optInt("gestureCount", strokes.size),
                strokeQualityScore = layerObject.optInt("strokeQualityScore", 100),
                strokePreviewUri = layerObject.optNullableString("strokePreviewUri"),
                strokeComplexityWarnings = layerObject.optJSONArray("strokeComplexityWarnings")?.let { warnings ->
                    List(warnings.length()) { warningIndex -> warnings.optString(warningIndex) }
                        .filter { it.isNotBlank() }
                }.orEmpty(),
            )
        }
    }

    fun encodePalette(palette: List<TracePaletteColor>): String {
        val array = JSONArray()
        palette.forEach { color ->
            array.put(
                JSONObject()
                    .put("index", color.index)
                    .put("colorHex", color.colorHex)
                    .put("red", color.red)
                    .put("green", color.green)
                    .put("blue", color.blue)
                    .put("colorName", color.colorName)
                    .put("difficultyScore", color.difficultyScore)
                    .put("estimatedDrawingTimeMs", color.estimatedDrawingTimeMs)
                    .put("recommendedOrder", color.recommendedOrder)
                    .put("coveragePercent", color.coveragePercent.toDouble())
                    .put("pixelCount", color.pixelCount),
            )
        }
        return array.toString()
    }

    fun decodePalette(json: String): List<TracePaletteColor> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val hex = item.optString("colorHex", "#000000")
            TracePaletteColor(
                index = item.optInt("index", index),
                colorHex = hex,
                red = item.optInt("red", hex.redFromHex()),
                green = item.optInt("green", hex.greenFromHex()),
                blue = item.optInt("blue", hex.blueFromHex()),
                colorName = item.optString("colorName", "Custom color"),
                difficultyScore = item.optInt("difficultyScore", 1),
                estimatedDrawingTimeMs = item.optLong("estimatedDrawingTimeMs", 0L),
                recommendedOrder = item.optInt("recommendedOrder", index + 1),
                coveragePercent = item.optDouble("coveragePercent", 0.0).toFloat(),
                pixelCount = item.optInt("pixelCount", 0),
            )
        }
    }

    fun encodeCalibration(profile: CalibrationProfile?): String? =
        profile?.let {
            JSONObject()
                .put("type", it.type.storageKey)
                .put("topLeft", it.topLeft.toJson())
                .put("topRight", it.topRight.toJson())
                .put("bottomLeft", it.bottomLeft.toJson())
                .put("bottomRight", it.bottomRight.toJson())
                .put("screenWidth", it.screenWidth)
                .put("screenHeight", it.screenHeight)
                .put("savedAtMillis", it.savedAtMillis)
                .toString()
        }

    fun decodeCalibration(json: String?): CalibrationProfile? =
        json?.takeIf { it.isNotBlank() }?.let { raw ->
            val item = JSONObject(raw)
            CalibrationProfile(
                type = CalibrationProfileType.fromStorageKey(item.optString("type")),
                topLeft = item.optJSONArray("topLeft").toTracePoint(),
                topRight = item.optJSONArray("topRight").toTracePoint(),
                bottomLeft = item.optJSONArray("bottomLeft").toTracePoint(),
                bottomRight = item.optJSONArray("bottomRight").toTracePoint(),
                screenWidth = item.optInt("screenWidth", 0),
                screenHeight = item.optInt("screenHeight", 0),
                savedAtMillis = item.optLong("savedAtMillis", 0L),
            )
        }

    fun encodeOverlayState(state: OverlayControllerState): String =
        JSONObject()
            .put("hasPosition", state.hasPosition)
            .put("x", state.x)
            .put("y", state.y)
            .put("collapsed", state.collapsed)
            .put("opacity", state.opacity.toDouble())
            .put("sizeScale", state.sizeScale.toDouble())
            .put("updatedAtMillis", state.updatedAtMillis)
            .toString()

    fun decodeOverlayState(json: String?): OverlayControllerState =
        json?.takeIf { it.isNotBlank() }?.let { raw ->
            val item = JSONObject(raw)
            OverlayControllerState(
                hasPosition = item.optBoolean("hasPosition", false),
                x = item.optInt("x", 0),
                y = item.optInt("y", 0),
                collapsed = item.optBoolean("collapsed", false),
                opacity = item.optDouble("opacity", 0.94).toFloat().coerceIn(0.1f, 1f),
                sizeScale = item.optDouble("sizeScale", 1.0).toFloat().coerceIn(0.8f, 1.35f),
                updatedAtMillis = item.optLong("updatedAtMillis", 0L),
            )
        } ?: OverlayControllerState()

    fun encodeDrawingSettings(settings: ProjectDrawingSettings): String =
        JSONObject()
            .put("colorCount", settings.colorCount)
            .put("editSettings", settings.editSettings.toJson())
            .put("speedMultiplier", settings.speedMultiplier.toDouble())
            .put("gestureDelayMs", settings.gestureDelayMs)
            .put("safeAreaInsetPercent", settings.safeAreaInsetPercent.toDouble())
            .toString()

    fun decodeDrawingSettings(json: String?): ProjectDrawingSettings =
        json?.takeIf { it.isNotBlank() }?.let { raw ->
            val item = JSONObject(raw)
            ProjectDrawingSettings(
                colorCount = item.optInt("colorCount", 16),
                editSettings = item.optJSONObject("editSettings").toImageEditSettings(),
                speedMultiplier = item.optDouble("speedMultiplier", 1.0).toFloat().coerceIn(0.1f, 4f),
                gestureDelayMs = item.optLong("gestureDelayMs", 120L).coerceAtLeast(0L),
                safeAreaInsetPercent = item.optDouble("safeAreaInsetPercent", 1.5).toFloat().coerceIn(0f, 10f),
            )
        } ?: ProjectDrawingSettings()

    fun encodeWorkflow(progress: WorkflowProgress): String =
        JSONObject()
            .put("currentLayerIndex", progress.currentLayerIndex)
            .put("completedStrokes", progress.completedStrokes)
            .put("totalStrokes", progress.totalStrokes)
            .put("completedLayerIndexes", progress.completedLayerIndexes.toJsonArray())
            .put("skippedLayerIndexes", progress.skippedLayerIndexes.toJsonArray())
            .put("sessionState", progress.sessionState.name)
            .put("resumeDecisionRequired", progress.resumeDecisionRequired)
            .put("resumeWarning", progress.resumeWarning)
            .put("status", progress.status)
            .put("autosavedAtMillis", progress.autosavedAtMillis)
            .toString()

    fun decodeWorkflow(json: String?): WorkflowProgress =
        json?.takeIf { it.isNotBlank() }?.let { raw ->
            val item = JSONObject(raw)
            WorkflowProgress(
                currentLayerIndex = item.optInt("currentLayerIndex", 0),
                completedStrokes = item.optInt("completedStrokes", 0),
                totalStrokes = item.optInt("totalStrokes", 0),
                completedLayerIndexes = item.optJSONArray("completedLayerIndexes").toIndexSet(),
                skippedLayerIndexes = item.optJSONArray("skippedLayerIndexes").toIndexSet(),
                sessionState = DrawingSessionLifecycle.fromStorageName(item.optString("sessionState")),
                resumeDecisionRequired = item.optBoolean("resumeDecisionRequired", false),
                resumeWarning = item.optNullableString("resumeWarning"),
                status = item.optString("status", "Not started."),
                autosavedAtMillis = item.optLong("autosavedAtMillis", 0L),
            )
        } ?: WorkflowProgress()

    fun encodeProject(project: TraceProject): String =
        JSONObject()
            .put("schema", "musetrace.project")
            .put("schemaVersion", EXPORT_SCHEMA_VERSION)
            .put("id", project.id)
            .put("title", project.title)
            .put("sourceUri", project.sourceUri)
            .put("previewPath", project.previewPath)
            .put("colorCount", project.colorCount)
            .put("width", project.width)
            .put("height", project.height)
            .put("createdAtMillis", project.createdAtMillis)
            .put("updatedAtMillis", project.updatedAtMillis)
            .put("assets", project.resolvedAssets.toJson())
            .put("palette", JSONArray(encodePalette(project.resolvedPalette)))
            .put("layers", JSONArray(encode(project.layers)))
            .put("calibration", project.calibrationProfile?.let { JSONObject(encodeCalibration(it).orEmpty()) })
            .put("overlay", JSONObject(encodeOverlayState(project.overlayState)))
            .put("drawingSettings", JSONObject(encodeDrawingSettings(project.drawingSettings)))
            .put("workflow", JSONObject(encodeWorkflow(project.workflowProgress)))
            .put("canvasRestoreDisclaimer", "MuseTrace AI saves workflow progress, but it cannot restore an Instagram canvas that Instagram deleted.")
            .toString(2)

    fun decodeProject(json: String): TraceProject {
        val item = JSONObject(json)
        val layers = decode(item.getJSONArray("layers").toString())
        val sourceUri = item.optString("sourceUri")
        val previewPath = item.optString("previewPath")
        val assets = item.optJSONObject("assets").toAssets(sourceUri, previewPath)
        return TraceProject(
            id = item.optLong("id", 0L),
            title = item.optString("title", "Imported MuseTrace project"),
            sourceUri = sourceUri,
            previewPath = previewPath,
            colorCount = item.optInt("colorCount", layers.size.coerceIn(16, 32)),
            width = item.optInt("width", 0),
            height = item.optInt("height", 0),
            layers = layers,
            createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
            assets = assets,
            palette = item.optJSONArray("palette")?.let { decodePalette(it.toString()) }.orEmpty(),
            calibrationProfile = item.optJSONObject("calibration")?.let { decodeCalibration(it.toString()) },
            overlayState = item.optJSONObject("overlay")?.let { decodeOverlayState(it.toString()) } ?: OverlayControllerState(),
            drawingSettings = item.optJSONObject("drawingSettings")?.let { decodeDrawingSettings(it.toString()) } ?: ProjectDrawingSettings(),
            workflowProgress = item.optJSONObject("workflow")?.let { decodeWorkflow(it.toString()) } ?: WorkflowProgress(),
            updatedAtMillis = item.optLong("updatedAtMillis", System.currentTimeMillis()),
        )
    }
}

private fun JSONObject.optNullableString(name: String): String? =
    optString(name).takeIf { it.isNotBlank() && it != "null" }

private fun TracePoint.toJson(): JSONArray = JSONArray().put(x.toDouble()).put(y.toDouble())

private fun JSONArray?.toTracePoint(): TracePoint =
    TracePoint(
        x = this?.optDouble(0, 0.0)?.toFloat() ?: 0f,
        y = this?.optDouble(1, 0.0)?.toFloat() ?: 0f,
    )

private fun Set<Int>.toJsonArray(): JSONArray =
    JSONArray().also { array -> sorted().forEach { array.put(it) } }

private fun JSONArray?.toIndexSet(): Set<Int> =
    if (this == null) {
        emptySet()
    } else {
        List(length()) { index -> optInt(index) }.toSet()
    }

private fun TraceProjectAssets.toJson(): JSONObject =
    JSONObject()
        .put("originalImageUri", originalImageUri)
        .put("geminiGeneratedImageUri", geminiGeneratedImageUri)
        .put("processedImageUri", processedImageUri)
        .put("previewImageUri", previewImageUri)

private fun JSONObject?.toAssets(sourceUri: String, previewPath: String): TraceProjectAssets =
    TraceProjectAssets(
        originalImageUri = this?.optNullableString("originalImageUri") ?: sourceUri,
        geminiGeneratedImageUri = this?.optNullableString("geminiGeneratedImageUri"),
        processedImageUri = this?.optNullableString("processedImageUri") ?: sourceUri,
        previewImageUri = this?.optNullableString("previewImageUri") ?: previewPath,
    )

private fun ImageEditSettings.toJson(): JSONObject =
    JSONObject()
        .put("cropPercent", cropPercent.toDouble())
        .put("resizePercent", resizePercent.toDouble())
        .put("rotationDegrees", rotationDegrees)
        .put("flipHorizontal", flipHorizontal)
        .put("flipVertical", flipVertical)
        .put("brightness", brightness.toDouble())
        .put("contrast", contrast.toDouble())
        .put("saturation", saturation.toDouble())
        .put("sharpness", sharpness.toDouble())
        .put("autoEnhance", autoEnhance)
        .put("noiseReduction", noiseReduction.toDouble())
        .put("edgeEnhance", edgeEnhance.toDouble())
        .put("backgroundSimplification", backgroundSimplification.toDouble())
        .put("portraitEnhancement", portraitEnhancement.toDouble())
        .put("faceSafeProcessing", faceSafeProcessing)

private fun JSONObject?.toImageEditSettings(): ImageEditSettings =
    if (this == null) {
        ImageEditSettings()
    } else {
        ImageEditSettings(
            cropPercent = optDouble("cropPercent", 100.0).toFloat(),
            resizePercent = optDouble("resizePercent", 100.0).toFloat(),
            rotationDegrees = optInt("rotationDegrees", 0),
            flipHorizontal = optBoolean("flipHorizontal", false),
            flipVertical = optBoolean("flipVertical", false),
            brightness = optDouble("brightness", 0.0).toFloat(),
            contrast = optDouble("contrast", 1.0).toFloat(),
            saturation = optDouble("saturation", 1.0).toFloat(),
            sharpness = optDouble("sharpness", 0.0).toFloat(),
            autoEnhance = optBoolean("autoEnhance", false),
            noiseReduction = optDouble("noiseReduction", 0.0).toFloat(),
            edgeEnhance = optDouble("edgeEnhance", 0.0).toFloat(),
            backgroundSimplification = optDouble("backgroundSimplification", 0.0).toFloat(),
            portraitEnhancement = optDouble("portraitEnhancement", 0.0).toFloat(),
            faceSafeProcessing = optBoolean("faceSafeProcessing", true),
        )
    }

private fun String.redFromHex(): Int = hexComponent(1)

private fun String.greenFromHex(): Int = hexComponent(3)

private fun String.blueFromHex(): Int = hexComponent(5)

private fun String.hexComponent(start: Int): Int =
    runCatching { substring(start, start + 2).toInt(16) }.getOrDefault(0)
