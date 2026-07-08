package com.vishwajitrajput.musetraceai.service.session

import android.content.Context
import android.content.SharedPreferences
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfileType
import com.vishwajitrajput.musetraceai.domain.model.AppSettings
import com.vishwajitrajput.musetraceai.domain.model.CanvasQuality
import com.vishwajitrajput.musetraceai.domain.model.DrawingSessionLifecycle
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.WorkflowProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class DrawingSessionState(
    val project: TraceProject? = null,
    val layerIndex: Int = 0,
    val calibration: CalibrationProfile = CalibrationProfile(),
    val running: Boolean = false,
    val paused: Boolean = false,
    val completedStrokes: Int = 0,
    val totalStrokes: Int = 0,
    val completedLayerIndexes: Set<Int> = emptySet(),
    val skippedLayerIndexes: Set<Int> = emptySet(),
    val sessionStarted: Boolean = false,
    val finished: Boolean = false,
    val resumeDecisionRequired: Boolean = false,
    val resumeWarning: String? = null,
    val autosavedAtMillis: Long = 0L,
    val status: String = "No active drawing session",
) {
    val layerCount: Int = project?.layers?.size ?: 0
    val currentLayer: TraceLayer? = project?.layers?.getOrNull(layerIndex)
    val currentLayerColor: String = currentLayer?.colorHex.orEmpty()
    val colorNumber: Int = if (layerCount == 0) 0 else (layerIndex + 1).coerceIn(1, layerCount)
    val completedOrSkippedLayerCount: Int = (completedLayerIndexes + skippedLayerIndexes).size.coerceAtMost(layerCount)
    val canReorderLayers: Boolean = project != null && !running && !sessionStarted && !finished && layerCount > 1
    val progressPercent: Int =
        if (totalStrokes > 0) ((completedStrokes * 100f) / totalStrokes).toInt().coerceIn(0, 100) else 0
    val workflowProgressPercent: Int =
        if (layerCount > 0) {
            val currentFraction = if (running || paused) progressPercent / 100f else 0f
            (((completedOrSkippedLayerCount + currentFraction) * 100f) / layerCount).toInt().coerceIn(0, 100)
        } else {
            0
        }
}

data class DrawingRuntimeConfig(
    val drawingSpeed: Float = 1f,
    val smoothingLevel: Float = 0.65f,
    val simplificationLevel: Float = 0.55f,
    val minimumStrokeLength: Float = 6f,
    val gestureDelayMs: Long = 120L,
    val canvasQuality: CanvasQuality = CanvasQuality.Balanced,
)

object DrawingSessionStore {
    private val mutableState = MutableStateFlow(DrawingSessionState())
    private var preferences: SharedPreferences? = null
    private var crashRecoveryEnabled: Boolean = true
    @Volatile
    private var runtimeConfig = DrawingRuntimeConfig()

