package com.vishwajitrajput.musetraceai.core.errors

sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MissingGeminiKey : AppError("Add your Gemini API key in Settings before generating images.")
    class InvalidGeminiKey : AppError("Gemini rejected this API key. Check the key and save it again.")
    class NoInternet(cause: Throwable? = null) : AppError("No internet connection is available for Gemini generation.", cause)
    class ProviderTimeout(cause: Throwable? = null) : AppError("Gemini generation timed out. Try again with a shorter prompt.", cause)
    class ProviderRateLimit : AppError("Gemini rate limit reached. Wait a moment, then try again.")
    class GenerationCancelled : AppError("Generation was cancelled.")
    class UnsupportedGenerationResponse : AppError("Gemini returned an unsupported response for image generation.")
    class GeminiResponse(message: String) : AppError(message)
    class ProviderFailure(message: String, cause: Throwable? = null) : AppError(message, cause)
    class ImageLoad(message: String, cause: Throwable? = null) : AppError(message, cause)
    class SketchProcessing(message: String, cause: Throwable? = null) : AppError(message, cause)
    class DrawingUnavailable(message: String) : AppError(message)
}

fun Throwable.readableMessage(): String = message ?: "Something went wrong. Please try again."
