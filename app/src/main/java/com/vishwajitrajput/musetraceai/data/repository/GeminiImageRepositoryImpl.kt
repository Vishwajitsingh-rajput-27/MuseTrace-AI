package com.vishwajitrajput.musetraceai.data.repository

import android.graphics.BitmapFactory
import com.vishwajitrajput.musetraceai.core.common.DispatchersProvider
import com.vishwajitrajput.musetraceai.core.errors.AppError
import com.vishwajitrajput.musetraceai.core.security.GeminiKeyStore
import com.vishwajitrajput.musetraceai.core.storage.ImageFileStore
import com.vishwajitrajput.musetraceai.domain.AiImageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiImageRepositoryImpl @Inject constructor(
    private val keyStore: GeminiKeyStore,
    private val imageFileStore: ImageFileStore,
    private val okHttpClient: OkHttpClient,
    private val dispatchers: DispatchersProvider,
) : AiImageRepository {
    override suspend fun generateImage(prompt: String, colorCount: Int): String = withContext(dispatchers.io) {
        val key = keyStore.getGeminiKey() ?: throw AppError.MissingGeminiKey()
        val requestJson = GeminiInteractionApi.buildImageRequest(
            enhancedPrompt = "Create a clean semi-realistic reference image for tracing in $colorCount color layers. Subject: $prompt",
        )
        val request = Request.Builder()
            .url(GeminiInteractionApi.INTERACTIONS_URL)
            .header(GeminiInteractionApi.API_KEY_HEADER, key)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw GeminiInteractionApi.toAppError(response.code, body)
                }
                val base64 = GeminiInteractionApi.extractGeneratedImage(body) ?: throw AppError.UnsupportedGenerationResponse()
                val bytes = GeminiInteractionApi.decodeImageBytes(base64)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw AppError.UnsupportedGenerationResponse()
                imageFileStore.saveBitmap("gemini", bitmap)
            }
        } catch (error: CancellationException) {
            throw AppError.GenerationCancelled()
        } catch (error: InterruptedIOException) {
            throw AppError.ProviderTimeout(error)
        } catch (error: IOException) {
            throw AppError.NoInternet(error)
        }
    }
}