    val state: StateFlow<DrawingSessionState> = mutableState

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        autosave(mutableState.value)
    }

    fun setCrashRecoveryEnabled(enabled: Boolean) {
        crashRecoveryEnabled = enabled
        if (!enabled) {
            preferences?.edit()?.clear()?.apply()
        }
    }

    fun setRuntimeSettings(settings: AppSettings) {
        runtimeConfig = DrawingRuntimeConfig(
            drawingSpeed = settings.drawingSpeed,
            smoothingLevel = settings.smoothingLevel,
            simplificationLevel = settings.simplificationLevel,
            minimumStrokeLength = settings.minimumStrokeLength,
            gestureDelayMs = settings.gestureDelayMs,
            canvasQuality = settings.canvasQuality,
        )
    }

    fun currentRuntimeConfig(): DrawingRuntimeConfig = runtimeConfig

    fun load(project: TraceProject, calibration: CalibrationProfile) {
        val saved = if (crashRecoveryEnabled) {
            restore(project) ?: project.workflowProgress.toSavedSession(project)
        } else {
            null
        }
        val orderedProject = saved?.layerOrder?.let { project.withLayerOrder(it) } ?: project
        val restoredCalibration = saved?.calibration?.takeIf { it.isUsable() } ?: calibration
        val safeLayer = saved?.layerIndex?.coerceIn(0, orderedProject.layers.lastIndex.coerceAtLeast(0)) ?: 0
        val completed = saved?.completedLayerIndexes.orEmpty().filterValidLayerIndexes(orderedProject).toSet()
        val skipped = saved?.skippedLayerIndexes.orEmpty().filterValidLayerIndexes(orderedProject).toSet()
        val finished = saved?.finished == true && orderedProject.layers.isNotEmpty()
        val mustChooseResume = saved != null && !finished && (saved.sessionStarted || saved.resumeDecisionRequired)
        val currentLayer = orderedProject.layers.getOrNull(safeLayer)
        setState(
            DrawingSessionState(
                project = orderedProject,
                layerIndex = safeLayer,
                calibration = restoredCalibration,
                completedStrokes = saved?.completedStrokes?.coerceAtLeast(0) ?: 0,
                totalStrokes = currentLayer?.strokes?.size ?: 0,
                completedLayerIndexes = completed,
                skippedLayerIndexes = skipped,
                sessionStarted = saved?.sessionStarted == true,
                finished = finished,
                resumeDecisionRequired = mustChooseResume,
                resumeWarning = if (mustChooseResume) INTERRUPTED_CANVAS_WARNING else null,
                autosavedAtMillis = saved?.savedAtMillis ?: 0L,
                status = when {
                    !restoredCalibration.isUsable() -> "Calibration missing. Save a four-point Instagram Draw calibration before drawing."
                    finished -> COMPLETE_STATUS
                    mustChooseResume -> INTERRUPTED_CANVAS_WARNING
                    saved != null -> readyStatus(orderedProject, safeLayer, restored = true)
                    else -> readyStatus(orderedProject, safeLayer)
                },
            ),
        )
    }

    fun selectLayer(index: Int) {
        mutate { current ->
            val project = current.project ?: return@mutate current
            if (current.running) {
                return@mutate current.copy(status = "Drawing is running. Use the floating overlay controls before changing layers.")
            }
            val safe = index.coerceIn(0, (project.layers.size - 1).coerceAtLeast(0))
            current.copy(
                layerIndex = safe,
                running = false,
                paused = false,
                finished = false,
                resumeDecisionRequired = false,
                resumeWarning = null,
                completedStrokes = 0,
                totalStrokes = project.layers.getOrNull(safe)?.strokes?.size ?: 0,
                status = readyStatus(project, safe),
            )
        }
    }

    fun nextLayer() = selectLayer(state.value.layerIndex + 1)

    fun previousLayer() = selectLayer(state.value.layerIndex - 1)

    fun moveCurrentLayer(delta: Int) {
        mutate { current ->
            val project = current.project ?: return@mutate current
            if (!current.canReorderLayers) {
                return@mutate current.copy(status = "Layer order can be changed before drawing starts.")
            }
            val from = current.layerIndex
            val to = (from + delta).coerceIn(0, project.layers.lastIndex)
            if (from == to) return@mutate current
            val nextLayers = project.layers.toMutableList()
            val moved = nextLayers.removeAt(from)
            nextLayers.add(to, moved)
            val nextProject = project.copy(layers = nextLayers)
            current.copy(
                project = nextProject,
                layerIndex = to,
                totalStrokes = nextProject.layers[to].strokes.size,
                status = "Layer order updated. ${readyStatus(nextProject, to)}",
            )
        }
    }

    fun markRunning(status: String) {
        mutate {
            it.copy(
                running = true,
                paused = false,
                sessionStarted = true,
                finished = false,
                resumeDecisionRequired = false,
                resumeWarning = null,
                status = status,
            )
        }
    }

    fun updateProgress(completed: Int, total: Int, status: String) {
        mutate {
            it.copy(
                running = true,
                completedStrokes = completed.coerceAtLeast(0),
                totalStrokes = total.coerceAtLeast(0),
                sessionStarted = true,
                status = status,
            )
        }
    }

    fun markLayerCompleted(layerPosition: Int) {
        mutate { current ->
            val project = current.project ?: return@mutate current
            if (current.layerIndex != layerPosition || layerPosition !in project.layers.indices) return@mutate current
            val completed = current.completedLayerIndexes + layerPosition
            advanceAfterCurrentLayer(
                current = current,
                project = project,
                completed = completed,
                skipped = current.skippedLayerIndexes - layerPosition,
                statusPrefix = "Layer complete.",
            )
        }
    }

    fun skipCurrentLayer() {
        mutate { current ->
            val project = current.project ?: return@mutate current
            if (current.finished || current.layerIndex !in project.layers.indices) return@mutate current
            val skipped = current.skippedLayerIndexes + current.layerIndex
            advanceAfterCurrentLayer(
                current = current,
                project = project,
                completed = current.completedLayerIndexes - current.layerIndex,
                skipped = skipped,
                statusPrefix = "Layer skipped.",
            )
        }
    }

    fun resetCurrentLayerForRedraw() {
        mutate { current ->
            val project = current.project ?: return@mutate current
            val layer = project.layers.getOrNull(current.layerIndex) ?: return@mutate current
            current.copy(
                running = false,
                paused = false,
                finished = false,
                resumeDecisionRequired = false,
                resumeWarning = null,
                completedStrokes = 0,
                totalStrokes = layer.strokes.size,
                completedLayerIndexes = current.completedLayerIndexes - current.layerIndex,
                skippedLayerIndexes = current.skippedLayerIndexes - current.layerIndex,
                status = "Ready to redraw ${layer.title}. Manually select ${layer.colorHex}, then tap Continue.",
            )
        }
    }

    fun prepareOverlaySession() {
        mutate { current ->
            val project = current.project ?: return@mutate current.copy(status = "Open a sketch before starting a drawing session.")
            val resumeExistingCanvas = current.sessionStarted && !current.finished
            val startIndex = if (resumeExistingCanvas) {
                current.layerIndex.coerceIn(0, project.layers.lastIndex.coerceAtLeast(0))
            } else {
                0
            }
            val activeLayer = project.layers.getOrNull(startIndex)
            val completedStrokes = if (resumeExistingCanvas) {
                current.completedStrokes.coerceIn(0, activeLayer?.strokes?.size ?: 0)
            } else {
                0
            }
            current.copy(
                layerIndex = startIndex,
                running = false,
                paused = false,
                finished = false,
                resumeDecisionRequired = false,
                resumeWarning = null,
                completedStrokes = completedStrokes,
                totalStrokes = activeLayer?.strokes?.size ?: 0,
                completedLayerIndexes = if (resumeExistingCanvas) current.completedLayerIndexes else emptySet(),
                skippedLayerIndexes = if (resumeExistingCanvas) current.skippedLayerIndexes else emptySet(),
                status = when {
                    activeLayer == null -> "No drawable layer is available."
                    resumeExistingCanvas -> "Overlay session ready for canvas resume. Keep Instagram Draw open, manually select ${activeLayer.colorHex}, then tap Continue in the floating overlay."
                    else -> "Overlay session ready. Keep Instagram Draw open, manually select ${activeLayer.colorHex}, then tap Continue in the floating overlay."
                },
            )
        }
    }

    fun markInterrupted(completed: Int, total: Int) {
        mutate { current ->
            current.copy(
                running = true,
                paused = true,
                completedStrokes = completed.coerceAtLeast(0),
                totalStrokes = total.coerceAtLeast(0),
                sessionStarted = true,
                resumeDecisionRequired = true,
                resumeWarning = INTERRUPTED_CANVAS_WARNING,
                status = INTERRUPTED_CANVAS_WARNING,
            )
        }
    }

    fun restartFromLayer1() {
        mutate { current ->
            val project = current.project ?: return@mutate current
            val firstLayer = project.layers.firstOrNull()
            current.copy(
                layerIndex = 0,
                running = false,
                paused = false,
                completedStrokes = 0,
                totalStrokes = firstLayer?.strokes?.size ?: 0,
                completedLayerIndexes = emptySet(),
                skippedLayerIndexes = emptySet(),
                sessionStarted = false,
                finished = false,
                resumeDecisionRequired = false,
                resumeWarning = null,
                status = if (firstLayer == null) {
                    "No drawable layer is available."
                } else {
                    "Restarted from Layer 1. Manually select ${firstLayer.colorHex} in Instagram Draw, then tap Continue."
                },
            )
        }
    }

    fun confirmCanvasResume() {
        mutate { current ->
            val project = current.project ?: return@mutate current
            val layer = project.layers.getOrNull(current.layerIndex) ?: return@mutate current
            current.copy(
                paused = false,
                resumeDecisionRequired = false,
                resumeWarning = null,
                status = "Canvas resume selected. This only works if Instagram Draw is still open and the drawing was not deleted. Current color: ${layer.colorHex}.",
            )
        }
    }

    fun requestRecalibration() {
        mutate {
            it.copy(
                running = false,
                paused = false,
                sessionStarted = false,
                resumeDecisionRequired = false,
                resumeWarning = null,
                status = "Recalibration requested. Open Calibration, save the final Instagram Draw area again, then restart from Layer 1 for best results.",
            )
        }
    }

    fun cancelSession() {
        mutate {
            it.copy(
                running = false,
                paused = false,
                sessionStarted = false,
                resumeDecisionRequired = false,
                resumeWarning = null,
                status = "Drawing session cancelled. Workflow progress remains saved, but MuseTrace AI cannot restore an Instagram canvas that Instagram cleared.",
            )
        }
    }

    fun pause(status: String = "Paused") {
        mutate { it.copy(paused = true, status = status) }
    }

    fun resume() {
        mutate {
            if (it.resumeDecisionRequired) {
                it.copy(status = INTERRUPTED_CANVAS_WARNING)
            } else {
                it.copy(paused = false, status = "Drawing resumed. Keep Instagram Draw open.")
            }
        }
    }

    fun markPaused() {
        mutate { current ->
            if (current.paused) {
                current.copy(paused = false, status = "Drawing resumed. Keep Instagram Draw open.")
            } else {
                current.copy(paused = true, status = "Paused")
            }
        }
    }

    fun markStopped(status: String = "Drawing stopped") {
        mutate { it.copy(running = false, paused = false, status = status) }
    }

    private fun advanceAfterCurrentLayer(
        current: DrawingSessionState,
        project: TraceProject,
        completed: Set<Int>,
        skipped: Set<Int>,
        statusPrefix: String,
    ): DrawingSessionState {
        val nextIndex = nextPendingLayerIndex(project, current.layerIndex, completed + skipped)
        return if (nextIndex == null) {
            current.copy(
                running = false,
                paused = false,
                completedStrokes = current.totalStrokes,
                completedLayerIndexes = completed,
                skippedLayerIndexes = skipped,
                sessionStarted = true,
                finished = true,
                resumeDecisionRequired = false,
                resumeWarning = null,
                status = COMPLETE_STATUS,
            )
        } else {
            current.copy(
                layerIndex = nextIndex,
                running = false,
                paused = false,
                finished = false,
                completedStrokes = 0,
                totalStrokes = project.layers[nextIndex].strokes.size,
                completedLayerIndexes = completed,
                skippedLayerIndexes = skipped,
                sessionStarted = true,
                resumeDecisionRequired = false,
                resumeWarning = null,
                status = "$statusPrefix ${readyStatus(project, nextIndex)}",
            )
        }
    }

    private fun nextPendingLayerIndex(project: TraceProject, currentIndex: Int, blocked: Set<Int>): Int? =
        ((currentIndex + 1)..project.layers.lastIndex).firstOrNull { it !in blocked }

    private fun readyStatus(project: TraceProject, layerIndex: Int, restored: Boolean = false): String {
        val layer = project.layers.getOrNull(layerIndex)
        val prefix = if (restored) "Autosaved progress restored. " else ""
        return if (layer == null) {
            "${prefix}No drawable layer is available."
        } else {
            "${prefix}Color ${layerIndex + 1} of ${project.layers.size}: manually select ${layer.colorHex} in Instagram Draw, then tap Continue."
        }
    }

    private fun setState(next: DrawingSessionState) {
        mutableState.value = next
        autosave(next)
    }

    private fun mutate(transform: (DrawingSessionState) -> DrawingSessionState) {
        var nextState = mutableState.value
        mutableState.update { current ->
            transform(current).also { nextState = it }
        }
        autosave(nextState)
    }

    private fun autosave(state: DrawingSessionState) {
        if (!crashRecoveryEnabled) return
        val project = state.project ?: return
        val savedAt = System.currentTimeMillis()
        preferences?.edit()
            ?.putLong(KEY_PROJECT_ID, project.id)
            ?.putInt(KEY_LAYER_INDEX, state.layerIndex)
            ?.putInt(KEY_COMPLETED_STROKES, state.completedStrokes)
            ?.putString(KEY_COMPLETED_LAYERS, state.completedLayerIndexes.encodeIndexes())
            ?.putString(KEY_SKIPPED_LAYERS, state.skippedLayerIndexes.encodeIndexes())
            ?.putString(KEY_LAYER_ORDER, project.layers.map { it.index }.encodeIndexes())
            ?.putBoolean(KEY_SESSION_STARTED, state.sessionStarted)
            ?.putBoolean(KEY_FINISHED, state.finished)
            ?.putBoolean(KEY_RESUME_DECISION_REQUIRED, state.resumeDecisionRequired)
            ?.putCalibration(state.calibration)
            ?.putLong(KEY_SAVED_AT, savedAt)
            ?.apply()
    }

    private fun restore(project: TraceProject): SavedDrawingSession? {
        val prefs = preferences ?: return null
        val projectId = prefs.getLong(KEY_PROJECT_ID, Long.MIN_VALUE)
        if (projectId != project.id) return null
        return SavedDrawingSession(
            layerIndex = prefs.getInt(KEY_LAYER_INDEX, 0),
            completedStrokes = prefs.getInt(KEY_COMPLETED_STROKES, 0),
            completedLayerIndexes = prefs.getString(KEY_COMPLETED_LAYERS, null).decodeIndexSet(),
            skippedLayerIndexes = prefs.getString(KEY_SKIPPED_LAYERS, null).decodeIndexSet(),
            layerOrder = prefs.getString(KEY_LAYER_ORDER, null).decodeIndexList(),
            sessionStarted = prefs.getBoolean(KEY_SESSION_STARTED, false),
            finished = prefs.getBoolean(KEY_FINISHED, false),
            resumeDecisionRequired = prefs.getBoolean(KEY_RESUME_DECISION_REQUIRED, false),
            calibration = prefs.savedCalibration(),
            savedAtMillis = prefs.getLong(KEY_SAVED_AT, 0L),
        )
    }

    private fun WorkflowProgress.toSavedSession(project: TraceProject): SavedDrawingSession? {
        val hasWorkflow = autosavedAtMillis > 0L || sessionState != DrawingSessionLifecycle.NotStarted
        if (!hasWorkflow) return null
        return SavedDrawingSession(
            layerIndex = currentLayerIndex,
            completedStrokes = completedStrokes,
            completedLayerIndexes = completedLayerIndexes,
            skippedLayerIndexes = skippedLayerIndexes,
            layerOrder = project.layers.map { it.index },
            sessionStarted = sessionState != DrawingSessionLifecycle.NotStarted,
            finished = sessionState == DrawingSessionLifecycle.Completed,
            resumeDecisionRequired = resumeDecisionRequired || sessionState == DrawingSessionLifecycle.Interrupted,
            calibration = project.calibrationProfile,
            savedAtMillis = autosavedAtMillis,
        )
    }

    private data class SavedDrawingSession(
        val layerIndex: Int,
        val completedStrokes: Int,
        val completedLayerIndexes: Set<Int>,
        val skippedLayerIndexes: Set<Int>,
        val layerOrder: List<Int>,
        val sessionStarted: Boolean,
        val finished: Boolean,
        val resumeDecisionRequired: Boolean,
        val calibration: CalibrationProfile?,
        val savedAtMillis: Long,
    )

    private const val PREFS_NAME = "musetrace_drawing_session"
    private const val KEY_PROJECT_ID = "project_id"
    private const val KEY_LAYER_INDEX = "layer_index"
    private const val KEY_COMPLETED_STROKES = "completed_strokes"
    private const val KEY_COMPLETED_LAYERS = "completed_layers"
    private const val KEY_SKIPPED_LAYERS = "skipped_layers"
    private const val KEY_LAYER_ORDER = "layer_order"
    private const val KEY_SESSION_STARTED = "session_started"
    private const val KEY_FINISHED = "finished"
    private const val KEY_RESUME_DECISION_REQUIRED = "resume_decision_required"
    private const val KEY_SAVED_AT = "saved_at"
    const val INTERRUPTED_CANVAS_WARNING = "Instagram Draw may have cleared your canvas. MuseTrace AI saved your project progress, but it cannot restore a drawing that Instagram deleted. For best results, restart from Layer 1."
    const val RESUME_EXPLANATION = "Workflow Resume saved the project, current layer, settings, calibration, strokes, and progress. Canvas Resume is only possible if Instagram Draw is still open and the drawing has not been deleted."
    private const val COMPLETE_STATUS = "Drawing complete. Manually send or save in Instagram. MuseTrace AI will not send anything automatically."
}

