package com.vishwajitrajput.musetraceai.data.repository

import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeSimplifierTest {
    @Test
    fun simplifyReducesCollinearPathButPreservesEndpoints() {
        val points = (0..20).map { TracePoint(it.toFloat(), 0f) }
        val stroke = VectorStroke(points, StrokeKind.Contour)

        val simplified = StrokeSimplifier.simplify(listOf(stroke), epsilon = 0.25f).single()

        assertEquals(points.first(), simplified.points.first())
        assertEquals(points.last(), simplified.points.last())
        assertTrue(simplified.points.size < points.size)
    }

    @Test
    fun splitLongPathsKeepsDrawableChunks() {
        val points = (0..160).map { TracePoint(it * 2f, 0f) }
        val stroke = VectorStroke(points, StrokeKind.Fill)

        val split = StrokeSimplifier.splitLongPaths(listOf(stroke), maxPoints = 30, maxDistance = 80f)

        assertTrue(split.size > 1)
        assertTrue(split.all { it.points.size >= 2 })
        assertEquals(points.first(), split.first().points.first())
        assertEquals(points.last(), split.last().points.last())
    }
}
