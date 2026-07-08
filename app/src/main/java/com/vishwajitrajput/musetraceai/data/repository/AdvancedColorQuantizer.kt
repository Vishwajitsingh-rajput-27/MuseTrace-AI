package com.vishwajitrajput.musetraceai.data.repository

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.vishwajitrajput.musetraceai.core.storage.ImageFileStore
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal data class QuantizedLayerSpec(
    val color: Int,
    val red: Int,
    val green: Int,
    val blue: Int,
    val colorHex: String,
    val colorName: String,
    val recommendedOrder: Int,
    val coveragePercent: Float,
    val pixelCount: Int,
    val edgePixelCount: Int,
    val importantPixelCount: Int,
    val maskUri: String,
    val layerBitmapUri: String,
    val qualityWarnings: List<String>,
)

internal data class AdvancedQuantizationResult(
    val previewBitmap: Bitmap,
    val layers: List<QuantizedLayerSpec>,
)

internal object AdvancedColorQuantizer {
    fun quantize(
        bitmap: Bitmap,
        requestedColorCount: Int,
        edges: BooleanArray,
        imageFileStore: ImageFileStore,
    ): AdvancedQuantizationResult {
        val targetColorCount = requestedColorCount.coerceIn(16, 32)
        val samples = collectWeightedSamples(bitmap, edges)
        val centers = refineCenters(samples, targetColorCount)
        val initialAssignments = assignPixels(bitmap, centers)
        val activeCenters = activeCentersAfterTinyMerge(initialAssignments, centers, bitmap.width * bitmap.height)
        val finalAssignments = assignPixels(bitmap, activeCenters)
        val preview = renderPreview(bitmap.width, bitmap.height, activeCenters, finalAssignments)
        val specs = buildLayerSpecs(
            bitmap = bitmap,
            centers = activeCenters,
            assignments = finalAssignments,
            edges = edges,
            imageFileStore = imageFileStore,
        )
        return AdvancedQuantizationResult(
            previewBitmap = preview,
            layers = specs.sortedWith(compareBy<QuantizedLayerSpec> { it.color.luma() }.thenByDescending { it.pixelCount }),
        )
    }

