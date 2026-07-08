package com.vishwajitrajput.musetraceai.data.repository

import com.vishwajitrajput.musetraceai.core.errors.AppError
import com.vishwajitrajput.musetraceai.domain.model.GenerationAspectRatio
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

internal object GeminiInteractionApi {
    const val MODELS_URL = "https://generativelanguage.googleapis.com/v1/models"
    const val INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
    const val MODEL = "gemini-3.1-flash-image"
    const val API_KEY_HEADER = "x-goog-api-key"

    fun buildImageRequest(
        enhancedPrompt: String,
        aspectRatio: GenerationAspectRatio = GenerationAspectRatio.SQUARE,
    ): JSONObject =
        JSONObject()
            .put("model", MODEL)
            .put(
                "input",
                JSONArray().put(
                    JSONObject()
                        .put("type", "text")
                        .put("text", enhancedPrompt),
                ),
            )
            .put(
                "response_format",
                JSONObject()
                    .put("type", "image")
                    .put("mime_type", "image/png")
                    .put("aspect_ratio", aspectRatio.geminiAspectRatio())
                    .put("image_size", "1K"),
            )

    fun extractGeneratedImage(json: String): String? {
        val root = JSONObject(json)
        return extractOutputImage(root) ?: extractInteractionImage(root) ?: extractInlineImage(root)
    }

    fun decodeImageBytes(data: String): ByteArray =
        runCatching {
            val normalized = data.substringAfter("base64,", data).filterNot { it.isWhitespace() }
            Base64.getDecoder().decode(normalized)
        }.getOrElse {
            throw AppError.UnsupportedGenerationResponse()
        }

    fun toAppError(code: Int, body: String): AppError {
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

    private fun extractOutputImage(root: JSONObject): String? {
        val outputImage = root.optJSONObject("output_image") ?: root.optJSONObject("outputImage")
        return outputImage?.optString("data").orEmpty().takeIf { it.isNotBlank() }
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

    private fun GenerationAspectRatio.geminiAspectRatio(): String =
        when (this) {
            GenerationAspectRatio.SQUARE -> "1:1"
            GenerationAspectRatio.PORTRAIT -> "4:5"
            GenerationAspectRatio.STORY -> "9:16"
            GenerationAspectRatio.CUSTOM_CANVAS -> "3:4"
        }
}
