package com.vishwajitrajput.musetraceai.domain.usecase

import com.vishwajitrajput.musetraceai.domain.GenerationRepository
import com.vishwajitrajput.musetraceai.domain.model.GeneratedImage
import com.vishwajitrajput.musetraceai.domain.model.GenerationAspectRatio
import com.vishwajitrajput.musetraceai.domain.model.GenerationRequest
import com.vishwajitrajput.musetraceai.domain.model.GenerationStyle
import javax.inject.Inject

class GenerateImageUseCase @Inject constructor(
    private val generationRepository: GenerationRepository,
) {
    suspend operator fun invoke(request: GenerationRequest): GeneratedImage {
        require(request.prompt.isNotBlank()) { "Enter a prompt before generating." }
        return generationRepository.generate(request)
    }
}

class ObserveGenerationHistoryUseCase @Inject constructor(
    private val generationRepository: GenerationRepository,
) {
    operator fun invoke() = generationRepository.observeHistory()
}

class SaveGeneratedImageUseCase @Inject constructor(
    private val generationRepository: GenerationRepository,
) {
    suspend operator fun invoke(image: GeneratedImage): Long = generationRepository.saveGeneratedImage(image)
}

class TestGeminiKeyUseCase @Inject constructor(
    private val generationRepository: GenerationRepository,
) {
    suspend operator fun invoke(): Boolean = generationRepository.testGeminiKey()
}

data class GenerationUseCases @Inject constructor(
    val generate: GenerateImageUseCase,
    val observeHistory: ObserveGenerationHistoryUseCase,
    val save: SaveGeneratedImageUseCase,
    val testGeminiKey: TestGeminiKeyUseCase,
) {
    fun request(
        prompt: String,
        negativePrompt: String,
        styleName: String,
        aspectRatioName: String,
    ): GenerationRequest = GenerationRequest(
        prompt = prompt,
        negativePrompt = negativePrompt,
        style = GenerationStyle.fromDisplayName(styleName),
        aspectRatio = GenerationAspectRatio.fromDisplayName(aspectRatioName),
    )
}
