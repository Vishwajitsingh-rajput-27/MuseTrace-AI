package com.vishwajitrajput.musetraceai.data.local

import com.vishwajitrajput.musetraceai.domain.model.GeneratedImage

fun GeneratedImage.toEntity(): GeneratedImageEntity = GeneratedImageEntity(
    id = id,
    prompt = prompt,
    enhancedPrompt = enhancedPrompt,
    negativePrompt = negativePrompt,
    styleName = styleName,
    aspectRatioName = aspectRatioName,
    imageUri = imageUri,
    providerName = providerName,
    createdAtMillis = createdAtMillis,
)

fun GeneratedImageEntity.toDomain(): GeneratedImage = GeneratedImage(
    id = id,
    prompt = prompt,
    enhancedPrompt = enhancedPrompt,
    negativePrompt = negativePrompt,
    styleName = styleName,
    aspectRatioName = aspectRatioName,
    imageUri = imageUri,
    providerName = providerName,
    createdAtMillis = createdAtMillis,
)
