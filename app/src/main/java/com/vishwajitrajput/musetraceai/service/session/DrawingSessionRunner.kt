package com.vishwajitrajput.musetraceai.service.session

import com.vishwajitrajput.musetraceai.domain.model.CanvasQuality
import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import com.vishwajitrajput.musetraceai.domain.model.TraceStroke
import com.vishwajitrajput.musetraceai.service.accessibility.AccessibilityBridge
import com.vishwajitrajput.musetraceai.service.accessibility.GestureQueueCallbacks
import com.vishwajitrajput.musetraceai.service.accessibility.GestureQueueStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.hypot

object DrawingSessionRunner {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun drawCurrentLayer() {
        val state = DrawingSessionStore.state.value
        val project = state.project
        if (project == null) {
            DrawingSessionStore.markStopped("Open a sketch before drawing.")
            return
        }
        if (!AccessibilityBridge.isConnected()) {
            DrawingSessionStore.markStopped("Enable MuseTrace AI AccessibilityService first.")
            return
        }
        if (!state.calibration.isUsable()) {
            DrawingSessionStore.markStopped("Calibration is missing. Open Calibration, manually use Add Space if needed, then save the final Draw area.")
            return
        }
        val layer = project.layers.getOrNull(state.layerIndex)
        if (layer == null) {
            DrawingSessionStore.markStopped("Selected layer is not available.")
            return
        }
        val requestedLayerIndex = state.layerIndex
        scope.launch {
            val runtime = DrawingSessionStore.currentRuntimeConfig()
            val strokes = layer.strokes.prepareForDispatch(runtime)
            val total = strokes.size
            val startIndex = state.completedStrokes.coerceIn(0, total)
            DrawingSessionStore.markRunning("Drawing ${layer.title}. Keep Instagram Draw open and do not touch the screen.")
            val result = AccessibilityBridge.drawStrokes(
                project = project,
                calibration = state.calibration,
                strokes = strokes,
                startIndex = startIndex,
                speedMultiplier = runtime.drawingSpeed,
                gestureDelayMs = runtime.gestureDelayMs,
                callbacks = object : GestureQueueCallbacks {
                    override fun onStarted(completed: Int, total: Int) {
                        DrawingSessionStore.updateProgress(completed, total, "Drawing ${layer.title}: $completed of $total strokes")
                    }

                    override fun onProgress(completed: Int, total: Int) {
                        DrawingSessionStore.updateProgress(completed, total, "Drawing ${layer.title}: $completed of $total strokes")
                    }

                    override fun onPaused(completed: Int, total: Int, message: String) {
                        DrawingSessionStore.markInterrupted(completed, total)
                    }
                },
            )
            when (result.status) {
                GestureQueueStatus.Completed -> {
                    DrawingSessionStore.updateProgress(result.total, result.total, "Layer complete. Preparing the next color.")
                    DrawingSessionStore.markLayerCompleted(requestedLayerIndex)
                }
                GestureQueueStatus.Cancelled -> {
                    val current = DrawingSessionStore.state.value
                    if (current.layerIndex == requestedLayerIndex && (current.running || current.paused)) {
                        DrawingSessionStore.markStopped(result.message)
                    }
                }
                GestureQueueStatus.EmergencyStopped -> DrawingSessionStore.markStopped(result.message)
                GestureQueueStatus.Failed -> {
                    if (!DrawingSessionStore.state.value.resumeDecisionRequired) {
                        DrawingSessionStore.markStopped(result.message)
                    }
                }
            }
        }
    }

    private fun List<TraceStroke>.prepareForDispatch(config: DrawingRuntimeConfig): List<TraceStroke> {
        val qualityFactor = when (config.canvasQuality) {
            CanvasQuality.Performance -> 1.35f
            CanvasQuality.Balanced -> 1f
            CanvasQuality.High -> 0.72f
        }
        val minLength = (config.minimumStrokeLength * qualityFactor).coerceAtLeast(1f)
        val tolerance = (0.45f + config.simplificationLevel.coerceIn(0f, 1f) * 5.5f) * qualityFactor
        return mapNotNull { stroke ->
            val simplified = stroke.points
                .takeIf { it.size >= 2 }
                ?.douglasPeucker(tolerance)
                ?.smooth(config.smoothingLevel)
                ?: stroke.points
            val tuned = stroke.copy(points = simplified)
            tuned.takeIf { it.length() >= minLength && it.points.isNotEmpty() }
        }
    }

    private fun List<TracePoint>.smooth(level: Float): List<TracePoint> {
        if (size < 3 || level <= 0f) return this
        val amount = level.coerceIn(0f, 1f) * 0.5f
        return indices.map { index ->
            if (index == 0 || index == lastIndex) {
                this[index]
            } else {
                val previous = this[index - 1]
                val current = this[index]
                val next = this[index + 1]
                TracePoint(
                    x = current.x * (1f - amount) + ((previous.x + next.x) / 2f) * amount,
                    y = current.y * (1f - amount) + ((previous.y + next.y) / 2f) * amount,
                )
            }
        }
    }

    private fun List<TracePoint>.douglasPeucker(tolerance: Float): List<TracePoint> {
        if (size <= 2) return this
        var farthestIndex = 0
        var farthestDistance = 0f
        val first = first()
        val last = last()
        for (index in 1 until lastIndex) {
            val distance = this[index].perpendicularDistance(first, last)
            if (distance > farthestDistance) {
                farthestDistance = distance
                farthestIndex = index
            }
        }
        return if (farthestDistance > tolerance) {
            val left = subList(0, farthestIndex + 1).douglasPeucker(tolerance)
            val right = subList(farthestIndex, size).douglasPeucker(tolerance)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun TracePoint.perpendicularDistance(start: TracePoint, end: TracePoint): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        if (dx == 0f && dy == 0f) return distanceTo(start)
        val numerator = kotlin.math.abs(dy * x - dx * y + end.x * start.y - end.y * start.x)
        return numerator / hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.001f)
    }

    private fun TraceStroke.length(): Float =
        points.zipWithNext().sumOf { (from, to) -> from.distanceTo(to).toDouble() }.toFloat()

    private fun TracePoint.distanceTo(other: TracePoint): Float =
        hypot((x - other.x).toDouble(), (y - other.y).toDouble()).toFloat()
}
