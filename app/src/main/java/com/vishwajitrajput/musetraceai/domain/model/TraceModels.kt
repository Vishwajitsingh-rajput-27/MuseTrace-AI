package com.vishwajitrajput.musetraceai.domain.model

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

data class TracePoint(
    val x: Float,
    val y: Float,
)

data class TraceStroke(
    val points: List<TracePoint>,
    val durationMs: Long,
)

data class TraceLayer(
    val index: Int,
    val title: String,
    val colorHex: String,
    val strokes: List<TraceStroke>,
    val red: Int = 0,
    val green: Int = 0,
    val blue: Int = 0,
    val colorName: String = "Custom color",
    val difficultyScore: Int = 1,
    val estimatedDrawingTimeMs: Long = 0L,
    val recommendedOrder: Int = index + 1,
    val coveragePercent: Float = 0f,
    val pixelCount: Int = 0,
    val maskUri: String? = null,
    val layerBitmapUri: String? = null,
    val qualityWarnings: List<String> = emptyList(),
    val gestureCount: Int = 0,
    val strokeQualityScore: Int = 100,
    val strokePreviewUri: String? = null,
    val strokeComplexityWarnings: List<String> = emptyList(),
)

data class TracePaletteColor(
    val index: Int,
    val colorHex: String,
    val red: Int,
    val green: Int,
    val blue: Int,
    val colorName: String,
    val difficultyScore: Int,
    val estimatedDrawingTimeMs: Long,
    val recommendedOrder: Int,
    val coveragePercent: Float,
    val pixelCount: Int,
)

data class TraceProjectAssets(
    val originalImageUri: String? = null,
    val geminiGeneratedImageUri: String? = null,
    val processedImageUri: String? = null,
    val previewImageUri: String? = null,
)

data class ProjectDrawingSettings(
    val colorCount: Int = 16,
    val editSettings: ImageEditSettings = ImageEditSettings(),
    val speedMultiplier: Float = 1f,
    val gestureDelayMs: Long = 120L,
    val safeAreaInsetPercent: Float = 1.5f,
)

enum class DrawingSessionLifecycle {
    NotStarted,
    Ready,
    Running,
    Paused,
    Interrupted,
    Completed,
    Cancelled;

    companion object {
        fun fromStorageName(value: String?): DrawingSessionLifecycle =
            entries.firstOrNull { it.name == value } ?: NotStarted
    }
}

data class WorkflowProgress(
    val currentLayerIndex: Int = 0,
    val completedStrokes: Int = 0,
    val totalStrokes: Int = 0,
    val completedLayerIndexes: Set<Int> = emptySet(),
    val skippedLayerIndexes: Set<Int> = emptySet(),
    val sessionState: DrawingSessionLifecycle = DrawingSessionLifecycle.NotStarted,
    val resumeDecisionRequired: Boolean = false,
    val resumeWarning: String? = null,
    val status: String = "Not started.",
    val autosavedAtMillis: Long = 0L,
)

data class OverlayControllerState(
    val hasPosition: Boolean = false,
    val x: Int = 0,
    val y: Int = 0,
    val collapsed: Boolean = false,
    val opacity: Float = 0.94f,
    val sizeScale: Float = 1f,
    val updatedAtMillis: Long = 0L,
)

data class TraceProject(
    val id: Long,
    val title: String,
    val sourceUri: String,
    val previewPath: String,
    val colorCount: Int,
    val width: Int,
    val height: Int,
    val layers: List<TraceLayer>,
    val createdAtMillis: Long,
    val assets: TraceProjectAssets = TraceProjectAssets(),
    val palette: List<TracePaletteColor> = emptyList(),
    val calibrationProfile: CalibrationProfile? = null,
    val overlayState: OverlayControllerState = OverlayControllerState(),
    val drawingSettings: ProjectDrawingSettings = ProjectDrawingSettings(),
    val workflowProgress: WorkflowProgress = WorkflowProgress(),
    val updatedAtMillis: Long = createdAtMillis,
) {
    val strokeCount: Int = layers.sumOf { it.strokes.size }

    val resolvedAssets: TraceProjectAssets
        get() = assets.copy(
            originalImageUri = assets.originalImageUri ?: sourceUri,
            processedImageUri = assets.processedImageUri ?: sourceUri,
            previewImageUri = assets.previewImageUri ?: previewPath,
        )

    val resolvedPalette: List<TracePaletteColor>
        get() = palette.ifEmpty { layers.map { it.toPaletteColor() } }
}

