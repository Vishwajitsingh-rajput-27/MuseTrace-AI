package com.vishwajitrajput.musetraceai.data.repository

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.vishwajitrajput.musetraceai.core.storage.ImageFileStore
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import com.vishwajitrajput.musetraceai.domain.model.TraceStroke
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

internal data class VectorStroke(
    val points: List<TracePoint>,
    val kind: StrokeKind,
)

internal enum class StrokeKind {
    Contour,
    Skeleton,
    Fill,
}

internal data class GestureBatch(
    val strokes: List<VectorStroke>,
    val estimatedMs: Long,
)

internal data class StrokeQuality(
    val gestureCount: Int,
    val strokeCount: Int,
    val estimatedTimeMs: Long,
    val qualityScore: Int,
    val difficultyScore: Int,
    val warnings: List<String>,
)

internal object StrokeExtractor {
    fun extract(maskBitmap: Bitmap): List<VectorStroke> {
        val original = Mask.fromBitmap(maskBitmap)
        val closed = original.closeGaps()
        val cleaned = closed.removeSmallArtifacts()
        val contourStrokes = cleaned.contourPaths()
        val fillStrokes = cleaned.fillRuns()
        val skeletonStrokes = if (cleaned.coveragePercent() > 4f) {
            cleaned.skeletonize().skeletonRuns()
        } else {
            emptyList()
        }
        return contourStrokes + skeletonStrokes + fillStrokes
    }
}

internal object StrokeSimplifier {
    fun simplify(strokes: List<VectorStroke>, epsilon: Float = 2.1f): List<VectorStroke> =
        strokes.mapNotNull { stroke ->
            val simplified = douglasPeucker(stroke.points, epsilon)
            if (simplified.size >= 2) stroke.copy(points = simplified) else null
        }

    fun splitLongPaths(strokes: List<VectorStroke>, maxPoints: Int = 74, maxDistance: Float = 260f): List<VectorStroke> {
        val split = mutableListOf<VectorStroke>()
        strokes.forEach { stroke ->
            var chunk = mutableListOf<TracePoint>()
            var distance = 0f
            stroke.points.forEachIndexed { index, point ->
                if (chunk.isNotEmpty()) distance += chunk.last().distanceTo(point)
                chunk.add(point)
                val shouldSplit = chunk.size >= maxPoints || distance >= maxDistance
                if (shouldSplit && index < stroke.points.lastIndex) {
                    split.add(stroke.copy(points = chunk.toList()))
                    chunk = mutableListOf(point)
                    distance = 0f
                }
            }
            if (chunk.size >= 2) split.add(stroke.copy(points = chunk.toList()))
        }
        return split
    }

    fun mergeCloseStrokes(strokes: List<VectorStroke>, maxGap: Float = 10f): List<VectorStroke> {
        val remaining = strokes.toMutableList()
        val merged = mutableListOf<VectorStroke>()
        while (remaining.isNotEmpty()) {
            var current = remaining.removeAt(0)
            var changed: Boolean
            do {
                changed = false
                val end = current.points.lastOrNull()
                if (end != null) {
                    val index = remaining.indexOfFirst { other ->
                        val start = other.points.firstOrNull()
                        val otherEnd = other.points.lastOrNull()
                        (start != null && end.distanceTo(start) <= maxGap) ||
                            (otherEnd != null && end.distanceTo(otherEnd) <= maxGap)
                    }
                    if (index >= 0 && current.points.size + remaining[index].points.size <= 96) {
                        val next = remaining.removeAt(index)
                        val nextPoints = if (end.distanceTo(next.points.first()) <= maxGap) {
                            next.points
                        } else {
                            next.points.asReversed()
                        }
                        current = current.copy(points = current.points + nextPoints.drop(1))
                        changed = true
                    }
                }
            } while (changed)
            merged.add(current)
        }
        return merged
    }

