package com.vishwajitrajput.musetraceai.data.local

import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfileType
import com.vishwajitrajput.musetraceai.domain.model.DrawingSessionLifecycle
import com.vishwajitrajput.musetraceai.domain.model.OverlayControllerState
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.TraceStroke
import com.vishwajitrajput.musetraceai.domain.model.WorkflowProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TraceJsonCodecTest {
    @Test
    fun projectJsonRoundTripPreservesWorkflowAndLayerMetadata() {
        val project = TraceProject(
            id = 7L,
            title = "Round trip",
            sourceUri = "file:///source.png",
            previewPath = "file:///preview.png",
            colorCount = 16,
            width = 100,
            height = 100,
            layers = listOf(
                TraceLayer(
                    index = 0,
                    title = "Layer 1 - Skin light",
                    colorHex = "#EEC29A",
                    strokes = listOf(TraceStroke(listOf(TracePoint(1f, 1f), TracePoint(8f, 8f)), 120L)),
                    red = 238,
                    green = 194,
                    blue = 154,
                    colorName = "Skin light",
                    qualityWarnings = listOf("Check visibility before drawing."),
                    gestureCount = 1,
                ),
            ),
            createdAtMillis = 10L,
            calibrationProfile = CalibrationProfile.fromBounds(
                type = CalibrationProfileType.NormalDraw,
                left = 0,
                top = 0,
                width = 240,
                height = 360,
                screenWidth = 240,
                screenHeight = 420,
                savedAtMillis = 20L,
            ),
            overlayState = OverlayControllerState(hasPosition = true, x = 11, y = 22, collapsed = true),
            workflowProgress = WorkflowProgress(
                currentLayerIndex = 0,
                completedStrokes = 1,
                totalStrokes = 3,
                sessionState = DrawingSessionLifecycle.Interrupted,
                resumeDecisionRequired = true,
                resumeWarning = "Canvas may be cleared.",
                status = "Paused for resume decision.",
                autosavedAtMillis = 30L,
            ),
            updatedAtMillis = 40L,
        )

        val decoded = TraceJsonCodec.decodeProject(TraceJsonCodec.encodeProject(project))

        assertEquals(project.title, decoded.title)
        assertEquals(project.layers.single().colorHex, decoded.layers.single().colorHex)
        assertEquals(project.layers.single().qualityWarnings, decoded.layers.single().qualityWarnings)
        assertEquals(DrawingSessionLifecycle.Interrupted, decoded.workflowProgress.sessionState)
        assertTrue(decoded.workflowProgress.resumeDecisionRequired)
        assertEquals(11, decoded.overlayState.x)
    }
}