enum class CalibrationProfileType(val displayName: String, val storageKey: String) {
    NormalDraw("Normal Draw", "normal_draw"),
    AddSpaceSmall("Add Space Small", "add_space_small"),
    AddSpaceMedium("Add Space Medium", "add_space_medium"),
    AddSpaceMaximum("Add Space Maximum", "add_space_maximum"),
    Custom("Custom", "custom");

    companion object {
        fun fromStorageKey(key: String?): CalibrationProfileType =
            entries.firstOrNull { it.storageKey == key } ?: NormalDraw
    }
}

data class CalibrationBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float = (right - left).coerceAtLeast(0f)
    val height: Float = (bottom - top).coerceAtLeast(0f)
}

data class OverlaySafeZone(
    val name: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float = (right - left).coerceAtLeast(0f)
    val height: Float = (bottom - top).coerceAtLeast(0f)
}

enum class CalibrationCorner(val displayName: String) {
    TopLeft("top-left"),
    TopRight("top-right"),
    BottomLeft("bottom-left"),
    BottomRight("bottom-right"),
}

data class CalibrationMetrics(
    val bounds: CalibrationBounds,
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Float,
    val offsetY: Float,
    val rotationDegrees: Float,
    val aspectRatio: Float,
    val safeTopLeft: TracePoint,
    val safeTopRight: TracePoint,
    val safeBottomLeft: TracePoint,
    val safeBottomRight: TracePoint,
    val overlaySafeZones: List<OverlaySafeZone>,
)