    private fun douglasPeucker(points: List<TracePoint>, epsilon: Float): List<TracePoint> {
        if (points.size < 3) return points
        var maxDistance = 0f
        var index = 0
        val first = points.first()
        val last = points.last()
        for (i in 1 until points.lastIndex) {
            val distance = perpendicularDistance(points[i], first, last)
            if (distance > maxDistance) {
                maxDistance = distance
                index = i
            }
        }
        return if (maxDistance > epsilon) {
            val left = douglasPeucker(points.subList(0, index + 1), epsilon)
            val right = douglasPeucker(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(point: TracePoint, start: TracePoint, end: TracePoint): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        if (dx == 0f && dy == 0f) return point.distanceTo(start)
        val numerator = abs(dy * point.x - dx * point.y + end.x * start.y - end.y * start.x)
        return numerator / hypot(dx, dy)
    }
}

internal object BezierSmoother {
    fun smooth(strokes: List<VectorStroke>): List<VectorStroke> =
        strokes.map { stroke ->
            if (stroke.points.size < 4 || stroke.kind == StrokeKind.Fill) {
                stroke
            } else {
                stroke.copy(points = chaikin(stroke.points))
            }
        }

    private fun chaikin(points: List<TracePoint>): List<TracePoint> {
        val output = mutableListOf(points.first())
        for (i in 0 until points.lastIndex) {
            val a = points[i]
            val b = points[i + 1]
            output.add(TracePoint(a.x * 0.75f + b.x * 0.25f, a.y * 0.75f + b.y * 0.25f))
            output.add(TracePoint(a.x * 0.25f + b.x * 0.75f, a.y * 0.25f + b.y * 0.75f))
        }
        output.add(points.last())
        return output
    }
}

internal object TravelOptimizer {
    fun optimize(strokes: List<VectorStroke>): List<VectorStroke> {
        val remaining = strokes.toMutableList()
        val optimized = mutableListOf<VectorStroke>()
        var cursor = TracePoint(0f, 0f)
        while (remaining.isNotEmpty()) {
            var bestIndex = 0
            var reverse = false
            var bestDistance = Float.MAX_VALUE
            remaining.forEachIndexed { index, stroke ->
                val start = stroke.points.first()
                val end = stroke.points.last()
                val startDistance = cursor.distanceTo(start)
                val endDistance = cursor.distanceTo(end)
                if (startDistance < bestDistance) {
                    bestDistance = startDistance
                    bestIndex = index
                    reverse = false
                }
                if (endDistance < bestDistance) {
                    bestDistance = endDistance
                    bestIndex = index
                    reverse = true
                }
            }
            val next = remaining.removeAt(bestIndex)
            val ordered = if (reverse) next.copy(points = next.points.asReversed()) else next
            optimized.add(ordered)
            cursor = ordered.points.last()
        }
        return optimized
    }
}

internal object GestureBatcher {
    fun batch(strokes: List<VectorStroke>, maxGestures: Int = 18, maxDurationMs: Long = 4_200L): List<GestureBatch> {
        val batches = mutableListOf<GestureBatch>()
        var current = mutableListOf<VectorStroke>()
        var duration = 0L
        strokes.forEach { stroke ->
            val strokeDuration = stroke.estimatedDurationMs()
            if (current.isNotEmpty() && (current.size >= maxGestures || duration + strokeDuration > maxDurationMs)) {
                batches.add(GestureBatch(current.toList(), duration))
                current = mutableListOf()
                duration = 0L
            }
            current.add(stroke)
            duration += strokeDuration
        }
        if (current.isNotEmpty()) batches.add(GestureBatch(current, duration))
        return batches
    }
}

internal object StrokeQualityAnalyzer {
    fun analyze(
        strokes: List<VectorStroke>,
        batches: List<GestureBatch>,
        layerSpec: QuantizedLayerSpec,
    ): StrokeQuality {
        val strokeCount = strokes.size
        val gestureCount = batches.sumOf { it.strokes.size }
        val pathDistance = strokes.sumOf { it.pathDistance().toDouble() }
        val estimatedTime = batches.sumOf { it.estimatedMs } + batches.size * 420L + (pathDistance * 3.2).toLong()
        val contourCount = strokes.count { it.kind == StrokeKind.Contour }
        val skeletonCount = strokes.count { it.kind == StrokeKind.Skeleton }
        val warnings = mutableListOf<String>()
        if (gestureCount > 240) warnings.add("High gesture count; use a slower drawing pace for this layer.")
        if (contourCount > 120) warnings.add("Many contour strokes; details may need careful manual color selection.")
        if (skeletonCount > 90) warnings.add("Dense skeleton strokes; inspect this layer before drawing.")
        if (estimatedTime > 120_000L) warnings.add("Long estimated drawing time; split the layer mentally into sections.")
        if (layerSpec.coveragePercent < 0.7f) warnings.add("Tiny visible region; consider merging if it is not important.")
        val complexityPenalty = gestureCount / 8 + (pathDistance / 180f).toInt() + warnings.size * 7
        val qualityScore = (100 - complexityPenalty).coerceIn(28, 100)
        val difficulty = (1 + gestureCount / 34 + warnings.size + (pathDistance / 2200f).toInt()).coerceIn(1, 10)
        return StrokeQuality(
            gestureCount = gestureCount,
            strokeCount = strokeCount,
            estimatedTimeMs = estimatedTime.coerceAtLeast(5_000L),
            qualityScore = qualityScore,
            difficultyScore = difficulty,
            warnings = warnings,
        )
    }
}

internal object StrokePreviewRenderer {
    fun render(width: Int, height: Int, strokes: List<VectorStroke>, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = 230
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 2.3f
        }
        strokes.forEach { stroke ->
            val first = stroke.points.firstOrNull() ?: return@forEach
            val path = Path().apply { moveTo(first.x, first.y) }
            stroke.points.drop(1).forEach { path.lineTo(it.x, it.y) }
            canvas.drawPath(path, paint)
        }
        return bitmap
    }
}

internal object StrokePlanner {
    fun plan(
        width: Int,
        height: Int,
        layers: List<QuantizedLayerSpec>,
        imageFileStore: ImageFileStore,
    ): List<TraceLayer> =
        layers.mapIndexed { index, layerSpec ->
            val mask = imageFileStore.loadBitmap(layerSpec.maskUri)
            val raw = StrokeExtractor.extract(mask)
            val simplified = StrokeSimplifier.simplify(raw)
            val smoothed = BezierSmoother.smooth(simplified)
            val merged = StrokeSimplifier.mergeCloseStrokes(smoothed)
            val split = StrokeSimplifier.splitLongPaths(merged)
            val optimized = TravelOptimizer.optimize(split)
            val batches = GestureBatcher.batch(optimized)
            val quality = StrokeQualityAnalyzer.analyze(optimized, batches, layerSpec)
            val strokePreviewUri = imageFileStore.saveBitmap(
                "stroke_preview_${index + 1}",
                StrokePreviewRenderer.render(width, height, optimized, layerSpec.color),
            )
            TraceLayer(
                index = index,
                title = "Layer ${index + 1} - ${layerSpec.colorName}",
                colorHex = layerSpec.colorHex,
                strokes = optimized.map { it.toTraceStroke() },
                red = layerSpec.red,
                green = layerSpec.green,
                blue = layerSpec.blue,
                colorName = layerSpec.colorName,
                difficultyScore = max(layerSpec.qualityWarnings.size + 1, quality.difficultyScore),
                estimatedDrawingTimeMs = quality.estimatedTimeMs,
                recommendedOrder = layerSpec.recommendedOrder,
                coveragePercent = layerSpec.coveragePercent,
                pixelCount = layerSpec.pixelCount,
                maskUri = layerSpec.maskUri,
                layerBitmapUri = layerSpec.layerBitmapUri,
                qualityWarnings = (layerSpec.qualityWarnings + quality.warnings).distinct(),
                gestureCount = quality.gestureCount,
                strokeQualityScore = quality.qualityScore,
                strokePreviewUri = strokePreviewUri,
                strokeComplexityWarnings = quality.warnings,
            )
        }
}

private data class Mask(
    val width: Int,
    val height: Int,
    val pixels: BooleanArray,
) {
    fun closeGaps(): Mask = dilate().erode()

    fun removeSmallArtifacts(): Mask {
        val visited = BooleanArray(pixels.size)
        val output = pixels.copyOf()
        val threshold = max(10, (pixels.size * 0.00035f).toInt())
        for (index in pixels.indices) {
            if (!pixels[index] || visited[index]) continue
            val component = collectComponent(index, visited)
            if (component.size < threshold) {
                component.forEach { output[it] = false }
            }
        }
        return copy(pixels = output)
    }

    fun contourPaths(): List<VectorStroke> {
        val boundary = boundaryMask()
        val visited = BooleanArray(boundary.size)
        val paths = mutableListOf<VectorStroke>()
        for (index in boundary.indices) {
            if (!boundary[index] || visited[index]) continue
            val points = traceBoundary(index, boundary, visited)
            if (points.size >= 5) {
                paths.add(VectorStroke(points, StrokeKind.Contour))
            }
        }
        return paths.ifEmpty { contourRuns(boundary) }
    }

    private fun contourRuns(boundary: BooleanArray): List<VectorStroke> {
        val strokes = mutableListOf<VectorStroke>()
        val step = max(2, height / 220)
        for (y in 0 until height step step) {
            addRuns(boundary, y, horizontal = true, kind = StrokeKind.Contour, target = strokes)
        }
        for (x in 0 until width step step) {
            addRuns(boundary, x, horizontal = false, kind = StrokeKind.Contour, target = strokes)
        }
        return strokes
    }

    private fun boundaryMask(): BooleanArray {
        val boundary = BooleanArray(pixels.size)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                boundary[index] = pixels[index] && neighbors8(x, y).any { !pixels[it] }
            }
        }
        return boundary
    }

    private fun traceBoundary(start: Int, boundary: BooleanArray, visited: BooleanArray): List<TracePoint> {
        val points = mutableListOf<TracePoint>()
        var current = start
        var guard = 0
        while (current >= 0 && guard < boundary.size) {
            visited[current] = true
            points.add(TracePoint((current % width).toFloat(), (current / width).toFloat()))
            val next = neighbors8(current % width, current / width)
                .filter { boundary[it] && !visited[it] }
                .minByOrNull { neighbor ->
                    val dx = (neighbor % width) - (current % width)
                    val dy = (neighbor / width) - (current / width)
                    dx * dx + dy * dy
                } ?: -1
            current = next
            guard++
        }
        return points
    }

    fun fillRuns(): List<VectorStroke> {
        val strokes = mutableListOf<VectorStroke>()
        val step = max(3, height / 150)
        for (y in 0 until height step step) {
            addRuns(pixels, y, horizontal = true, kind = StrokeKind.Fill, target = strokes)
        }
        return strokes
    }

    fun skeletonize(): Mask {
        var current = pixels.copyOf()
        var changed: Boolean
        var passes = 0
        do {
            changed = false
            val removeA = mutableListOf<Int>()
            forEachInteriorPixel(current) { x, y, index ->
                if (shouldRemoveSkeletonPixel(current, x, y, firstPass = true)) removeA.add(index)
            }
            if (removeA.isNotEmpty()) {
                removeA.forEach { current[it] = false }
                changed = true
            }
            val removeB = mutableListOf<Int>()
            forEachInteriorPixel(current) { x, y, index ->
                if (shouldRemoveSkeletonPixel(current, x, y, firstPass = false)) removeB.add(index)
            }
            if (removeB.isNotEmpty()) {
                removeB.forEach { current[it] = false }
                changed = true
            }
            passes++
        } while (changed && passes < 42)
        return copy(pixels = current)
    }

    fun skeletonRuns(): List<VectorStroke> {
        val strokes = mutableListOf<VectorStroke>()
        val step = max(2, height / 260)
        for (y in 0 until height step step) {
            addRuns(pixels, y, horizontal = true, kind = StrokeKind.Skeleton, target = strokes)
        }
        for (x in 0 until width step (step * 2)) {
            addRuns(pixels, x, horizontal = false, kind = StrokeKind.Skeleton, target = strokes)
        }
        return strokes
    }

    fun coveragePercent(): Float = pixels.count { it } * 100f / pixels.size.coerceAtLeast(1)

    private fun dilate(): Mask {
        val output = BooleanArray(pixels.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                output[index] = pixels[index] || neighbors8(x, y).any { pixels[it] }
            }
        }
        return copy(pixels = output)
    }

    private fun erode(): Mask {
        val output = BooleanArray(pixels.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                output[index] = pixels[index] && neighbors8(x, y).all { pixels[it] }
            }
        }
        return copy(pixels = output)
    }

    private fun addRuns(
        mask: BooleanArray,
        fixed: Int,
        horizontal: Boolean,
        kind: StrokeKind,
        target: MutableList<VectorStroke>,
    ) {
        val limit = if (horizontal) width else height
        var runStart = -1
        for (cursor in 0 until limit) {
            val x = if (horizontal) cursor else fixed
            val y = if (horizontal) fixed else cursor
            val active = mask.getOrNull(y * width + x) == true
            if (active && runStart < 0) runStart = cursor
            if ((!active || cursor == limit - 1) && runStart >= 0) {
                val runEnd = if (active && cursor == limit - 1) cursor else cursor - 1
                if (runEnd - runStart >= 3) {
                    val start = if (horizontal) TracePoint(runStart.toFloat(), fixed.toFloat()) else TracePoint(fixed.toFloat(), runStart.toFloat())
                    val end = if (horizontal) TracePoint(runEnd.toFloat(), fixed.toFloat()) else TracePoint(fixed.toFloat(), runEnd.toFloat())
                    target.add(VectorStroke(listOf(start, end), kind))
                }
                runStart = -1
            }
        }
    }

    private fun collectComponent(start: Int, visited: BooleanArray): List<Int> {
        val queue = ArrayDeque<Int>()
        val component = mutableListOf<Int>()
        queue.add(start)
        visited[start] = true
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            component.add(index)
            val x = index % width
            val y = index / width
            neighbors8(x, y).forEach { neighbor ->
                if (pixels[neighbor] && !visited[neighbor]) {
                    visited[neighbor] = true
                    queue.add(neighbor)
                }
            }
        }
        return component
    }

    private fun forEachInteriorPixel(mask: BooleanArray, block: (Int, Int, Int) -> Unit) {
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                if (mask[index]) block(x, y, index)
            }
        }
    }

    private fun shouldRemoveSkeletonPixel(mask: BooleanArray, x: Int, y: Int, firstPass: Boolean): Boolean {
        val p2 = mask[(y - 1) * width + x]
        val p3 = mask[(y - 1) * width + x + 1]
        val p4 = mask[y * width + x + 1]
        val p5 = mask[(y + 1) * width + x + 1]
        val p6 = mask[(y + 1) * width + x]
        val p7 = mask[(y + 1) * width + x - 1]
        val p8 = mask[y * width + x - 1]
        val p9 = mask[(y - 1) * width + x - 1]
        val neighbors = listOf(p2, p3, p4, p5, p6, p7, p8, p9)
        val count = neighbors.count { it }
        if (count !in 2..6) return false
        val transitions = neighbors.zipWithNext().count { (a, b) -> !a && b } + if (!p9 && p2) 1 else 0
        if (transitions != 1) return false
        return if (firstPass) {
            !(p2 && p4 && p6) && !(p4 && p6 && p8)
        } else {
            !(p2 && p4 && p8) && !(p2 && p6 && p8)
        }
    }

    private fun neighbors8(x: Int, y: Int): List<Int> {
        val neighbors = ArrayList<Int>(8)
        for (ny in max(0, y - 1)..min(height - 1, y + 1)) {
            for (nx in max(0, x - 1)..min(width - 1, x + 1)) {
                if (nx != x || ny != y) neighbors.add(ny * width + nx)
            }
        }
        return neighbors
    }

    companion object {
        fun fromBitmap(bitmap: Bitmap): Mask {
            val pixels = BooleanArray(bitmap.width * bitmap.height)
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val color = bitmap.getPixel(x, y)
                    pixels[y * bitmap.width + x] = Color.alpha(color) > 32 && Color.red(color) > 120
                }
            }
            return Mask(bitmap.width, bitmap.height, pixels)
        }
    }
}

private fun VectorStroke.toTraceStroke(): TraceStroke =
    TraceStroke(points = points, durationMs = estimatedDurationMs())

private fun VectorStroke.estimatedDurationMs(): Long =
    (pathDistance() * 2.7f).toLong().coerceIn(90L, 1_450L)

private fun VectorStroke.pathDistance(): Float =
    points.zipWithNext().sumOf { (a, b) -> a.distanceTo(b).toDouble() }.toFloat()

private fun TracePoint.distanceTo(other: TracePoint): Float =
    sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
