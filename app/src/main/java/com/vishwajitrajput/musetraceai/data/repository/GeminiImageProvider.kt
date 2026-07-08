package com.vishwajitrajput.musetraceai.data.repository

import android.graphics.BitmapFactory
import android.util.Base64
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
import org.json.JSONObject
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
                .url(INTERACTIONS_URL)
                .header(API_KEY_HEADER, key)
                .post(buildRequestJson(request, enhancedPrompt).toString().toRequestBody("application/json".toMediaType()))
                .build()

            val call = okHttpClient.newCall(httpRequest)
            val cancellationHandle = coroutineContext.job.invokeOnCompletion { cause ->
                if (cause is CancellationException) call.cancel()
            }
            try {
                call.execute().use { response ->
                    coroutineContext.ensureActive()
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) throw response.toAppError(body)
                    val base64 = extractGeneratedImage(body) ?: throw AppError.UnsupportedGenerationResponse()
                    val bytes = decodeImageBytes(base64)
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
            .url(BASE_URL)
            .header(API_KEY_HEADER, key)
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

    private fun buildRequestJson(request: GenerationRequest, enhancedPrompt: String): JSONObject =
        JSONObject()
            .put("model", MODEL)
            .put("input", enhancedPrompt)
            .put(
                "response_format",
                JSONObject()
                    .put("type", "image")
                    .put("mime_type", "image/png")
                    .put("aspect_ratio", request.aspectRatio.geminiAspectRatio())
                    .put("image_size", "1K"),
            )

    private fun extractGeneratedImage(json: String): String? {
        val root = JSONObject(json)
        return extractInteractionImage(root) ?: extractInlineImage(root)
    }

    private fun extractInteractionImage(root: JSONObject): String? {
        val steps = root.optJSONArray("steps") ?: return null
        for (stepIndex in 0 until steps.length()) {
            val content = steps.optJSONObject(stepIndex)?.optJSONArray("content") ?: continue
            for (contentIndex in 0 until content.length()) {
                val block = content.optJSONObject(contentIndex) ?: continue
                val data = block.optString("data").orEmpty()
                if (block.optString("type") == "image" && data.isNotBlank()) return data
            }
        }
        return null
    }

    private fun extractInlineImage(root: JSONObject): String? {
        val candidates = root.optJSONArray("candidates") ?: return null
        for (candidateIndex in 0 until candidates.length()) {
            val parts = candidates
                .optJSONObject(candidateIndex)
                ?.optJSONObject("content")
                ?.optJSONArray("parts") ?: continue
            for (partIndex in 0 until parts.length()) {
                val part = parts.optJSONObject(partIndex) ?: continue
                val inline = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                val data = inline?.optString("data").orEmpty()
                if (data.isNotBlank()) return data
            }
        }
        return null
    }

    private fun decodeImageBytes(data: String): ByteArray =
        runCatching {
            Base64.decode(data.substringAfter("base64,", data), Base64.DEFAULT)
        }.getOrElse {
            throw AppError.UnsupportedGenerationResponse()
        }

    private fun com.vishwajitrajput.musetraceai.domain.model.GenerationAspectRatio.geminiAspectRatio(): String =
        when (displayName) {
            "4:5" -> "4:5"
            "9:16" -> "9:16"
            "Custom canvas" -> "3:4"
            else -> "1:1"
        }

    private fun okhttp3.Response.toAppError(body: String): AppError {
        val message = runCatching {
            JSONObject(body)
                .optJSONObject("error")
                ?.optString("message")
                .orEmpty()
        }.getOrDefault("")
        return when (code) {
            400, 401, 403 -> AppError.InvalidGeminiKey()
            408, 504 -> AppError.ProviderTimeout()
            429 -> AppError.ProviderRateLimit()
            else -> AppError.ProviderFailure(
                message.ifBlank { "Gemini provider failed with HTTP $code." },
            )
        }
    }

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1/models"
        const val INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
        const val MODEL = "gemini-3.1-flash-image"
        const val API_KEY_HEADER = "x-goog-api-key"
    }
}