data class CalibrationProfile(
    val type: CalibrationProfileType = CalibrationProfileType.NormalDraw,
    val topLeft: TracePoint = TracePoint(0f, 0f),
    val topRight: TracePoint = TracePoint(0f, 0f),
    val bottomLeft: TracePoint = TracePoint(0f, 0f),
    val bottomRight: TracePoint = TracePoint(0f, 0f),
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val savedAtMillis: Long = 0L,
) {
    val left: Int get() = metrics().bounds.left.roundToInt()
    val top: Int get() = metrics().bounds.top.roundToInt()
    val width: Int get() = metrics().bounds.width.roundToInt()
    val height: Int get() = metrics().bounds.height.roundToInt()

    fun point(corner: CalibrationCorner): TracePoint =
        when (corner) {
            CalibrationCorner.TopLeft -> topLeft
            CalibrationCorner.TopRight -> topRight
            CalibrationCorner.BottomLeft -> bottomLeft
            CalibrationCorner.BottomRight -> bottomRight
        }

    fun withPoint(corner: CalibrationCorner, point: TracePoint, nextScreenWidth: Int, nextScreenHeight: Int): CalibrationProfile =
        when (corner) {
            CalibrationCorner.TopLeft -> copy(topLeft = point, screenWidth = nextScreenWidth, screenHeight = nextScreenHeight)
            CalibrationCorner.TopRight -> copy(topRight = point, screenWidth = nextScreenWidth, screenHeight = nextScreenHeight)
            CalibrationCorner.BottomLeft -> copy(bottomLeft = point, screenWidth = nextScreenWidth, screenHeight = nextScreenHeight)
            CalibrationCorner.BottomRight -> copy(bottomRight = point, screenWidth = nextScreenWidth, screenHeight = nextScreenHeight)
        }

    fun isUsable(): Boolean {
        val metrics = metrics()
        return savedAtMillis > 0L &&
            metrics.bounds.width >= MIN_DRAWABLE_SIDE_PX &&
            metrics.bounds.height >= MIN_DRAWABLE_SIDE_PX &&
            polygonArea() >= MIN_DRAWABLE_AREA_PX &&
            metrics.scaleX >= MIN_DRAWABLE_SIDE_PX &&
            metrics.scaleY >= MIN_DRAWABLE_SIDE_PX
    }

    fun withType(nextType: CalibrationProfileType): CalibrationProfile = copy(type = nextType)

    fun mapPoint(projectWidth: Int, projectHeight: Int, point: TracePoint, useSafeArea: Boolean = true): TracePoint {
        val u = (point.x / projectWidth.coerceAtLeast(1)).coerceIn(0f, 1f)
        val v = (point.y / projectHeight.coerceAtLeast(1)).coerceIn(0f, 1f)
        val metrics = metrics()
        return if (useSafeArea) {
            bilinear(
                u,
                v,
                metrics.safeTopLeft,
                metrics.safeTopRight,
                metrics.safeBottomLeft,
                metrics.safeBottomRight,
            )
        } else {
            bilinear(u, v, topLeft, topRight, bottomLeft, bottomRight)
        }
    }

    fun metrics(): CalibrationMetrics {
        val bounds = boundsOf(listOf(topLeft, topRight, bottomLeft, bottomRight))
        val topWidth = topLeft.distanceTo(topRight)
        val bottomWidth = bottomLeft.distanceTo(bottomRight)
        val leftHeight = topLeft.distanceTo(bottomLeft)
        val rightHeight = topRight.distanceTo(bottomRight)
        val scaleX = (topWidth + bottomWidth) / 2f
        val scaleY = (leftHeight + rightHeight) / 2f
        val aspectRatio = if (scaleY > 0f) scaleX / scaleY else 0f
        val rotationDegrees = Math.toDegrees(atan2(topRight.y - topLeft.y, topRight.x - topLeft.x).toDouble()).toFloat()
        val safeInset = SAFE_INSET_FRACTION
        return CalibrationMetrics(
            bounds = bounds,
            scaleX = scaleX,
            scaleY = scaleY,
            offsetX = topLeft.x,
            offsetY = topLeft.y,
            rotationDegrees = rotationDegrees,
            aspectRatio = aspectRatio,
            safeTopLeft = bilinear(safeInset, safeInset, topLeft, topRight, bottomLeft, bottomRight),
            safeTopRight = bilinear(1f - safeInset, safeInset, topLeft, topRight, bottomLeft, bottomRight),
            safeBottomLeft = bilinear(safeInset, 1f - safeInset, topLeft, topRight, bottomLeft, bottomRight),
            safeBottomRight = bilinear(1f - safeInset, 1f - safeInset, topLeft, topRight, bottomLeft, bottomRight),
            overlaySafeZones = overlayZones(bounds),
        )
    }

    private fun polygonArea(): Float {
        val points = listOf(topLeft, topRight, bottomRight, bottomLeft)
        val sum = points.indices.sumOf { index ->
            val current = points[index]
            val next = points[(index + 1) % points.size]
            (current.x * next.y - next.x * current.y).toDouble()
        }
        return abs(sum.toFloat()) / 2f
    }

    private fun overlayZones(bounds: CalibrationBounds): List<OverlaySafeZone> {
        if (screenWidth <= 0 || screenHeight <= 0) return emptyList()
        val zones = listOf(
            OverlaySafeZone("Top controls", 0f, 0f, screenWidth.toFloat(), bounds.top),
            OverlaySafeZone("Bottom controls", 0f, bounds.bottom, screenWidth.toFloat(), screenHeight.toFloat()),
            OverlaySafeZone("Left controls", 0f, bounds.top, bounds.left, bounds.bottom),
            OverlaySafeZone("Right controls", bounds.right, bounds.top, screenWidth.toFloat(), bounds.bottom),
        )
        return zones.filter { it.width >= MIN_OVERLAY_ZONE_SIDE_PX && it.height >= MIN_OVERLAY_ZONE_SIDE_PX }
    }

    companion object {
        private const val SAFE_INSET_FRACTION = 0.015f
        private const val MIN_DRAWABLE_SIDE_PX = 120f
        private const val MIN_DRAWABLE_AREA_PX = 14_400f
        private const val MIN_OVERLAY_ZONE_SIDE_PX = 28f

        fun fromBounds(
            type: CalibrationProfileType,
            left: Int,
            top: Int,
            width: Int,
            height: Int,
            screenWidth: Int,
            screenHeight: Int,
            savedAtMillis: Long,
        ): CalibrationProfile {
            val safeWidth = width.coerceAtLeast(0).toFloat()
            val safeHeight = height.coerceAtLeast(0).toFloat()
            val leftF = left.coerceAtLeast(0).toFloat()
            val topF = top.coerceAtLeast(0).toFloat()
            return CalibrationProfile(
                type = type,
                topLeft = TracePoint(leftF, topF),
                topRight = TracePoint(leftF + safeWidth, topF),
                bottomLeft = TracePoint(leftF, topF + safeHeight),
                bottomRight = TracePoint(leftF + safeWidth, topF + safeHeight),
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                savedAtMillis = savedAtMillis,
            )
        }

        private fun bilinear(
            u: Float,
            v: Float,
            topLeft: TracePoint,
            topRight: TracePoint,
            bottomLeft: TracePoint,
            bottomRight: TracePoint,
        ): TracePoint {
            val top = topLeft.lerp(topRight, u)
            val bottom = bottomLeft.lerp(bottomRight, u)
            return top.lerp(bottom, v)
        }

        private fun boundsOf(points: List<TracePoint>): CalibrationBounds =
            CalibrationBounds(
                left = points.minOfOrNull { it.x } ?: 0f,
                top = points.minOfOrNull { it.y } ?: 0f,
                right = points.maxOfOrNull { it.x } ?: 0f,
                bottom = points.maxOfOrNull { it.y } ?: 0f,
            )
    }
}