private const val KEY_CALIBRATION_TYPE = "calibration_type"
private const val KEY_CALIBRATION_TOP_LEFT_X = "calibration_top_left_x"
private const val KEY_CALIBRATION_TOP_LEFT_Y = "calibration_top_left_y"
private const val KEY_CALIBRATION_TOP_RIGHT_X = "calibration_top_right_x"
private const val KEY_CALIBRATION_TOP_RIGHT_Y = "calibration_top_right_y"
private const val KEY_CALIBRATION_BOTTOM_LEFT_X = "calibration_bottom_left_x"
private const val KEY_CALIBRATION_BOTTOM_LEFT_Y = "calibration_bottom_left_y"
private const val KEY_CALIBRATION_BOTTOM_RIGHT_X = "calibration_bottom_right_x"
private const val KEY_CALIBRATION_BOTTOM_RIGHT_Y = "calibration_bottom_right_y"
private const val KEY_CALIBRATION_SCREEN_WIDTH = "calibration_screen_width"
private const val KEY_CALIBRATION_SCREEN_HEIGHT = "calibration_screen_height"
private const val KEY_CALIBRATION_SAVED_AT = "calibration_saved_at"

private fun SharedPreferences.Editor.putCalibration(calibration: CalibrationProfile): SharedPreferences.Editor =
    putString(KEY_CALIBRATION_TYPE, calibration.type.storageKey)
        .putFloat(KEY_CALIBRATION_TOP_LEFT_X, calibration.topLeft.x)
        .putFloat(KEY_CALIBRATION_TOP_LEFT_Y, calibration.topLeft.y)
        .putFloat(KEY_CALIBRATION_TOP_RIGHT_X, calibration.topRight.x)
        .putFloat(KEY_CALIBRATION_TOP_RIGHT_Y, calibration.topRight.y)
        .putFloat(KEY_CALIBRATION_BOTTOM_LEFT_X, calibration.bottomLeft.x)
        .putFloat(KEY_CALIBRATION_BOTTOM_LEFT_Y, calibration.bottomLeft.y)
        .putFloat(KEY_CALIBRATION_BOTTOM_RIGHT_X, calibration.bottomRight.x)
        .putFloat(KEY_CALIBRATION_BOTTOM_RIGHT_Y, calibration.bottomRight.y)
        .putInt(KEY_CALIBRATION_SCREEN_WIDTH, calibration.screenWidth)
        .putInt(KEY_CALIBRATION_SCREEN_HEIGHT, calibration.screenHeight)
        .putLong(KEY_CALIBRATION_SAVED_AT, calibration.savedAtMillis)

