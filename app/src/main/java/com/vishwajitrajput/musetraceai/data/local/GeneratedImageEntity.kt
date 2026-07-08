package com.vishwajitrajput.musetraceai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prompt: String,
    val enhancedPrompt: String,
    val negativePrompt: String,
    val styleName: String,
    val aspectRatioName: String,
    val imageUri: String,
    val providerName: String,
    val createdAtMillis: Long,
)
