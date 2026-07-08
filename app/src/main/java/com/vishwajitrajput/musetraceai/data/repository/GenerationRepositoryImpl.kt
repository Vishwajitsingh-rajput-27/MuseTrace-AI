package com.vishwajitrajput.musetraceai.data.repository

import com.vishwajitrajput.musetraceai.data.local.GeneratedImageDao
import com.vishwajitrajput.musetraceai.data.local.toDomain
import com.vishwajitrajput.musetraceai.data.local.toEntity
import com.vishwajitrajput.musetraceai.domain.GenerationRepository
import com.vishwajitrajput.musetraceai.domain.model.GeneratedImage
import com.vishwajitrajput.musetraceai.domain.model.GenerationRequest
import com.vishwajitrajput.musetraceai.domain.usecase.PromptEnhancer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerationRepositoryImpl @Inject constructor(
    private val generatedImageDao: GeneratedImageDao,
    private val geminiImageProvider: GeminiImageProvider,
    private val promptEnhancer: PromptEnhancer,
) : GenerationRepository {
    override fun observeHistory(): Flow<List<GeneratedImage>> =
        generatedImageDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun generate(request: GenerationRequest): GeneratedImage {
        val enhancedPrompt = promptEnhancer.enhance(request)
        val image = geminiImageProvider.generate(request, enhancedPrompt)
        val id = generatedImageDao.insert(image.toEntity())
        return image.copy(id = id)
    }

    override suspend fun saveGeneratedImage(image: GeneratedImage): Long =
        generatedImageDao.insert(image.copy(id = 0).toEntity())

    override suspend fun testGeminiKey(): Boolean = geminiImageProvider.testConnection()
}
