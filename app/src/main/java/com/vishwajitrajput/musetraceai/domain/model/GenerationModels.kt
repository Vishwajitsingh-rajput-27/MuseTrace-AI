package com.vishwajitrajput.musetraceai.domain.model

enum class GenerationStyle(
    val displayName: String,
    val instruction: String,
) {
    SEMI_REALISTIC_SKETCH("Semi-realistic sketch", "semi-realistic layered sketch with clean contours and expressive shading"),
    ANIME_SKETCH("Anime sketch", "anime-inspired sketch with clean linework and readable facial or object features"),
    CARTOON_PORTRAIT("Cartoon portrait", "cartoon portrait with simplified forms and trace-friendly color separation"),
    PENCIL_SKETCH("Pencil sketch", "pencil sketch with soft graphite texture and strong edge clarity"),
    MARKER_DRAWING("Marker drawing", "marker drawing with bold strokes, confident shapes, and flat color zones"),
    WATERCOLOR_SKETCH("Watercolor sketch", "watercolor sketch with translucent washes and traceable borders"),
    COUPLE_DOODLE("Couple doodle", "warm couple doodle with simple expressive faces and balanced composition"),
    CUTE_STICKER_STYLE("Cute sticker style", "cute sticker-style drawing with crisp outline and rounded features"),
    PORTRAIT_SKETCH("Portrait sketch", "portrait sketch with recognizable features and simplified tonal layers"),
    MINIMAL_COLOR_POSTER("Minimal color poster", "minimal color poster with strong silhouette and limited palette"),
    HIGH_CONTRAST_DRAWING("High contrast drawing", "high contrast drawing with bold shadows and clear negative space");

    companion object {
        fun fromDisplayName(value: String): GenerationStyle =
            entries.firstOrNull { it.displayName == value } ?: SEMI_REALISTIC_SKETCH
    }
}

enum class GenerationAspectRatio(
    val displayName: String,
    val width: Int,
    val height: Int,
) {
    SQUARE("1:1", 1024, 1024),
    PORTRAIT("4:5", 1024, 1280),
    STORY("9:16", 1080, 1920),
    CUSTOM_CANVAS("Custom canvas", 1080, 1440);

    companion object {
        fun fromDisplayName(value: String): GenerationAspectRatio =
            entries.firstOrNull { it.displayName == value } ?: SQUARE
    }
}

data class GenerationRequest(
    val prompt: String,
    val negativePrompt: String,
    val style: GenerationStyle,
    val aspectRatio: GenerationAspectRatio,
)

data class GeneratedImage(
    val id: Long,
    val prompt: String,
    val enhancedPrompt: String,
    val negativePrompt: String,
    val styleName: String,
    val aspectRatioName: String,
    val imageUri: String,
    val providerName: String,
    val createdAtMillis: Long,
)
