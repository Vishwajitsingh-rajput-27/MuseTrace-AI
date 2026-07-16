package com.vishwajitrajput.musetraceai.service.session

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfileType
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.TraceStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DrawingSessionStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        DrawingSessionStore.resetForTests(context)
    }

    @Test
    fun crashRecoveryRestoresInterruptedSessionWithResumeWarning() {
        val project = project()
        DrawingSessionStore.load(project, calibration())
        DrawingSessionStore.prepareOverlaySession()
        DrawingSessionStore.markInterrupted(completed = 1, total = 1)

        DrawingSessionStore.load(project, calibration())

        val restored = DrawingSessionStore.state.value
        assertTrue(restored.resumeDecisionRequired)
        assertTrue(restored.status.contains("cannot restore a drawing", ignoreCase = true))
        assertEquals(1, restored.completedStrokes)
        assertEquals(1, restored.totalStrokes)
    }

    @Test
    fun skippedLastLayerWrapsToEarlierPendingLayerInsteadOfFinishingEarly() {
        DrawingSessionStore.load(project(layerCount = 3), calibration())
        DrawingSessionStore.selectLayer(2)

        DrawingSessionStore.skipCurrentLayer()

        val state = DrawingSessionStore.state.value
        assertFalse(state.finished)
        assertEquals(0, state.layerIndex)
        assertEquals(setOf(2), state.skippedLayerIndexes)
    }

    private fun project(layerCount: Int = 2): TraceProject =
        TraceProject(
            id = 42L,
            title = "Session test",
            sourceUri = "file:///source.png",
            previewPath = "file:///preview.png",
            colorCount = 16,
            width = 100,
            height = 100,
            layers = List(layerCount) { index ->
                TraceLayer(
                    index = index,
                    title = "Layer ${index + 1}",
                    colorHex = if (index == 0) "#000000" else "#FFFFFF",
                    strokes = listOf(
                        TraceStroke(
                            points = listOf(TracePoint(0f, 0f), TracePoint(10f, 10f)),
                            durationMs = 100L,
                        ),
                    ),
                )
            },
            createdAtMillis = 1L,
        )

    private fun calibration(): CalibrationProfile =
        CalibrationProfile.fromBounds(
            type = CalibrationProfileType.NormalDraw,
            left = 0,
            top = 0,
            width = 240,
            height = 360,
            screenWidth = 240,
            screenHeight = 420,
            savedAtMillis = 1L,
        )
}
