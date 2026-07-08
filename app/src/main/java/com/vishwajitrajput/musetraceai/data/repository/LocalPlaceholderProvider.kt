package com.vishwajitrajput.musetraceai.data.repository

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.vishwajitrajput.musetraceai.core.common.DispatchersProvider
import com.vishwajitrajput.musetraceai.core.storage.ImageFileStore
import com.vishwajitrajput.musetraceai.domain.AIImageProvider
import com.vishwajitrajput.musetraceai.domain.model.GeneratedImage
import com.vishwajitrajput.musetraceai.domain.model.GenerationRequest
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalPlaceholderProvider @Inject constructor(
    private val imageFileStore: ImageFileStore,
    private val dispatchers: DispatchersProvider,
) : AIImageProvider {
    override val providerName: String = "Local"

    override suspend fun generate(request: GenerationRequest, enhancedPrompt: String): GeneratedImage =
        withContext(dispatchers.default) {
            val bitmap = Bitmap.createBitmap(
                request.aspectRatio.width.coerceAtMost(1200),
                request.aspectRatio.height.coerceAtMost(1600),
                Bitmap.Config.ARGB_8888,
            )
            renderPromptPoster(bitmap, request)
            val uri = imageFileStore.saveBitmap("local_generation", bitmap)
            GeneratedImage(
                id = 0,
                prompt = request.prompt,
                enhancedPrompt = enhancedPrompt,
                negativePrompt = request.negativePrompt,
                styleName = request.style.displayName,
                aspectRatioName = request.aspectRatio.displayName,
                imageUri = uri,
                providerName = providerName,
                createdAtMillis = System.currentTimeMillis(),
            )
        }

    override suspend fun testConnection(): Boolean = true

    private fun renderPromptPoster(bitmap: Bitmap, request: GenerationRequest) {
        val canvas = Canvas(bitmap)
        val seed = (request.prompt + request.style.displayName).hashCode() and Int.MAX_VALUE
        val background = Color.rgb(12 + seed % 24, 12 + seed / 3 % 24, 18 + seed / 7 % 28)
        canvas.drawColor(background)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = bitmap.width.coerceAtMost(bitmap.height) / 140f
            color = Color.rgb(180 + seed % 50, 180 + seed / 5 % 50, 230)
        }
        val inset = bitmap.width * 0.12f
        val rect = RectF(inset, bitmap.height * 0.14f, bitmap.width - inset, bitmap.height * 0.78f)
        canvas.drawOval(rect, paint)
        repeat(9) { index ->
            val y = bitmap.height * (0.22f + index * 0.055f)
            canvas.drawLine(inset * 1.4f, y, bitmap.width - inset * 1.4f, y + ((index % 3) - 1) * 18f, paint)
        }
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = bitmap.width / 20f
        canvas.drawText(request.style.displayName, bitmap.width / 2f, bitmap.height * 0.88f, paint)
    }
}