data class AppSettings(
    val defaultColorCount: Int = 16,
    val calibrationProfiles: Map<CalibrationProfileType, CalibrationProfile> =
        CalibrationProfileType.entries.associateWith { CalibrationProfile(type = it) },
    val selectedCalibrationType: CalibrationProfileType = CalibrationProfileType.NormalDraw,
    val disclaimerAccepted: Boolean = false,
    val drawingSpeed: Float = 1f,
    val smoothingLevel: Float = 0.65f,
    val simplificationLevel: Float = 0.55f,
    val minimumStrokeLength: Float = 6f,
    val gestureDelayMs: Long = 120L,
    val canvasQuality: CanvasQuality = CanvasQuality.Balanced,
    val overlayOpacity: Float = 0.94f,
    val overlaySize: Float = 1f,
    val appTheme: AppTheme = AppTheme.System,
    val simpleMode: Boolean = true,
    val crashRecoveryEnabled: Boolean = true,
    val keepScreenAwake: Boolean = false,
) {
    val calibration: CalibrationProfile =
        calibrationProfiles[selectedCalibrationType] ?: CalibrationProfile(type = selectedCalibrationType)
}

enum class CanvasQuality(val displayName: String, val storageKey: String) {
    Performance("Performance", "performance"),
    Balanced("Balanced", "balanced"),
    High("High quality", "high");

    companion object {
        fun fromStorageKey(key: String?): CanvasQuality =
            entries.firstOrNull { it.storageKey == key } ?: Balanced
    }
}

enum class AppTheme(val displayName: String, val storageKey: String) {
    System("System", "system"),
    Dark("Dark", "dark"),
    Light("Light", "light");

    companion object {
        fun fromStorageKey(key: String?): AppTheme =
            entries.firstOrNull { it.storageKey == key } ?: System
    }
}

private fun TracePoint.distanceTo(other: TracePoint): Float = hypot(x - other.x, y - other.y)

private fun TracePoint.lerp(other: TracePoint, progress: Float): TracePoint =
    TracePoint(
        x = x + (other.x - x) * progress.coerceIn(0f, 1f),
        y = y + (other.y - y) * progress.coerceIn(0f, 1f),
    )

private fun TraceLayer.toPaletteColor(): TracePaletteColor =
    TracePaletteColor(
        index = index,
        colorHex = colorHex,
        red = red,
        green = green,
        blue = blue,
        colorName = colorName,
        difficultyScore = difficultyScore,
        estimatedDrawingTimeMs = estimatedDrawingTimeMs,
        recommendedOrder = recommendedOrder,
        coveragePercent = coveragePercent,
        pixelCount = pixelCount,
    )
