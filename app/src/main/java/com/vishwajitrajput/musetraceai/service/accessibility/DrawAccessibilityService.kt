package com.vishwajitrajput.musetraceai.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.TraceStroke
import com.vishwajitrajput.musetraceai.service.session.DrawingSessionStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.pow
import kotlin.math.sqrt

class DrawAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val queueMutex = Mutex()

    @Volatile
    private var foregroundPackage: String? = null

    @Volatile
    private var queueActive = false

    @Volatile
    private var instagramSafetyBlocked = false

    @Volatile
    private var paused = false

    @Volatile
    private var cancelled = false

    @Volatile
    private var emergencyStopped = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBridge.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            foregroundPackage = packageName
            if (packageName != INSTAGRAM_PACKAGE) {
                instagramSafetyBlocked = false
            }
        }
        if (packageName == INSTAGRAM_PACKAGE) {
            instagramSafetyBlocked = event.indicatesBlockedInstagramSurface()
        }
        if ((!isInstagramForeground() || instagramSafetyBlocked) && isQueueActive()) {
            paused = true
        }
    }

    override fun onInterrupt() {
        emergencyStop()
    }

    override fun onDestroy() {
        emergencyStop()
        AccessibilityBridge.detach(this)
        super.onDestroy()
    }

    fun isInstagramForeground(): Boolean = foregroundPackage == INSTAGRAM_PACKAGE

    fun pauseQueue() {
        paused = true
    }

    fun resumeQueue() {
        paused = false
    }

    fun cancelQueue() {
        cancelled = true
        paused = false
    }

    fun emergencyStop() {
        emergencyStopped = true
        cancelled = true
        paused = false
    }

    suspend fun drawQueue(
        request: GestureQueueRequest,
        callbacks: GestureQueueCallbacks,
    ): GestureQueueResult = queueMutex.withLock {
        queueActive = true
        try {
            runQueue(request, callbacks)
        } finally {
            queueActive = false
        }
    }

    private suspend fun runQueue(
        request: GestureQueueRequest,
        callbacks: GestureQueueCallbacks,
    ): GestureQueueResult {
        if (!request.calibration.isUsable()) {
            return GestureQueueResult(
                completed = request.startIndex,
                total = request.strokes.size,
                status = GestureQueueStatus.Failed,
                message = "Calibration is missing. Drawing blocked.",
            )
        }
        cancelled = false
        emergencyStopped = false
        paused = false
        callbacks.onStarted(request.startIndex, request.strokes.size)
        var completed = request.startIndex.coerceIn(0, request.strokes.size)
        for (index in completed until request.strokes.size) {
            val safetyResult = waitForSafeForeground(callbacks, completed, request.strokes.size)
            if (safetyResult != null) return safetyResult
            if (cancelled || emergencyStopped) break
            val stroke = request.strokes[index]
            val dispatchResult = dispatchWithRecovery(request, stroke)
            if (!dispatchResult) {
                val message = "Gesture failed after retry. Drawing paused at stroke $completed of ${request.strokes.size}."
                paused = true
                callbacks.onPaused(completed, request.strokes.size, message)
                return GestureQueueResult(completed, request.strokes.size, GestureQueueStatus.Failed, message)
            }
            completed++
            callbacks.onProgress(completed, request.strokes.size)
            delay(request.gestureDelayMs)
        }
        return when {
            emergencyStopped -> GestureQueueResult(completed, request.strokes.size, GestureQueueStatus.EmergencyStopped, "Emergency stop.")
            cancelled -> GestureQueueResult(completed, request.strokes.size, GestureQueueStatus.Cancelled, "Drawing cancelled.")
            else -> GestureQueueResult(completed, request.strokes.size, GestureQueueStatus.Completed, "Layer complete.")
        }
    }

    private suspend fun waitForSafeForeground(
        callbacks: GestureQueueCallbacks,
        completed: Int,
        total: Int,
    ): GestureQueueResult? {
        var warningShown = false
        while (paused || !isInstagramForeground() || instagramSafetyBlocked) {
            if (cancelled || emergencyStopped) {
                return if (emergencyStopped) {
                    GestureQueueResult(completed, total, GestureQueueStatus.EmergencyStopped, "Emergency stop.")
                } else {
                    GestureQueueResult(completed, total, GestureQueueStatus.Cancelled, "Drawing cancelled.")
                }
            }
            val unsafeForeground = !isInstagramForeground() || instagramSafetyBlocked
            if (unsafeForeground && !warningShown) {
                paused = true
                callbacks.onPaused(completed, total, foregroundWarningMessage())
                warningShown = true
            }
            delay(FOREGROUND_CHECK_DELAY_MS)
        }
        return null
    }

    private fun foregroundWarningMessage(): String =
        DrawingSessionStore.INTERRUPTED_CANVAS_WARNING

    private suspend fun dispatchWithRecovery(request: GestureQueueRequest, stroke: TraceStroke): Boolean {
        repeat(request.maxRetries + 1) { attempt ->
            val ok = dispatchStroke(request, stroke)
            if (ok) return true
            if (attempt < request.maxRetries) delay(request.retryDelayMs)
        }
        return false
    }

    private suspend fun dispatchStroke(request: GestureQueueRequest, stroke: TraceStroke): Boolean {
        if (stroke.points.isEmpty()) return true
        return suspendCancellableCoroutine { continuation ->
            if (cancelled || emergencyStopped || !isInstagramForeground() || instagramSafetyBlocked) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            val gesture = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        stroke.toSmoothPath(request.project, request.calibration),
                        0L,
                        (stroke.durationMs / request.speedMultiplier)
                            .toLong()
                            .coerceIn(MIN_GESTURE_DURATION_MS, MAX_GESTURE_DURATION_MS),
                    ),
                )
                .build()
            mainHandler.post {
                dispatchGesture(
                    gesture,
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    },
                    mainHandler,
                )
            }
        }
    }

    private fun TraceStroke.toSmoothPath(project: TraceProject, calibration: CalibrationProfile): Path {
        val screenPoints = points
            .bezierInterpolated()
            .map { calibration.mapPoint(project.width, project.height, it, useSafeArea = true) }
        val path = Path()
        val first = screenPoints.firstOrNull() ?: return path
        path.moveTo(first.x, first.y)
        if (screenPoints.size == 1) {
            path.lineTo(first.x + 0.5f, first.y + 0.5f)
            return path
        }
        for (index in 1 until screenPoints.size) {
            val previous = screenPoints[index - 1]
            val current = screenPoints[index]
            val midX = (previous.x + current.x) / 2f
            val midY = (previous.y + current.y) / 2f
            path.quadTo(previous.x, previous.y, midX, midY)
        }
        val last = screenPoints.last()
        path.lineTo(last.x, last.y)
        return path
    }

    private fun List<TracePoint>.bezierInterpolated(): List<TracePoint> {
        if (size < 3) return this
        val output = mutableListOf(first())
        for (index in 0 until lastIndex) {
            val p0 = getOrElse(index - 1) { this[index] }
            val p1 = this[index]
            val p2 = this[index + 1]
            val p3 = getOrElse(index + 2) { p2 }
            val distance = p1.distanceTo(p2)
            val steps = (distance / INTERPOLATION_STEP_PX).toInt().coerceIn(1, 8)
            for (step in 1..steps) {
                val t = step.toFloat() / steps.toFloat()
                output.add(catmullRom(p0, p1, p2, p3, t))
            }
        }
        return output
    }

    private fun catmullRom(p0: TracePoint, p1: TracePoint, p2: TracePoint, p3: TracePoint, t: Float): TracePoint {
        val t2 = t * t
        val t3 = t2 * t
        return TracePoint(
            x = 0.5f * ((2f * p1.x) + (-p0.x + p2.x) * t + (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 + (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3),
            y = 0.5f * ((2f * p1.y) + (-p0.y + p2.y) * t + (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 + (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3),
        )
    }

    private fun TracePoint.distanceTo(other: TracePoint): Float =
        sqrt((x - other.x).pow(2) + (y - other.y).pow(2))

    private fun AccessibilityEvent.indicatesBlockedInstagramSurface(): Boolean {
        val textSurface = buildString {
            className?.let { append(it).append(' ') }
            contentDescription?.let { append(it).append(' ') }
            text.forEach { append(it).append(' ') }
        }.lowercase()
        if (textSurface.isBlank()) return false
        return BLOCKED_INSTAGRAM_SURFACE_KEYWORDS.any { keyword -> textSurface.contains(keyword) }
    }

    private fun isQueueActive(): Boolean = queueActive && !cancelled && !emergencyStopped

    companion object {
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val MIN_GESTURE_DURATION_MS = 80L
        private const val MAX_GESTURE_DURATION_MS = 1_200L
        private const val FOREGROUND_CHECK_DELAY_MS = 220L
        private const val INTERPOLATION_STEP_PX = 18f
        private val BLOCKED_INSTAGRAM_SURFACE_KEYWORDS = listOf(
            "log in",
            "login",
            "sign up",
            "password",
            "forgot password",
            "captcha",
            "security check",
            "challenge",
            "suspicious",
            "verification code",
            "two-factor",
            "2fa",
        )
    }
}

data class GestureQueueRequest(
    val project: TraceProject,
    val calibration: CalibrationProfile,
    val strokes: List<TraceStroke>,
    val startIndex: Int = 0,
    val speedMultiplier: Float = 1f,
    val gestureDelayMs: Long = 18L,
    val retryDelayMs: Long = 160L,
    val maxRetries: Int = 1,
)

interface GestureQueueCallbacks {
    fun onStarted(completed: Int, total: Int)
    fun onProgress(completed: Int, total: Int)
    fun onPaused(completed: Int, total: Int, message: String)
}

data class GestureQueueResult(
    val completed: Int,
    val total: Int,
    val status: GestureQueueStatus,
    val message: String,
)

enum class GestureQueueStatus {
    Completed,
    Cancelled,
    EmergencyStopped,
    Failed,
}
