package com.vishwajitrajput.musetraceai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationProfileTest {
    @Test
    fun mapsProjectCoordinatesIntoFourPointCanvas() {
        val profile = CalibrationProfile(
            type = CalibrationProfileType.Custom,
            topLeft = TracePoint(10f, 20f),
            topRight = TracePoint(210f, 20f),
            bottomLeft = TracePoint(10f, 420f),
            bottomRight = TracePoint(210f, 420f),
            screenWidth = 240,
            screenHeight = 480,
            savedAtMillis = 1L,
        )

        val center = profile.mapPoint(
            projectWidth = 100,
            projectHeight = 200,
            point = TracePoint(50f, 100f),
            useSafeArea = false,
        )

        assertTrue(profile.isUsable())
        assertEquals(110f, center.x, 0.001f)
        assertEquals(220f, center.y, 0.001f)
        assertEquals(200f, profile.metrics().scaleX, 0.001f)
        assertEquals(400f, profile.metrics().scaleY, 0.001f)
    }

    @Test
    fun safeAreaMappingStaysInsideCalibratedBounds() {
        val profile = CalibrationProfile.fromBounds(
            type = CalibrationProfileType.NormalDraw,
            left = 0,
            top = 0,
            width = 300,
            height = 600,
            screenWidth = 360,
            screenHeight = 720,
            savedAtMillis = 1L,
        )

        val topLeftSafe = profile.mapPoint(100, 100, TracePoint(0f, 0f), useSafeArea = true)
        val bottomRightSafe = profile.mapPoint(100, 100, TracePoint(100f, 100f), useSafeArea = true)

        assertTrue(topLeftSafe.x > profile.topLeft.x)
        assertTrue(topLeftSafe.y > profile.topLeft.y)
        assertTrue(bottomRightSafe.x < profile.bottomRight.x)
        assertTrue(bottomRightSafe.y < profile.bottomRight.y)
    }
}
