package com.vishwajitrajput.musetraceai.service.accessibility

import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.TraceStroke

object AccessibilityBridge {
    @Volatile
    private var service: DrawAccessibilityService? = null

    fun attach(service: DrawAccessibilityService) {
        this.service = service
    }

    fun detach(service: DrawAccessibilityService) {
        if (this.service === service) this.service = null
    }

    fun isConnected(): Boolean = service != null

    fun isInstagramForeground(): Boolean = service?.isInstagramForeground() == true

    fun pauseDrawing() {
        service?.pauseQueue()
    }

    fun resumeDrawing() {
        service?.resumeQueue()
    }

    fun cancelDrawing() {
        service?.cancelQueue()
    }

    fun emergencyStop() {
        service?.emergencyStop()
    }

    suspend fun drawStrokes(
        project: TraceProject,
        calibration: CalibrationProfile,
        strokes: List<TraceStroke>,
        startIndex: Int,
        speedMultiplier: Float = 1f,
        gestureDelayMs: Long = 18L,
        callbacks: GestureQueueCallbacks,
    ): GestureQueueResult {
        val activeService = service
            ?: return GestureQueueResult(
                completed = startIndex,
                total = strokes.size,
                status = GestureQueueStatus.Failed,
                message = "Enable MuseTrace AI AccessibilityService first.",
            )
        return activeService.drawQueue(
            request = GestureQueueRequest(
                project = project,
                calibration = calibration,
                strokes = strokes,
                startIndex = startIndex,
                speedMultiplier = speedMultiplier.coerceIn(0.35f, 2.5f),
                gestureDelayMs = gestureDelayMs.coerceIn(0L, 800L),
            ),
            callbacks = callbacks,
        )
    }
}
