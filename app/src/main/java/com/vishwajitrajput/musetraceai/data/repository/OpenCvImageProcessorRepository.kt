package com.vishwajitrajput.musetraceai.data.repository

import android.graphics.Bitmap
import android.graphics.Color
import com.vishwajitrajput.musetraceai.core.common.DispatchersProvider
import com.vishwajitrajput.musetraceai.core.errors.AppError
import com.vishwajitrajput.musetraceai.core.storage.ImageFileStore
import com.vishwajitrajput.musetraceai.domain.ImageProcessorRepository
import com.vishwajitrajput.musetraceai.domain.model.ProjectDrawingSettings
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import com.vishwajitrajput.musetraceai.domain.model.TraceProjectAssets
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.sqrt

@Singleton
class OpenCvImageProcessorRepository @Inject constructor(
    private val imageFileStore: ImageFileStore,
    private val dispatchers: DispatchersProvider,
) : ImageProcessorRepository {
    override suspend fun createSketch(sourceUri: String, colorCount: Int): TraceProject = withContext(dispatchers.default) {
        try {
            val source = imageFileStore.loadBitmap(sourceUri)
            val working = source.scaleToMax(760).copy(Bitmap.Config.ARGB_8888, false)
            val edges = EdgeDetector.detect(working)
            val quantized = AdvancedColorQuantizer.quantize(working, colorCount, edges, imageFileStore)
            val layers = StrokePlanner.plan(working.width, working.height, quantized.layers, imageFileStore)
            val previewPath = imageFileStore.saveBitmap("preview", quantized.previewBitmap)
            TraceProject(
                id = 0,
                title = "Trace ${System.currentTimeMillis()}",
                sourceUri = sourceUri,
                previewPath = previewPath,
                colorCount = colorCount,
                width = working.width,
                height = working.height,
                layers = layers,
                createdAtMillis = System.currentTimeMillis(),
                assets = TraceProjectAssets(
                    originalImageUri = sourceUri,
                    processedImageUri = sourceUri,
                    previewImageUri = previewPath,
                ),
                palette = layers.map {
                    com.vishwajitrajput.musetraceai.domain.model.TracePaletteColor(
                        index = it.index,
                        colorHex = it.colorHex,
                        red = it.red,
                        green = it.green,
                        blue = it.blue,
                        colorName = it.colorName,
                        difficultyScore = it.difficultyScore,
                        estimatedDrawingTimeMs = it.estimatedDrawingTimeMs,
                        recommendedOrder = it.recommendedOrder,
                        coveragePercent = it.coveragePercent,
                        pixelCount = it.pixelCount,
                    )
                },
                drawingSettings = ProjectDrawingSettings(colorCount = colorCount),
            )
        } catch (throwable: Throwable) {
            if (throwable is AppError) throw throwable
            throw AppError.SketchProcessing("MuseTrace could not convert this image into layers.", throwable)
        }
    }

    private fun Bitmap.scaleToMax(maxSide: Int): Bitmap {
        val largest = max(width, height)
        if (largest <= maxSide) return this
        val ratio = maxSide.toFloat() / largest.toFloat()
        return Bitmap.createScaledBitmap(this, (width * ratio).toInt(), (height * ratio).toInt(), true)
    }
}

private object EdgeDetector {
    fun detect(bitmap: Bitmap): BooleanArray =
        if (runCatching { OpenCVLoader.initDebug() }.getOrDefault(false)) {
            runCatching { canny(bitmap) }.getOrElse { fallback(bitmap) }
        } else {
            fallback(bitmap)
        }

    private fun canny(bitmap: Bitmap): BooleanArray {
        val rgba = Mat()
        val gray = Mat()
        val blurred = Mat()
        val edges = Mat()
        return try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)
            Imgproc.Canny(blurred, edges, 55.0, 145.0)
            val bytes = ByteArray(bitmap.width * bitmap.height)
            edges.get(0, 0, bytes)
            BooleanArray(bytes.size) { (bytes[it].toInt() and 0xFF) > 0 }
        } finally {
            rgba.release()
            gray.release()
            blurred.release()
            edges.release()
        }
    }

    private fun fallback(bitmap: Bitmap): BooleanArray {
        val w = bitmap.width
        val h = bitmap.height
        val edges = BooleanArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val center = bitmap.getPixel(x, y).luma()
                val dx = bitmap.getPixel(x + 1, y).luma() - bitmap.getPixel(x - 1, y).luma()
                val dy = bitmap.getPixel(x, y + 1).luma() - bitmap.getPixel(x, y - 1).luma()
                edges[y * w + x] = sqrt((dx * dx + dy * dy).toDouble()) > 38.0 || center < 36
            }
        }
        return edges
    }
}

private fun Int.luma(): Int = (Color.red(this) * 0.299f + Color.green(this) * 0.587f + Color.blue(this) * 0.114f).toInt()
