package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Entity specifically designed to persist extracted OCR text,
 * engine details, transcription metrics, and revision logs.
 */
@Entity(
    tableName = "ocr_records",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("documentId"),
        Index("ocrEngine"),
        Index("timestamp")
    ]
)
data class OcrRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val rawOcrText: String,
    val searchableText: String = rawOcrText,
    val ocrEngine: String = "ML_KIT", // "ML_KIT", "GEMINI_FAST", "GEMINI_DEEP", "MANUAL"
    val executionTimeMs: Long = 0L,
    val confidenceScore: Float = 1.0f,
    val wordCount: Int = 0,
    val lineCount: Int = 0,
    val blockCount: Int = 0,
    val detectedLanguage: String = "en",
    val recognizedBlocksJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
