package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a document with its core metadata, categorization,
 * and persisted extracted OCR text content.
 */
@Entity(
    tableName = "documents",
    indices = [
        Index("folderCategory"),
        Index("isOfflinePrivacy"),
        Index("timestamp"),
        Index("isFavorite"),
        Index("isArchived")
    ]
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val fileType: String, // "PDF", "EPUB", "IMAGE", "VIDEO", "TEXT"
    val fileName: String,
    val extractedText: String,
    val summary: String,
    val folderCategory: String, // "Computer Science", "Biology", "Mathematics", "Literature", "History", "Exam Prep", "Confidential / Offline"
    val tags: String, // comma-separated
    val isOfflinePrivacy: Boolean = false,
    val isCloudSynced: Boolean = false,
    val wordCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBase64: String? = null,
    // Enhanced OCR & local document metadata fields
    val characterCount: Int = 0,
    val lineCount: Int = 0,
    val ocrEngine: String = "ML_KIT", // "ML_KIT", "GEMINI_FAST", "GEMINI_DEEP", "MANUAL"
    val confidenceScore: Float = 1.0f,
    val detectedLanguage: String = "en",
    val fileSizeBytes: Long = 0L,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val lastModifiedTimestamp: Long = timestamp,
    val userNotes: String = ""
)

