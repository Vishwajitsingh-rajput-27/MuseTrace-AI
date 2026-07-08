package com.vishwajitrajput.musetraceai.data.repository

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import com.vishwajitrajput.musetraceai.core.common.DispatchersProvider
import com.vishwajitrajput.musetraceai.core.errors.AppError
import com.vishwajitrajput.musetraceai.core.storage.ImageFileStore
import com.vishwajitrajput.musetraceai.domain.ImageEditorRepository
import com.vishwajitrajput.musetraceai.domain.model.EditableImage
import com.vishwajitrajput.musetraceai.domain.model.EditedImage
import com.vishwajitrajput.musetraceai.domain.model.ImageEditSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class OpenCvImageEditorRepository @Inject constructor(
    private val imageFileStore: ImageFileStore,
    private val dispatchers: DispatchersProvider,
) : ImageEditorRepository {
    override suspend fun importImage(sourceUri: String): EditableImage = withContext(dispatchers.io) {
        try {
            coroutineContext.ensureActive()
            val imported = imageFileStore.loadBitmapSampled(sourceUri, PROCESS_MAX_SIDE)
            val sourceCopy = imageFileStore.saveBitmap("editor_source", imported)
            val preview = imported.scaleToMax(PREVIEW_MAX_SIDE)
            val previewUri = imageFileStore.saveBitmap("editor_before", preview)
            EditableImage(
                sourceUri = sourceCopy,
                previewUri = previewUri,
                width = imported.width,
                height = imported.height,
            )
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            if (throwable is AppError) throw throwable
            throw AppError.ImageLoad("MuseTrace could not import this image safely.", throwable)
        }
    }

    override suspend fun processImage(sourceUri: String, settings: ImageEditSettings): EditedImage =
        process(sourceUri, settings, "editor_processed")

    override suspend fun exportProcessedBitmap(sourceUri: String, settings: ImageEditSettings): EditedImage =
        process(sourceUri, settings, "editor_export")

    private suspend fun process(sourceUri: String, settings: ImageEditSettings, prefix: String): EditedImage =
        withContext(dispatchers.default) {
            try {
                coroutineContext.ensureActive()
                val source = imageFileStore.loadBitmapSampled(sourceUri, PROCESS_MAX_SIDE)
                coroutineContext.ensureActive()
                val processed = ImageEditorEngine.process(source, settings) {
                    coroutineContext.ensureActive()
                }
                coroutineContext.ensureActive()
                val processedUri = imageFileStore.saveBitmap(prefix, processed)
                val previewUri = imageFileStore.saveBitmap("${prefix}_preview", processed.scaleToMax(PREVIEW_MAX_SIDE))
                EditedImage(
                    processedUri = processedUri,
                    previewUri = previewUri,
                    width = processed.width,
                    height = processed.height,
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                if (throwable is AppError) throw throwable
                throw AppError.ImageLoad("MuseTrace could not process this image.", throwable)
            }
        }

    private companion object {
        const val PREVIEW_MAX_SIDE = 1280
        const val PROCESS_MAX_SIDE = 2600
    }
}

private object ImageEditorEngine {
    fun process(source: Bitmap, settings: ImageEditSettings, ensureActive: () -> Unit): Bitmap {
        ensureActive()
        val transformed = applyGeometry(source.copy(Bitmap.Config.ARGB_8888, false), settings)
        ensureActive()
        val enhanced = if (runCatching { OpenCVLoader.initDebug() }.getOrDefault(false)) {
            applyOpenCvEffects(transformed, settings)
        } else {
            applyPixelEffects(transformed, settings, ensureActive)
        }
        ensureActive()
        return simplifyColorRegions(
            bitmap = enhanced,
            strength = settings.backgroundSimplification,
            faceSafe = settings.faceSafeProcessing,
            ensureActive = ensureActive,
        )
    }

    private fun applyGeometry(source: Bitmap, settings: ImageEditSettings): Bitmap {
        var bitmap = source
        val matrix = Matrix().apply {
            if (settings.flipHorizontal || settings.flipVertical) {
                postScale(
                    if (settings.flipHorizontal) -1f else 1f,
                    if (settings.flipVertical) -1f else 1f,
                )
            }
            val rotation = ((settings.rotationDegrees % 360) + 360) % 360
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        if (!matrix.isIdentity) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        bitmap = cropCenter(bitmap, settings.cropPercent)
        bitmap = resize(bitmap, settings.resizePercent)
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun cropCenter(source: Bitmap, cropPercent: Float): Bitmap {
        val percent = (cropPercent / 100f).coerceIn(0.6f, 1f)
        if (percent >= 0.999f) return source
        val cropWidth = (source.width * percent).roundToInt().coerceIn(1, source.width)
        val cropHeight = (source.height * percent).roundToInt().coerceIn(1, source.height)
        val left = (source.width - cropWidth) / 2
        val top = (source.height - cropHeight) / 2
        return Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    }

    private fun resize(source: Bitmap, resizePercent: Float): Bitmap {
        val percent = (resizePercent / 100f).coerceIn(0.4f, 1f)
        if (percent >= 0.999f) return source
        val width = (source.width * percent).roundToInt().coerceAtLeast(1)
        val height = (source.height * percent).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun applyOpenCvEffects(source: Bitmap, settings: ImageEditSettings): Bitmap {
        var mat = Mat()
        Utils.bitmapToMat(source, mat)
        try {
            if (settings.noiseReduction > 0f || settings.portraitEnhancement > 0f) {
                mat = replace(mat, bilateral(mat, max(settings.noiseReduction, settings.portraitEnhancement * 0.35f)))
            }
            if (settings.autoEnhance) {
                mat = replace(mat, autoContrast(mat))
            }
            if (settings.contrast != 1f || settings.brightness != 0f) {
                mat = replace(mat, brightnessContrast(mat, settings.brightness, settings.contrast))
            }
            if (settings.saturation != 1f || settings.portraitEnhancement > 0f) {
                val saturation = settings.saturation + settings.portraitEnhancement * 0.12f
                mat = replace(mat, saturation(mat, saturation.coerceIn(0f, 2f)))
            }
            if (settings.sharpness > 0f || settings.edgeEnhance > 0f || settings.portraitEnhancement > 0f) {
                val amount = (settings.sharpness + settings.edgeEnhance * 0.75f + settings.portraitEnhancement * 0.2f)
                    .coerceIn(0f, 1.6f)
                mat = replace(mat, sharpen(mat, amount))
            }
            val output = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, output)
            return output
        } finally {
            mat.release()
        }
    }

    private fun replace(current: Mat, next: Mat): Mat {
        current.release()
        return next
    }

    private fun bilateral(source: Mat, intensity: Float): Mat {
        val dst = Mat()
        val diameter = (3 + intensity.coerceIn(0f, 1f) * 8f).roundToInt().let { if (it % 2 == 0) it + 1 else it }
        Imgproc.bilateralFilter(
            source,
            dst,
            diameter.coerceAtLeast(3),
            30.0 + intensity * 55.0,
            15.0 + intensity * 35.0,
        )
        return dst
    }

    private fun autoContrast(source: Mat): Mat {
        val rgb = Mat()
        val lab = Mat()
        val merged = Mat()
        val rgba = Mat()
        val channels = ArrayList<Mat>()
        return try {
            Imgproc.cvtColor(source, rgb, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(rgb, lab, Imgproc.COLOR_RGB2Lab)
            Core.split(lab, channels)
            Imgproc.createCLAHE(2.0, Size(8.0, 8.0)).apply(channels[0], channels[0])
            Core.merge(channels, merged)
            Imgproc.cvtColor(merged, rgb, Imgproc.COLOR_Lab2RGB)
            Imgproc.cvtColor(rgb, rgba, Imgproc.COLOR_RGB2RGBA)
            rgba.clone()
        } finally {
            rgb.release()
            lab.release()
            merged.release()
            rgba.release()
            channels.forEach { it.release() }
        }
    }

    private fun brightnessContrast(source: Mat, brightness: Float, contrast: Float): Mat {
        val dst = Mat()
        source.convertTo(dst, -1, contrast.toDouble(), brightness.toDouble())
        return dst
    }

    private fun saturation(source: Mat, saturation: Float): Mat {
        val rgb = Mat()
        val hsv = Mat()
        val rgba = Mat()
        val channels = ArrayList<Mat>()
        return try {
            Imgproc.cvtColor(source, rgb, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV)
            Core.split(hsv, channels)
            channels[1].convertTo(channels[1], -1, saturation.toDouble(), 0.0)
            Core.merge(channels, hsv)
            Imgproc.cvtColor(hsv, rgb, Imgproc.COLOR_HSV2RGB)
            Imgproc.cvtColor(rgb, rgba, Imgproc.COLOR_RGB2RGBA)
            rgba.clone()
        } finally {
            rgb.release()
            hsv.release()
            rgba.release()
            channels.forEach { it.release() }
        }
    }

    private fun sharpen(source: Mat, amount: Float): Mat {
        val dst = Mat()
        val a = amount.coerceIn(0f, 1.6f).toDouble()
        val kernel = Mat(3, 3, CvType.CV_32F)
        try {
            kernel.put(
                0,
                0,
                0.0,
                -a,
                0.0,
                -a,
                1.0 + 4.0 * a,
                -a,
                0.0,
                -a,
                0.0,
            )
            Imgproc.filter2D(source, dst, source.depth(), kernel)
            return dst
        } finally {
            kernel.release()
        }
    }

    private fun applyPixelEffects(
        source: Bitmap,
        settings: ImageEditSettings,
        ensureActive: () -> Unit,
    ): Bitmap {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val contrast = settings.contrast + if (settings.autoEnhance) 0.08f else 0f
        val brightness = settings.brightness + if (settings.autoEnhance) 6f else 0f
        val saturation = settings.saturation + settings.portraitEnhancement * 0.12f
        pixels.indices.forEach { index ->
            if (index % CANCELLATION_CHECK_INTERVAL == 0) ensureActive()
            pixels[index] = adjustPixel(pixels[index], brightness, contrast, saturation)
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun adjustPixel(color: Int, brightness: Float, contrast: Float, saturation: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * saturation).coerceIn(0f, 1f)
        hsv[2] = (((hsv[2] - 0.5f) * contrast) + 0.5f + brightness / 255f).coerceIn(0f, 1f)
        return Color.HSVToColor(Color.alpha(color), hsv)
    }

    private fun simplifyColorRegions(
        bitmap: Bitmap,
        strength: Float,
        faceSafe: Boolean,
        ensureActive: () -> Unit,
    ): Bitmap {
        if (strength <= 0f) return bitmap
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(output.width * output.height)
        output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
        val baseStep = (18 + strength.coerceIn(0f, 1f) * 54f).roundToInt()
        pixels.indices.forEach { index ->
            if (index % CANCELLATION_CHECK_INTERVAL == 0) ensureActive()
            val color = pixels[index]
            val localStrength = if (faceSafe && isLikelySkinTone(color)) strength * 0.25f else strength
            val step = (baseStep * localStrength.coerceIn(0.15f, 1f)).roundToInt().coerceAtLeast(8)
            val simplified = Color.argb(
                Color.alpha(color),
                quantizeChannel(Color.red(color), step),
                quantizeChannel(Color.green(color), step),
                quantizeChannel(Color.blue(color), step),
            )
            pixels[index] = blend(color, simplified, localStrength)
        }
        output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
        return output
    }

    private fun quantizeChannel(value: Int, step: Int): Int =
        (((value + step / 2) / step) * step).coerceIn(0, 255)

    private fun blend(original: Int, target: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        return Color.argb(
            Color.alpha(original),
            (Color.red(original) + (Color.red(target) - Color.red(original)) * t).roundToInt().coerceIn(0, 255),
            (Color.green(original) + (Color.green(target) - Color.green(original)) * t).roundToInt().coerceIn(0, 255),
            (Color.blue(original) + (Color.blue(target) - Color.blue(original)) * t).roundToInt().coerceIn(0, 255),
        )
    }

    private fun isLikelySkinTone(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val maxChannel = max(red, max(green, blue))
        val minChannel = minOf(red, green, blue)
        return red > 65 && green > 35 && blue > 20 && red > green && red > blue && maxChannel - minChannel > 12
    }

    private const val CANCELLATION_CHECK_INTERVAL = 8192
}

private fun Bitmap.scaleToMax(maxSide: Int): Bitmap {
    val largest = width.coerceAtLeast(height)
    if (largest <= maxSide) return this
    val ratio = maxSide.toFloat() / largest.toFloat()
    return Bitmap.createScaledBitmap(
        this,
        (width * ratio).roundToInt().coerceAtLeast(1),
        (height * ratio).roundToInt().coerceAtLeast(1),
        true,
    )
}
