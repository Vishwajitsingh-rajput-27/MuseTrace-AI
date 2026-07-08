package com.vishwajitrajput.musetraceai.data.repository

import android.graphics.BitmapFactory
import com.vishwajitrajput.musetraceai.core.common.DispatchersProvider
import com.vishwajitrajput.musetraceai.core.errors.AppError
import com.vishwajitrajput.musetraceai.core.security.GeminiKeyStore
import com.vishwajitrajput.musetraceai.core.storage.ImageFileStore
import com.vishwajitrajput.musetraceai.domain.AIImageProvider
import com.vishwajitrajput.musetraceai.domain.model.GeneratedImage
import com.vishwajitrajput.musetraceai.domain.model.GenerationRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class GeminiImageProvider @Inject constructor(
    private val keyStore: GeminiKeyStore,
    private val imageFileStore: ImageFileStore,
    private val okHttpClient: OkHttpClient,
    private val dispatchers: DispatchersProvider,
) : AIImageProvider {
    override val providerName: String = "Gemini"

    override suspend fun generate(request: GenerationRequest, enhancedPrompt: String): GeneratedImage =
        withContext(dispatchers.io) {
            val key = keyStore.getGeminiKey() ?: throw AppError.MissingGeminiKey()
            val httpRequest = Request.Builder()
                .url(GeminiInteractionApi.INTERACTIONS_URL)
                .header(GeminiInteractionApi.API_KEY_HEADER, key)
                .post(
                    GeminiInteractionApi.buildImageRequest(enhancedPrompt, request.aspectRatio)
                        .toString()
                        .toRequestBody("application/json".toMediaType()),
                )
                .build()

            val call = okHttpClient.newCall(httpRequest)
            val cancellationHandle = coroutineContext.job.invokeOnCompletion { cause ->
                if (cause is CancellationException) call.cancel()
            }
            try {
                call.execute().use { response ->
                    coroutineContext.ensureActive()
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) throw GeminiInteractionApi.toAppError(response.code, body)
                    val base64 = GeminiInteractionApi.extractGeneratedImage(body) ?: throw AppError.UnsupportedGenerationResponse()
                    val bytes = GeminiInteractionApi.decodeImageBytes(base64)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw AppError.UnsupportedGenerationResponse()
                    val uri = imageFileStore.saveBitmap("gemini", bitmap)
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
            } catch (error: CancellationException) {
                throw AppError.GenerationCancelled()
            } catch (error: InterruptedIOException) {
                throw AppError.ProviderTimeout(error)
            } catch (error: IOException) {
                if (!coroutineContext.isActive) throw AppError.GenerationCancelled()
                throw AppError.NoInternet(error)
            } finally {
                cancellationHandle.dispose()
            }
        }

    override suspend fun testConnection(): Boolean = withContext(dispatchers.io) {
        val key = keyStore.getGeminiKey() ?: throw AppError.MissingGeminiKey()
        val request = Request.Builder()
            .url(GeminiInteractionApi.MODELS_URL)
            .header(GeminiInteractionApi.API_KEY_HEADER, key)
            .get()
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.code == 400 || response.code == 401 || response.code == 403) {
                    throw AppError.InvalidGeminiKey()
                }
                if (response.code == 429) throw AppError.ProviderRateLimit()
                if (!response.isSuccessful) {
                    throw AppError.ProviderFailure("Gemini key test failed with HTTP ${response.code}.")
                }
                true
            }
        } catch (error: InterruptedIOException) {
            throw AppError.ProviderTimeout(error)
        } catch (error: IOException) {
            throw AppError.NoInternet(error)
        }
    }
}