    private fun collectWeightedSamples(bitmap: Bitmap, edges: BooleanArray): List<WeightedColor> {
        val totalPixels = bitmap.width * bitmap.height
        val step = sqrt(totalPixels / MAX_SAMPLES.toDouble()).roundToInt().coerceAtLeast(1)
        val samples = ArrayList<WeightedColor>(MAX_SAMPLES)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val color = bitmap.getPixel(x, y)
                val edge = edges.getOrNull(y * bitmap.width + x) == true
                val semanticWeight = semanticWeight(color)
                samples.add(
                    WeightedColor(
                        red = Color.red(color),
                        green = Color.green(color),
                        blue = Color.blue(color),
                        weight = 1.0 + (if (edge) 2.2 else 0.0) + semanticWeight,
                    ),
                )
            }
        }
        return samples.ifEmpty {
            listOf(WeightedColor(255, 255, 255, 1.0))
        }
    }

    private fun refineCenters(samples: List<WeightedColor>, targetColorCount: Int): List<Int> {
        var centers = initialCenters(samples, targetColorCount)
        repeat(KMEANS_ITERATIONS) {
            val totals = Array(centers.size) { DoubleArray(4) }
            samples.forEach { sample ->
                val nearest = nearestIndex(sample.toColor(), centers)
                totals[nearest][0] += sample.red * sample.weight
                totals[nearest][1] += sample.green * sample.weight
                totals[nearest][2] += sample.blue * sample.weight
                totals[nearest][3] += sample.weight
            }
            centers = centers.mapIndexed { index, center ->
                val count = totals[index][3]
                if (count <= 0.0) {
                    center
                } else {
                    Color.rgb(
                        (totals[index][0] / count).roundToInt().coerceIn(0, 255),
                        (totals[index][1] / count).roundToInt().coerceIn(0, 255),
                        (totals[index][2] / count).roundToInt().coerceIn(0, 255),
                    )
                }
            }
            centers = mergeSimilarCenters(centers)
            centers = fillMissingCenters(centers, samples, targetColorCount)
        }
        return centers.sortedBy { it.luma() }.take(targetColorCount)
    }

    private fun initialCenters(samples: List<WeightedColor>, targetColorCount: Int): List<Int> {
        val centers = mutableListOf<Int>()
        semanticAverage(samples, ::isLikelySkinTone)?.let(centers::add)
        semanticAverage(samples, ::isLikelyLipTone)?.let(centers::add)
        semanticAverage(samples, ::isLikelyHairTone)?.let(centers::add)
        semanticAverage(samples) { it.luma() < 42 }?.let(centers::add)
        semanticAverage(samples) { it.luma() > 216 }?.let(centers::add)
        return fillMissingCenters(mergeSimilarCenters(centers), samples, targetColorCount)
    }

    private fun fillMissingCenters(
        existingCenters: List<Int>,
        samples: List<WeightedColor>,
        targetColorCount: Int,
    ): List<Int> {
        val centers = existingCenters.toMutableList()
        val sorted = samples.sortedWith(compareBy<WeightedColor> { it.toColor().luma() }.thenBy { it.toColor().saturation() })
        var cursor = 0
        while (centers.size < targetColorCount && sorted.isNotEmpty()) {
            val position = ((cursor + 1) * (sorted.lastIndex.coerceAtLeast(1))) / (targetColorCount + 1)
            val candidate = sorted[position.coerceIn(0, sorted.lastIndex)].toColor()
            if (centers.none { colorDistance(it, candidate) < CENTER_MERGE_DISTANCE }) {
                centers.add(candidate)
            } else if (cursor > targetColorCount * 4) {
                centers.add(candidate)
            }
            cursor++
        }
        return centers.take(targetColorCount)
    }

    private fun mergeSimilarCenters(centers: List<Int>): List<Int> {
        val sorted = centers.distinct().sortedBy { it.luma() }
        val merged = mutableListOf<Int>()
        sorted.forEach { color ->
            val existingIndex = merged.indexOfFirst { colorDistance(it, color) < CENTER_MERGE_DISTANCE }
            if (existingIndex >= 0) {
                merged[existingIndex] = averageColor(merged[existingIndex], color)
            } else {
                merged.add(color)
            }
        }
        return merged
    }

    private fun assignPixels(bitmap: Bitmap, centers: List<Int>): IntArray {
        val assignments = IntArray(bitmap.width * bitmap.height)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                assignments[y * bitmap.width + x] = nearestIndex(bitmap.getPixel(x, y), centers)
            }
        }
        return assignments
    }

    private fun activeCentersAfterTinyMerge(assignments: IntArray, centers: List<Int>, totalPixels: Int): List<Int> {
        val counts = IntArray(centers.size)
        assignments.forEach { index -> counts[index]++ }
        val tinyThreshold = max(80, (totalPixels * TINY_LAYER_RATIO).roundToInt())
        val active = centers.filterIndexed { index, _ -> counts[index] >= tinyThreshold }
        return active.ifEmpty { centers.sortedByDescending { color -> counts[centers.indexOf(color)] }.take(1) }
    }

    private fun renderPreview(width: Int, height: Int, centers: List<Int>, assignments: IntArray): Bitmap {
        val preview = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(assignments.size) { index -> centers[assignments[index]] }
        preview.setPixels(pixels, 0, width, 0, 0, width, height)
        return preview
    }

    private fun buildLayerSpecs(
        bitmap: Bitmap,
        centers: List<Int>,
        assignments: IntArray,
        edges: BooleanArray,
        imageFileStore: ImageFileStore,
    ): List<QuantizedLayerSpec> {
        val counts = IntArray(centers.size)
        val edgeCounts = IntArray(centers.size)
        val importantCounts = IntArray(centers.size)
        assignments.forEachIndexed { pixelIndex, layerIndex ->
            val color = centers[layerIndex]
            counts[layerIndex]++
            if (edges.getOrNull(pixelIndex) == true) edgeCounts[layerIndex]++
            if (semanticWeight(color) > 0.0 || edges.getOrNull(pixelIndex) == true) importantCounts[layerIndex]++
        }
        val totalPixels = bitmap.width * bitmap.height
        return centers.sortedBy { it.luma() }.mapIndexed { orderIndex, color ->
            val originalIndex = centers.indexOf(color)
            val mask = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val layerBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val layerCanvas = Canvas(layerBitmap)
            layerCanvas.drawColor(Color.TRANSPARENT)
            val maskPixels = IntArray(totalPixels)
            val layerPixels = IntArray(totalPixels)
            assignments.forEachIndexed { pixelIndex, assignedIndex ->
                if (assignedIndex == originalIndex) {
                    maskPixels[pixelIndex] = Color.WHITE
                    layerPixels[pixelIndex] = color
                } else {
                    maskPixels[pixelIndex] = Color.BLACK
                    layerPixels[pixelIndex] = Color.TRANSPARENT
                }
            }
            mask.setPixels(maskPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            layerBitmap.setPixels(layerPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val coverage = counts[originalIndex] * 100f / totalPixels.coerceAtLeast(1)
            val warnings = qualityWarnings(color, coverage, edgeCounts[originalIndex], centers)
            QuantizedLayerSpec(
                color = color,
                red = Color.red(color),
                green = Color.green(color),
                blue = Color.blue(color),
                colorHex = color.toHex(),
                colorName = approximateColorName(color),
                recommendedOrder = orderIndex + 1,
                coveragePercent = coverage,
                pixelCount = counts[originalIndex],
                edgePixelCount = edgeCounts[originalIndex],
                importantPixelCount = importantCounts[originalIndex],
                maskUri = imageFileStore.saveBitmap("mask_${orderIndex + 1}", mask),
                layerBitmapUri = imageFileStore.saveBitmap("layer_${orderIndex + 1}", layerBitmap),
                qualityWarnings = warnings,
            )
        }
    }

    private fun qualityWarnings(color: Int, coverage: Float, edgePixels: Int, centers: List<Int>): List<String> {
        val warnings = mutableListOf<String>()
        if (coverage < 0.6f) warnings.add("Very small layer; it may be merged manually if drawing feels too slow.")
        if (edgePixels > 1800) warnings.add("High edge density; draw this layer slowly around details.")
        val nearest = centers.filter { it != color }.minOfOrNull { colorDistance(it, color) } ?: Double.MAX_VALUE
        if (nearest < 26.0) warnings.add("Similar neighboring color; consider merging if Instagram color selection is difficult.")
        if (color.luma() < 24) warnings.add("Very dark layer; select a clear black or near-black manually.")
        if (color.luma() > 232) warnings.add("Very light highlight layer; check visibility before drawing.")
        return warnings
    }

    private fun nearestIndex(color: Int, centers: List<Int>): Int {
        var bestIndex = 0
        var bestDistance = Double.MAX_VALUE
        centers.forEachIndexed { index, center ->
            val distance = colorDistance(color, center)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun semanticAverage(samples: List<WeightedColor>, predicate: (Int) -> Boolean): Int? {
        val selected = samples.filter { predicate(it.toColor()) }
        if (selected.isEmpty()) return null
        val totalWeight = selected.sumOf { it.weight }.coerceAtLeast(1.0)
        return Color.rgb(
            (selected.sumOf { it.red * it.weight } / totalWeight).roundToInt().coerceIn(0, 255),
            (selected.sumOf { it.green * it.weight } / totalWeight).roundToInt().coerceIn(0, 255),
            (selected.sumOf { it.blue * it.weight } / totalWeight).roundToInt().coerceIn(0, 255),
        )
    }

    private fun semanticWeight(color: Int): Double =
        when {
            isLikelySkinTone(color) -> 4.0
            isLikelyLipTone(color) -> 3.5
            isLikelyHairTone(color) -> 2.6
            color.luma() < 38 -> 2.1
            color.luma() > 218 -> 1.8
            else -> 0.0
        }

    private fun isLikelySkinTone(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val maxChannel = max(red, max(green, blue))
        val minChannel = min(red, min(green, blue))
        return red > 70 && green > 38 && blue > 24 && red > green && red > blue && maxChannel - minChannel > 12
    }

    private fun isLikelyLipTone(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return red > 105 && red - green > 24 && red - blue > 18 && green in 35..145
    }

    private fun isLikelyHairTone(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val luma = color.luma()
        return luma < 92 && abs(red - green) < 46 && blue < 90
    }

    private fun averageColor(a: Int, b: Int): Int =
        Color.rgb(
            (Color.red(a) + Color.red(b)) / 2,
            (Color.green(a) + Color.green(b)) / 2,
            (Color.blue(a) + Color.blue(b)) / 2,
        )

    private fun colorDistance(a: Int, b: Int): Double {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        val dl = a.luma() - b.luma()
        return dr.toDouble().pow(2) * 0.28 +
            dg.toDouble().pow(2) * 0.48 +
            db.toDouble().pow(2) * 0.16 +
            dl.toDouble().pow(2) * 0.08
    }

    private fun approximateColorName(color: Int): String =
        NAMED_COLORS.minBy { colorDistance(color, it.value) }.name

    private fun Int.luma(): Int =
        (Color.red(this) * 0.299f + Color.green(this) * 0.587f + Color.blue(this) * 0.114f).roundToInt()

    private fun Int.saturation(): Float {
        val hsv = FloatArray(3)
        Color.colorToHSV(this, hsv)
        return hsv[1]
    }

    private fun Int.toHex(): String = String.format("#%06X", 0xFFFFFF and this)

    private data class WeightedColor(
        val red: Int,
        val green: Int,
        val blue: Int,
        val weight: Double,
    ) {
        fun toColor(): Int = Color.rgb(red, green, blue)
    }

    private data class NamedColor(val name: String, val value: Int)

    private val NAMED_COLORS = listOf(
        NamedColor("Black", Color.rgb(10, 10, 10)),
        NamedColor("Charcoal", Color.rgb(45, 45, 48)),
        NamedColor("Gray", Color.rgb(128, 128, 128)),
        NamedColor("White", Color.rgb(245, 245, 245)),
        NamedColor("Ivory", Color.rgb(240, 230, 205)),
        NamedColor("Skin light", Color.rgb(238, 190, 160)),
        NamedColor("Skin medium", Color.rgb(190, 125, 86)),
        NamedColor("Skin deep", Color.rgb(112, 66, 48)),
        NamedColor("Brown", Color.rgb(122, 76, 42)),
        NamedColor("Dark brown", Color.rgb(70, 43, 28)),
        NamedColor("Blonde", Color.rgb(198, 160, 84)),
        NamedColor("Red", Color.rgb(205, 45, 55)),
        NamedColor("Pink", Color.rgb(225, 120, 155)),
        NamedColor("Orange", Color.rgb(220, 118, 42)),
        NamedColor("Yellow", Color.rgb(235, 202, 70)),
        NamedColor("Green", Color.rgb(70, 150, 84)),
        NamedColor("Teal", Color.rgb(50, 155, 150)),
        NamedColor("Blue", Color.rgb(60, 110, 210)),
        NamedColor("Navy", Color.rgb(32, 54, 112)),
        NamedColor("Purple", Color.rgb(126, 78, 190)),
        NamedColor("Violet", Color.rgb(150, 112, 230)),
    )

    private const val MAX_SAMPLES = 24_000
    private const val KMEANS_ITERATIONS = 16
    private const val CENTER_MERGE_DISTANCE = 22.0
    private const val TINY_LAYER_RATIO = 0.0025f
}
