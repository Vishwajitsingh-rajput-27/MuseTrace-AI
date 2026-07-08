package com.vishwajitrajput.musetraceai.data.repository

import android.graphics.BitmapFactory
import android.util.Base64
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
import org.json.JSONObject
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
        val requestJson = JSONObject()
            .put("model", MODEL)
            .put(
                "input",
                "Create a clean semi-realistic reference image for tracing in $colorCount color layers. Subject: $prompt",
            )
            .put(
                "response_format",
                JSONObject()
                    .put("type", "image")
                    .put("mime_type", "image/png")
                    .put("aspect_ratio", "1:1")
                    .put("image_size", "1K"),
            )
        val request = Request.Builder()
            .url(INTERACTIONS_URL)
            .header(API_KEY_HEADER, key)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw response.toAppError(body)
                }
                val base64 = extractGeneratedImage(body) ?: throw AppError.UnsupportedGenerationResponse()
                val bytes = decodeImageBytes(base64)
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
        const val INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
        const val MODEL = "gemini-3.1-flash-image"
        const val API_KEY_HEADER = "x-goog-api-key"
    }
}
