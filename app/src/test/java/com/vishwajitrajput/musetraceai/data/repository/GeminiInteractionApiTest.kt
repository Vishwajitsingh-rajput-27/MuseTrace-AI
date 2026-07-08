package com.vishwajitrajput.musetraceai.data.repository

import com.vishwajitrajput.musetraceai.core.errors.AppError
import com.vishwajitrajput.musetraceai.domain.model.GenerationAspectRatio
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class GeminiInteractionApiTest {
    @Test
    fun buildImageRequestUsesTraceFriendlyImageResponseFormat() {
        val request = GeminiInteractionApi.buildImageRequest(
            enhancedPrompt = "semi-realistic colored sketch of a sunset",
            aspectRatio = GenerationAspectRatio.STORY,
        )

        assertEquals(GeminiInteractionApi.MODEL, request.getString("model"))
        assertEquals("text", request.getJSONArray("input").getJSONObject(0).getString("type"))
        assertEquals("9:16", request.getJSONObject("response_format").getString("aspect_ratio"))
        assertEquals("image/png", request.getJSONObject("response_format").getString("mime_type"))
    }

    @Test
    fun extractGeneratedImageSupportsOutputImageResponse() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val body = JSONObject()
            .put(
                "output_image",
                JSONObject().put("data", Base64.getEncoder().encodeToString(bytes)),
            )
            .toString()

        val encoded = GeminiInteractionApi.extractGeneratedImage(body)

        assertArrayEquals(bytes, GeminiInteractionApi.decodeImageBytes(encoded!!))
    }

    @Test
    fun mapsGeminiProviderErrorsToUserSafeErrors() {
        assertTrue(GeminiInteractionApi.toAppError(401, "{}") is AppError.InvalidGeminiKey)
        assertTrue(GeminiInteractionApi.toAppError(429, "{}") is AppError.ProviderRateLimit)
        assertTrue(GeminiInteractionApi.toAppError(504, "{}") is AppError.ProviderTimeout)
    }
}
