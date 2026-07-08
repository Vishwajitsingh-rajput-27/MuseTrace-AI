package com.vishwajitrajput.musetraceai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trace_projects")
data class TraceProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceUri: String,
    val previewPath: String,
    val colorCount: Int,
    val width: Int,
    val height: Int,
    val layersJson: String,
    val createdAtMillis: Long,
    val originalImageUri: String? = null,
    val geminiGeneratedImageUri: String? = null,
    val processedImageUri: String? = null,
    val previewImageUri: String? = null,
    val paletteJson: String = "[]",
    val calibrationJson: String? = null,
    val overlayJson: String? = null,
    val drawingSettingsJson: String? = null,
    val workflowJson: String? = null,
    val updatedAtMillis: Long = createdAtMillis,
)
