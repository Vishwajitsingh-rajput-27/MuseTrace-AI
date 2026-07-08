package com.vishwajitrajput.musetraceai.service.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.vishwajitrajput.musetraceai.MainActivity
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.core.storage.OverlayPositionStore
import com.vishwajitrajput.musetraceai.core.ui.SfButton
import com.vishwajitrajput.musetraceai.core.ui.SfDangerButton
import com.vishwajitrajput.musetraceai.core.ui.SfGhostButton
import com.vishwajitrajput.musetraceai.domain.SettingsRepository
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.DrawingSessionLifecycle
import com.vishwajitrajput.musetraceai.domain.model.OverlayControllerState
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.usecase.SaveOverlayStateUseCase
import com.vishwajitrajput.musetraceai.domain.usecase.SaveProjectUseCase
import com.vishwajitrajput.musetraceai.service.accessibility.AccessibilityBridge
import com.vishwajitrajput.musetraceai.service.session.DrawingSessionRunner
import com.vishwajitrajput.musetraceai.service.session.DrawingSessionState
import com.vishwajitrajput.musetraceai.service.session.DrawingSessionStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

@AndroidEntryPoint
class OverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Inject
    lateinit var overlayPositionStore: OverlayPositionStore

    @Inject
    lateinit var saveOverlayStateUseCase: SaveOverlayStateUseCase

    @Inject
    lateinit var saveProjectUseCase: SaveProjectUseCase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var windowManager: WindowManager? = null
    private var overlayView: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var hasSavedPosition = false
    private var savedX = 0
    private var savedY = 0
    private var collapsed = false
    private var opacity = DEFAULT_OPACITY
    private var overlaySize = DEFAULT_SIZE
    private var persistJob: Job? = null
    private var sessionPersistJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        DrawingSessionStore.initialize(this)
        scope.launch {
            overlayPositionStore.state.collectLatest { stored ->
                hasSavedPosition = stored.hasPosition
                savedX = stored.x
                savedY = stored.y
                collapsed = stored.collapsed
                opacity = stored.opacity.coerceIn(MIN_OPACITY, MAX_OPACITY)
                overlaySize = stored.sizeScale.coerceIn(MIN_SIZE, MAX_SIZE)
                renderOverlay(DrawingSessionStore.state.value)
            }
        }
        scope.launch {
            DrawingSessionStore.state.collectLatest { state ->
                renderOverlay(state)
                persistSessionSnapshot(state)
            }
        }
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                DrawingSessionStore.setCrashRecoveryEnabled(settings.crashRecoveryEnabled)
                DrawingSessionStore.setRuntimeSettings(settings)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        if (Settings.canDrawOverlays(this)) {
            renderOverlay(DrawingSessionStore.state.value)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        overlayView?.let { view -> runCatching { windowManager?.removeView(view) } }
        overlayView = null
        scope.cancel()
        super.onDestroy()
    }

    private fun renderOverlay(state: DrawingSessionState) {
        if (!Settings.canDrawOverlays(this)) return
        val manager = windowManager ?: (getSystemService(WINDOW_SERVICE) as WindowManager).also { windowManager = it }
        val oldView = overlayView
        val oldParams = params
        if (oldView != null && oldParams != null) {
            runCatching { manager.removeView(oldView) }
        }
        val view = if (collapsed) collapsedView(state) else expandedView(state)
        val nextParams = oldParams ?: createLayoutParams()
        nextParams.alpha = opacity
        view.setOnTouchListener(DragTouchListener(nextParams, onMoved = {
            manager.updateViewLayout(view, nextParams)
            savePosition(nextParams)
            updateCanvasWarning(view, state.calibration)
        }))
        manager.addView(view, nextParams)
        overlayView = view
        params = nextParams
        view.post {
            if (!hasSavedPosition) {
                placeOutsideDrawingArea(view, nextParams, state.calibration)
                manager.updateViewLayout(view, nextParams)
                savePosition(nextParams)
            }
            updateCanvasWarning(view, state.calibration)
        }
    }

    private fun expandedView(state: DrawingSessionState): LinearLayout {
        val container = glassPanel(LinearLayout.VERTICAL, width = dp(314))
        container.addView(headerRow(state, expanded = true))
        if (state.finished) {
            container.addView(completionPanel())
            container.addView(label(state.status, secondary = true, topMargin = dp(8)))
            container.addView(progressRow(state))
            container.addView(completionControls())
        } else if (state.resumeDecisionRequired) {
            container.addView(resumeWarningPanel())
            container.addView(colorPreview(state))
            container.addView(progressRow(state))
            container.addView(resumeDecisionControls())
        } else {
            container.addView(colorPreview(state))
            container.addView(label("Select this color manually in Instagram Draw, then tap Continue.", secondary = false, topMargin = dp(10)))
            container.addView(label(state.status, secondary = true, topMargin = dp(8)))
            container.addView(progressRow(state))
            container.addView(warningLabel())
            if (state.canReorderLayers) {
                container.addView(reorderControls())
            }
            container.addView(primaryControls())
            container.addView(secondaryControls())
            container.addView(emergencyControls())
        }
        return container
    }

    private fun collapsedView(state: DrawingSessionState): LinearLayout {
        val layer = state.currentLayer
        val container = glassPanel(LinearLayout.HORIZONTAL, width = WindowManager.LayoutParams.WRAP_CONTENT)
        container.gravity = Gravity.CENTER_VERTICAL
        container.addView(colorDot(layer))
        val compactText = when {
            state.finished -> "Done 100%"
            state.resumeDecisionRequired -> "Resume?"
            else -> "C${state.colorNumber} ${state.progressPercent}%"
        }
        container.addView(label(compactText, secondary = false).apply {
            setPadding(dp(8), 0, dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        })
        container.addView(warningLabel().apply {
            setPadding(0, 0, dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        })
        val expandButton = ghostButton("Expand") {
            collapsed = false
            persistOverlayState()
            renderOverlay(DrawingSessionStore.state.value)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        }
        container.addView(expandButton)
        return container
    }

    private fun headerRow(state: DrawingSessionState, expanded: Boolean): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            label(
                if (state.finished) {
                    "Drawing complete"
                } else if (state.resumeDecisionRequired) {
                    "Resume decision"
                } else {
                    "Color ${state.colorNumber} of ${state.layerCount.coerceAtLeast(1)}"
                },
                secondary = false,
            ),
            LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f),
        )
        row.addView(ghostButton(if (expanded) "Collapse" else "Expand") {
            collapsed = !collapsed
            persistOverlayState()
            renderOverlay(DrawingSessionStore.state.value)
        })
        row.addView(ghostButton("+") { adjustOpacity(OPACITY_STEP) })
        row.addView(ghostButton("-") { adjustOpacity(-OPACITY_STEP) })
        return row
    }

    private fun colorPreview(state: DrawingSessionState): LinearLayout {
        val layer = state.currentLayer
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = roundedBackground(R.color.mt_card_alt, R.color.mt_border, dp(16).toFloat())
        }
        row.addView(colorBlock(layer, size = dp(78)))
        row.addView(
            label(
                if (layer == null) {
                    "No active layer\nOpen a sketch and drawing session first."
                } else {
                    "${layer.title}\n${layer.colorHex} - RGB(${layer.red}, ${layer.green}, ${layer.blue})\n${formatDuration(layer.estimatedDrawingTimeMs)} - ${layer.strokes.size} strokes"
                },
                secondary = false,
            ).apply { setPadding(dp(12), 0, 0, 0) },
            LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f),
        )
        return row.withTopMargin(dp(10))
    }

    private fun progressRow(state: DrawingSessionState): LinearLayout {
        val layer = state.currentLayer
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(
            label(
                "Layer ${state.progressPercent}% - workflow ${state.workflowProgressPercent}% - ${state.completedOrSkippedLayerCount}/${state.layerCount} layers done - time ${formatDuration(layer?.estimatedDrawingTimeMs ?: 0L)} - ${layer?.strokes?.size ?: 0} strokes",
                secondary = true,
                topMargin = dp(8),
            ),
        )
        column.addView(
            ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = state.progressPercent
                progressTintList = android.content.res.ColorStateList.valueOf(color(R.color.mt_primary))
                progressBackgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.mt_border))
            },
            LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dp(8)).apply {
                topMargin = dp(8)
            },
        )
        return column
    }

    private fun completionPanel(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedBackground(R.color.mt_card_alt, R.color.mt_success, dp(16).toFloat())
        }
        panel.addView(label("Drawing complete", secondary = false))
        panel.addView(label("Manually send or save in Instagram. MuseTrace AI will not send anything automatically.", secondary = true, topMargin = dp(6)))
        return panel.withTopMargin(dp(10))
    }

    private fun resumeWarningPanel(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedBackground(R.color.mt_warning_container, R.color.mt_warning, dp(16).toFloat())
        }
        panel.addView(label("Instagram Draw may have cleared your canvas", secondary = false))
        panel.addView(label(DrawingSessionStore.INTERRUPTED_CANVAS_WARNING, secondary = true, topMargin = dp(6)))
        panel.addView(label(DrawingSessionStore.RESUME_EXPLANATION, secondary = true, topMargin = dp(8)))
        return panel.withTopMargin(dp(10))
    }

    private fun primaryControls(): LinearLayout {
        val row = buttonRow()
        row.addView(primaryButton("Continue") {
            val current = DrawingSessionStore.state.value
            when {
                current.finished -> DrawingSessionStore.markStopped("Drawing complete. Manually send or save in Instagram.")
                current.paused -> {
                    AccessibilityBridge.resumeDrawing()
                    DrawingSessionStore.resume()
                }
                current.running -> DrawingSessionStore.markRunning("Drawing is already running. Keep Instagram Draw open.")
                else -> DrawingSessionRunner.drawCurrentLayer()
            }
        })
        row.addView(ghostButton("Skip Layer") {
            AccessibilityBridge.cancelDrawing()
            DrawingSessionStore.skipCurrentLayer()
        })
        return row.withTopMargin(dp(10))
    }

    private fun reorderControls(): LinearLayout {
        val row = buttonRow()
        row.addView(ghostButton("Move Up") { DrawingSessionStore.moveCurrentLayer(-1) })
        row.addView(ghostButton("Move Down") { DrawingSessionStore.moveCurrentLayer(1) })
        row.addView(label("Reorder before Continue", secondary = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        return row.withTopMargin(dp(10))
    }

    private fun secondaryControls(): LinearLayout {
        val row = buttonRow()
        row.addView(ghostButton("Redraw Layer") {
            AccessibilityBridge.cancelDrawing()
            DrawingSessionStore.resetCurrentLayerForRedraw()
        })
        row.addView(ghostButton("Pause") {
            AccessibilityBridge.pauseDrawing()
            DrawingSessionStore.pause()
        })
        row.addView(ghostButton("Resume") {
            AccessibilityBridge.resumeDrawing()
            DrawingSessionStore.resume()
        })
        return row.withTopMargin(dp(8))
    }

    private fun completionControls(): LinearLayout {
        val row = buttonRow()
        row.addView(primaryButton("Close Overlay") { stopSelf() })
        row.addView(ghostButton("Collapse") {
            collapsed = true
            persistOverlayState()
            renderOverlay(DrawingSessionStore.state.value)
        })
        return row.withTopMargin(dp(10))
    }

    private fun resumeDecisionControls(): LinearLayout {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        column.addView(fullWidthButton("Restart from Layer 1") {
            AccessibilityBridge.cancelDrawing()
            DrawingSessionStore.restartFromLayer1()
        })
        column.addView(fullWidthGhostButton("Continue from selected layer") {
            val current = DrawingSessionStore.state.value
            DrawingSessionStore.confirmCanvasResume()
            if (current.running || current.paused) {
                AccessibilityBridge.resumeDrawing()
            } else {
                DrawingSessionRunner.drawCurrentLayer()
            }
        })
        column.addView(fullWidthGhostButton("Recalibrate") {
            AccessibilityBridge.cancelDrawing()
            DrawingSessionStore.requestRecalibration()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
        column.addView(fullWidthDangerButton("Cancel session") {
            AccessibilityBridge.cancelDrawing()
            DrawingSessionStore.cancelSession()
        })
        return column.withTopMargin(dp(10))
    }

    private fun emergencyControls(): LinearLayout {
        val row = buttonRow()
        row.addView(ghostButton("Cancel") {
            AccessibilityBridge.cancelDrawing()
            DrawingSessionStore.cancelSession()
        })
        row.addView(
            SfDangerButton(this).apply {
                text = "Emergency Stop"
                setOnClickListener {
                    AccessibilityBridge.emergencyStop()
                    DrawingSessionStore.markStopped("Emergency stop. No more gestures will be started.")
                    stopSelf()
                }
            },
            LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(8)
            },
        )
        return row.withTopMargin(dp(8))
    }

    private fun warningLabel(): TextView =
        label("", secondary = true, topMargin = dp(8)).apply {
            tag = WARNING_TAG
            visibility = View.GONE
            setTextColor(color(R.color.mt_warning))
        }

    private fun updateCanvasWarning(view: View, calibration: CalibrationProfile) {
        val warning = findWarningView(view) ?: return
        if (!calibration.isUsable()) {
            warning.visibility = View.VISIBLE
            warning.text = if (collapsed) "Calibrate" else "Calibration is missing; draw gestures are blocked."
            return
        }
        val overlap = overlapsDrawingArea(view, calibration)
        warning.visibility = if (overlap) View.VISIBLE else View.GONE
        warning.text = if (overlap) {
            if (collapsed) "Move" else "Overlay is covering the calibrated drawing area. Drag it away before continuing."
        } else {
            ""
        }
    }

    private fun overlapsDrawingArea(view: View, calibration: CalibrationProfile): Boolean {
        val current = params ?: return false
        val bounds = calibration.metrics().bounds
        val overlay = Rect(current.x, current.y, current.x + view.width, current.y + view.height)
        val drawing = Rect(
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.right.roundToInt(),
            bounds.bottom.roundToInt(),
        )
        return Rect.intersects(overlay, drawing)
    }

    private fun placeOutsideDrawingArea(view: View, params: WindowManager.LayoutParams, calibration: CalibrationProfile) {
        val screen = screenBounds()
        val margin = dp(14)
        val viewWidth = max(view.width, dp(if (collapsed) 168 else 314))
        val viewHeight = max(view.height, dp(if (collapsed) 56 else 420))
        if (calibration.isUsable()) {
            val zone = calibration.metrics().overlaySafeZones
                .filter {
                    it.width >= (viewWidth + margin).toFloat() &&
                        it.height >= (viewHeight + margin).toFloat()
                }
                .maxByOrNull { it.width * it.height }
            if (zone != null) {
                params.x = (zone.left + margin).roundToInt().coerceIn(0, (screen.width() - viewWidth).coerceAtLeast(0))
                params.y = (zone.top + margin).roundToInt().coerceIn(0, (screen.height() - viewHeight).coerceAtLeast(0))
                return
            }
        }
        params.x = (screen.width() - viewWidth - margin).coerceAtLeast(0)
        params.y = (screen.height() * DEFAULT_VERTICAL_FRACTION).roundToInt().coerceIn(0, (screen.height() - viewHeight).coerceAtLeast(0))
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val saved = hasSavedPosition
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            alpha = opacity
            x = if (saved) savedX else 0
            y = if (saved) savedY else 0
        }
    }

    private fun savePosition(params: WindowManager.LayoutParams) {
        hasSavedPosition = true
        savedX = params.x
        savedY = params.y
        persistOverlayState(params)
    }

    private fun adjustOpacity(delta: Float) {
        opacity = (opacity + delta).coerceIn(MIN_OPACITY, MAX_OPACITY)
        persistOverlayState()
        params?.let { layoutParams ->
            layoutParams.alpha = opacity
            overlayView?.let { windowManager?.updateViewLayout(it, layoutParams) }
        }
    }

    private fun persistOverlayState(layoutParams: WindowManager.LayoutParams? = params) {
        layoutParams?.let {
            hasSavedPosition = true
            savedX = it.x
            savedY = it.y
        }
        val state = OverlayControllerState(
            hasPosition = hasSavedPosition,
            x = savedX,
            y = savedY,
            collapsed = collapsed,
            opacity = opacity,
            sizeScale = overlaySize,
            updatedAtMillis = System.currentTimeMillis(),
        )
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(OVERLAY_SAVE_DEBOUNCE_MS)
            overlayPositionStore.save(state)
            DrawingSessionStore.state.value.project?.id?.takeIf { it > 0L }?.let { projectId ->
                saveOverlayStateUseCase(projectId, state)
            }
        }
    }

    private fun persistSessionSnapshot(state: DrawingSessionState) {
        val project = state.project ?: return
        if (project.id <= 0L) return
        sessionPersistJob?.cancel()
        sessionPersistJob = scope.launch {
            delay(SESSION_SAVE_DEBOUNCE_MS)
            if (!settingsRepository.settings.first().crashRecoveryEnabled) return@launch
            saveProjectUseCase(
                project.copy(
                    calibrationProfile = state.calibration,
                    workflowProgress = state.toWorkflowProgress(),
                    updatedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun DrawingSessionState.toWorkflowProgress(): com.vishwajitrajput.musetraceai.domain.model.WorkflowProgress {
        val lifecycle = when {
            finished -> DrawingSessionLifecycle.Completed
            resumeDecisionRequired -> DrawingSessionLifecycle.Interrupted
            paused -> DrawingSessionLifecycle.Paused
            running -> DrawingSessionLifecycle.Running
            sessionStarted -> DrawingSessionLifecycle.Ready
            else -> DrawingSessionLifecycle.NotStarted
        }
        return com.vishwajitrajput.musetraceai.domain.model.WorkflowProgress(
            currentLayerIndex = layerIndex,
            completedStrokes = completedStrokes,
            totalStrokes = totalStrokes,
            completedLayerIndexes = completedLayerIndexes,
            skippedLayerIndexes = skippedLayerIndexes,
            sessionState = lifecycle,
            resumeDecisionRequired = resumeDecisionRequired,
            resumeWarning = resumeWarning,
            status = status,
            autosavedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun glassPanel(orientationValue: Int, width: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = orientationValue
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedBackground(R.color.mt_scrim, R.color.mt_border, dp(22).toFloat())
            elevation = dp(8).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                if (width > 0) scaledPx(width) else width,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        }

    private fun buttonRow(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun primaryButton(textValue: String, onClick: () -> Unit): SfButton =
        SfButton(this).apply {
            text = textValue
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            }
        }

    private fun fullWidthButton(textValue: String, onClick: () -> Unit): SfButton =
        SfButton(this).apply {
            text = textValue
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
        }

    private fun ghostButton(textValue: String, onClick: () -> Unit): SfGhostButton =
        SfGhostButton(this).apply {
            text = textValue
            minWidth = 0
            setPadding(dp(10), 0, dp(10), 0)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
        }

    private fun fullWidthGhostButton(textValue: String, onClick: () -> Unit): SfGhostButton =
        SfGhostButton(this).apply {
            text = textValue
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
        }

    private fun fullWidthDangerButton(textValue: String, onClick: () -> Unit): SfDangerButton =
        SfDangerButton(this).apply {
            text = textValue
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
        }

    private fun label(textValue: String, secondary: Boolean, topMargin: Int = 0): TextView =
        TextView(this).apply {
            text = textValue
            setTextColor(color(if (secondary) R.color.mt_text_secondary else R.color.mt_text))
            textSize = if (secondary) 12f else 14f
            if (!secondary) setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                topMargin.takeIf { it > 0 }?.let { this.topMargin = it }
            }
        }

    private fun colorDot(layer: TraceLayer?, size: Int = dp(24)): View =
        View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(runCatching { Color.parseColor(layer?.colorHex ?: "#FFFFFF") }.getOrDefault(Color.WHITE))
                setStroke(dp(1), color(R.color.mt_border_strong))
            }
            layoutParams = LinearLayout.LayoutParams(size, size)
        }

    private fun colorBlock(layer: TraceLayer?, size: Int): View =
        View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(runCatching { Color.parseColor(layer?.colorHex ?: "#FFFFFF") }.getOrDefault(Color.WHITE))
                setStroke(dp(1), color(R.color.mt_border_strong))
            }
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(size, size)
        }

    private fun roundedBackground(fillColorRes: Int, strokeColorRes: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color(fillColorRes))
            setStroke(dp(1), color(strokeColorRes))
        }

    private fun LinearLayout.withTopMargin(margin: Int): LinearLayout =
        apply {
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                topMargin = margin
            }
        }

    private fun TextView.withTopMargin(margin: Int): TextView =
        apply {
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                topMargin = margin
            }
        }

    private fun findWarningView(view: View): TextView? {
        if (view is TextView && view.tag == WARNING_TAG) return view
        if (view is android.view.ViewGroup) {
            for (index in 0 until view.childCount) {
                findWarningView(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }

    private fun screenBounds(): Rect {
        val manager = windowManager ?: getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.currentWindowMetrics.bounds
        } else {
            val metrics = resources.displayMetrics
            Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000L).coerceAtLeast(1L)
        val minutes = seconds / 60L
        val remainder = seconds % 60L
        return if (minutes > 0) "${minutes}m ${remainder}s" else "${remainder}s"
    }

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("MuseTrace overlay")
        .setContentText("Floating controller stays on top for user-confirmed drawing.")
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                20,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .build()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "MuseTrace overlay", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun color(colorRes: Int): Int = androidx.core.content.ContextCompat.getColor(this, colorRes)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun scaledPx(value: Int): Int = (value * overlaySize).roundToInt().coerceAtLeast(1)

    private class DragTouchListener(
        private val params: WindowManager.LayoutParams,
        private val onMoved: () -> Unit,
    ) : View.OnTouchListener {
        private var downX = 0
        private var downY = 0
        private var touchX = 0f
        private var touchY = 0f
        private var dragging = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = params.x
                    downY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    dragging = dragging || kotlin.math.abs(dx) > view.touchSlop() || kotlin.math.abs(dy) > view.touchSlop()
                    if (dragging) {
                        params.x = downX + dx.toInt()
                        params.y = downY + dy.toInt()
                        onMoved()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) view.performClick()
                    return dragging
                }
            }
            return false
        }

        private fun View.touchSlop(): Int = android.view.ViewConfiguration.get(context).scaledTouchSlop
    }

    companion object {
        private const val CHANNEL_ID = "musetrace_overlay"
        private const val NOTIFICATION_ID = 2102
        private const val WARNING_TAG = "overlay_warning"
        private const val DEFAULT_OPACITY = 0.94f
        private const val MIN_OPACITY = 0.48f
        private const val MAX_OPACITY = 1f
        private const val DEFAULT_SIZE = 1f
        private const val MIN_SIZE = 0.8f
        private const val MAX_SIZE = 1.35f
        private const val OPACITY_STEP = 0.08f
        private const val DEFAULT_VERTICAL_FRACTION = 0.18f
        private const val OVERLAY_SAVE_DEBOUNCE_MS = 120L
        private const val SESSION_SAVE_DEBOUNCE_MS = 250L

        fun startIntent(context: Context): Intent = Intent(context, OverlayService::class.java)
    }
}