private fun SharedPreferences.savedCalibration(): CalibrationProfile? {
    val savedAt = getLong(KEY_CALIBRATION_SAVED_AT, 0L)
    if (savedAt <= 0L) return null
    return CalibrationProfile(
        type = CalibrationProfileType.fromStorageKey(getString(KEY_CALIBRATION_TYPE, null)),
        topLeft = com.vishwajitrajput.musetraceai.domain.model.TracePoint(
            getFloat(KEY_CALIBRATION_TOP_LEFT_X, 0f),
            getFloat(KEY_CALIBRATION_TOP_LEFT_Y, 0f),
        ),
        topRight = com.vishwajitrajput.musetraceai.domain.model.TracePoint(
            getFloat(KEY_CALIBRATION_TOP_RIGHT_X, 0f),
            getFloat(KEY_CALIBRATION_TOP_RIGHT_Y, 0f),
        ),
        bottomLeft = com.vishwajitrajput.musetraceai.domain.model.TracePoint(
            getFloat(KEY_CALIBRATION_BOTTOM_LEFT_X, 0f),
            getFloat(KEY_CALIBRATION_BOTTOM_LEFT_Y, 0f),
        ),
        bottomRight = com.vishwajitrajput.musetraceai.domain.model.TracePoint(
            getFloat(KEY_CALIBRATION_BOTTOM_RIGHT_X, 0f),
            getFloat(KEY_CALIBRATION_BOTTOM_RIGHT_Y, 0f),
        ),
        screenWidth = getInt(KEY_CALIBRATION_SCREEN_WIDTH, 0),
        screenHeight = getInt(KEY_CALIBRATION_SCREEN_HEIGHT, 0),
        savedAtMillis = savedAt,
    )
}

private fun TraceProject.withLayerOrder(order: List<Int>): TraceProject {
    if (order.isEmpty()) return this
    val byOriginalIndex = layers.associateBy { it.index }
    val ordered = order.mapNotNull { byOriginalIndex[it] }.toMutableList()
    val used = ordered.map { it.index }.toSet()
    layers.filter { it.index !in used }.forEach { ordered.add(it) }
    return if (ordered.size == layers.size) copy(layers = ordered) else this
}

private fun Set<Int>.encodeIndexes(): String = sorted().joinToString(",")

private fun List<Int>.encodeIndexes(): String = joinToString(",")

private fun String?.decodeIndexSet(): Set<Int> =
    decodeIndexList().toSet()

private fun String?.decodeIndexList(): List<Int> =
    orEmpty()
        .split(",")
        .mapNotNull { value -> value.trim().toIntOrNull() }

private fun Set<Int>.filterValidLayerIndexes(project: TraceProject): List<Int> =
    filter { it in project.layers.indices }
