package com.vishwajitrajput.musetraceai.domain.usecase

import com.vishwajitrajput.musetraceai.domain.model.GenerationAspectRatio
import com.vishwajitrajput.musetraceai.domain.model.GenerationRequest
import com.vishwajitrajput.musetraceai.domain.model.GenerationStyle
import javax.inject.Inject

class PromptEnhancer @Inject constructor() {
    fun enhance(request: GenerationRequest): String {
        val subject = normalizeSubject(request.prompt)
        val positivePrompt = buildPositivePrompt(request.style, request.aspectRatio, subject)
        val negativePrompt = buildNegativePrompt(request.negativePrompt)
        return "$positivePrompt Negative prompt: $negativePrompt."
    }

    fun buildNegativePrompt(userNegativePrompt: String): String {
        val userTerms = userNegativePrompt
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return (DEFAULT_NEGATIVE_TERMS + userTerms)
            .distinctBy { it.lowercase() }
            .joinToString(", ")
    }

    private fun buildPositivePrompt(
        style: GenerationStyle,
        aspectRatio: GenerationAspectRatio,
        subject: String,
    ): String {
        val stylePrompt = style.promptOpening(subject)
        val styleTraits = style.drawingTraits()
        val canvasInstruction = aspectRatio.canvasInstruction()
        return listOf(
            stylePrompt,
            styleTraits,
            "clear subject",
            "bold outlines",
            "simple shapes",
            "high contrast",
            "clean color regions",
            "low background complexity",
            "16-color friendly palette",
            "optimized for layered color drawing",
            "optimized for 16-color layered Instagram Draw recreation",
            canvasInstruction,
            "no tiny details",
            "no tiny text",
            "no excessive detail",
            "no text",
            "no watermark",
        ).joinToString(", ") + "."
    }

    private fun normalizeSubject(prompt: String): String {
        val trimmed = prompt.trim()
            .replace(Regex("\\s+"), " ")
            .trim('.', ',', ';', ':', '-', ' ')
        return trimmed.ifBlank { "a clear centered subject" }
    }

    private fun GenerationStyle.promptOpening(subject: String): String =
        when (this) {
            GenerationStyle.SEMI_REALISTIC_SKETCH ->
                "semi-realistic colored sketch of $subject"
            GenerationStyle.ANIME_SKETCH ->
                "anime sketch of $subject with expressive clean line art"
            GenerationStyle.CARTOON_PORTRAIT ->
                "cartoon portrait of $subject with readable facial features"
            GenerationStyle.PENCIL_SKETCH ->
                "pencil sketch of $subject with clean graphite-style shading"
            GenerationStyle.MARKER_DRAWING ->
                "marker drawing of $subject with confident graphic strokes"
            GenerationStyle.WATERCOLOR_SKETCH ->
                "watercolor sketch of $subject with soft washes and clear contour lines"
            GenerationStyle.COUPLE_DOODLE ->
                "warm couple doodle of $subject with simple expressive poses"
            GenerationStyle.CUTE_STICKER_STYLE ->
                "cute sticker style drawing of $subject with crisp sticker-like edges"
            GenerationStyle.PORTRAIT_SKETCH ->
                "portrait sketch of $subject with recognizable simplified features"
            GenerationStyle.MINIMAL_COLOR_POSTER ->
                "minimal color poster of $subject with strong silhouettes"
            GenerationStyle.HIGH_CONTRAST_DRAWING ->
                "high contrast drawing of $subject with bold light and shadow separation"
        }

    private fun GenerationStyle.drawingTraits(): String =
        when (this) {
            GenerationStyle.SEMI_REALISTIC_SKETCH ->
                "expressive lighting, smooth forms, semi-realistic sketch output"
            GenerationStyle.ANIME_SKETCH ->
                "large readable shapes, clean anime contours, simplified shading"
            GenerationStyle.CARTOON_PORTRAIT ->
                "rounded shapes, simplified color blocks, friendly expression"
            GenerationStyle.PENCIL_SKETCH ->
                "controlled pencil texture, clear edges, limited tonal regions"
            GenerationStyle.MARKER_DRAWING ->
                "flat marker color fills, strong edges, minimal blending"
            GenerationStyle.WATERCOLOR_SKETCH ->
                "gentle watercolor texture, preserved outlines, simple wash areas"
            GenerationStyle.COUPLE_DOODLE ->
                "balanced two-person composition, sweet mood, uncluttered background"
            GenerationStyle.CUTE_STICKER_STYLE ->
                "adorable simplified proportions, clean border, compact composition"
            GenerationStyle.PORTRAIT_SKETCH ->
                "face-focused composition, readable eyes and hair, simplified skin tones"
            GenerationStyle.MINIMAL_COLOR_POSTER ->
                "poster-like composition, few large color fields, crisp silhouette"
            GenerationStyle.HIGH_CONTRAST_DRAWING ->
                "dramatic contrast, bold shadow regions, clear negative space"
        }

    private fun GenerationAspectRatio.canvasInstruction(): String =
        when (this) {
            GenerationAspectRatio.SQUARE -> "square 1:1 composition"
            GenerationAspectRatio.PORTRAIT -> "vertical 4:5 portrait composition"
            GenerationAspectRatio.STORY -> "tall 9:16 story composition"
            GenerationAspectRatio.CUSTOM_CANVAS -> "custom vertical drawing canvas composition"
        }

    companion object {
        val DEFAULT_NEGATIVE_TERMS: List<String> = listOf(
            "blurry",
            "noisy",
            "too detailed",
            "tiny text",
            "complex background",
            "photorealistic texture",
            "excessive shadows",
            "low contrast",
            "messy lines",
            "overcomplicated background",
        )
    }
}
