package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Composite model joining a Document with all its associated OCR text extraction records.
 */
data class DocumentWithOcrRecords(
    @Embedded val document: DocumentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val ocrRecords: List<OcrRecordEntity>
)

/**
 * Composite model joining a Document with its OCR records and rich metadata entity.
 */
data class DocumentWithFullDetails(
    @Embedded val document: DocumentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val ocrRecords: List<OcrRecordEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val metadata: DocumentMetadataEntity?
)

/**
 * Aggregation result for category summary statistics.
 */
data class CategoryDocCount(
    val folderCategory: String,
    val documentCount: Int,
    val totalWords: Int
)

/**
 * Aggregation result for OCR engine performance and volume statistics.
 */
data class OcrStatsSummary(
    val totalRecords: Int,
    val totalWordCount: Int,
    val avgExecutionTimeMs: Double,
    val mlKitCount: Int,
    val geminiCount: Int
)
