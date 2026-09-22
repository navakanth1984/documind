package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity dedicated to managing document metadata locally:
 * file properties, reading progress, user notes, favorite/archived status,
 * and category classifications.
 */
@Entity(
    tableName = "document_metadata",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("documentId", unique = true),
        Index("folderCategory"),
        Index("isFavorite"),
        Index("lastModifiedTimestamp")
    ]
)
data class DocumentMetadataEntity(
    @PrimaryKey val documentId: String,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "application/octet-stream",
    val pageCount: Int = 1,
    val scanResolution: String? = null,
    val author: String? = null,
    val creationTimestamp: Long = System.currentTimeMillis(),
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val lastViewedTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val folderCategory: String = "General",
    val tags: String = "",
    val userNotes: String = "",
    val readProgressPercent: Int = 0
)
