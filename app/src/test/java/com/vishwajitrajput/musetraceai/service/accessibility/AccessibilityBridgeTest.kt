package com.vishwajitrajput.musetraceai.service.accessibility

import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfileType
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.TraceStroke
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AccessibilityBridgeTest {
    @Test
    fun drawStrokesFailsClosedWhenAccessibilityServiceIsNotConnected() = runTest {
        val result = AccessibilityBridge.drawStrokes(
            project = project(),
            calibration = calibration(),
            strokes = listOf(TraceStroke(listOf(TracePoint(0f, 0f), TracePoint(10f, 10f)), 100L)),
            startIndex = 0,
            callbacks = object : GestureQueueCallbacks {
                override fun onStarted(completed: Int, total: Int) = Unit
                override fun onProgress(completed: Int, total: Int) = Unit
                override fun onPaused(completed: Int, total: Int, message: String) = Unit
            },
        )

        assertFalse(AccessibilityBridge.isConnected())
        assertEquals(GestureQueueStatus.Failed, result.status)
        assertEquals("Enable MuseTrace AI AccessibilityService first.", result.message)
    }

    private fun project(): TraceProject =
        TraceProject(
            id = 1L,
            title = "Gesture test",
            sourceUri = "file:///source.png",
            previewPath = "file:///preview.png",
            colorCount = 16,
            width = 100,
            height = 100,
            layers = listOf(
                TraceLayer(
                    index = 0,
                    title = "Layer 1",
                    colorHex = "#000000",
                    strokes = emptyList(),
                ),
            ),
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
