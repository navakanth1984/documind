package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryDocCount
import com.example.data.model.DocumentMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing document metadata locally,
 * including user notes, classifications, favorites, archives, and reading progress.
 */
@Dao
interface DocumentMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMetadata(metadata: DocumentMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMetadata(metadataList: List<DocumentMetadataEntity>)

    @Update
    suspend fun updateMetadata(metadata: DocumentMetadataEntity)

    @Query("SELECT * FROM document_metadata WHERE documentId = :documentId LIMIT 1")
    fun getMetadataForDocument(documentId: String): Flow<DocumentMetadataEntity?>

    @Query("SELECT * FROM document_metadata WHERE documentId = :documentId LIMIT 1")
    suspend fun getMetadataSync(documentId: String): DocumentMetadataEntity?

    @Query("UPDATE document_metadata SET userNotes = :notes, lastModifiedTimestamp = :timestamp WHERE documentId = :documentId")
    suspend fun updateUserNotes(documentId: String, notes: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE document_metadata SET folderCategory = :category, lastModifiedTimestamp = :timestamp WHERE documentId = :documentId")
    suspend fun updateCategory(documentId: String, category: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE document_metadata SET tags = :tags, lastModifiedTimestamp = :timestamp WHERE documentId = :documentId")
    suspend fun updateTags(documentId: String, tags: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE document_metadata SET isFavorite = :isFavorite WHERE documentId = :documentId")
    suspend fun setFavorite(documentId: String, isFavorite: Boolean)

    @Query("UPDATE document_metadata SET isArchived = :isArchived WHERE documentId = :documentId")
    suspend fun setArchived(documentId: String, isArchived: Boolean)

    @Query("UPDATE document_metadata SET readProgressPercent = :progress, lastViewedTimestamp = :timestamp WHERE documentId = :documentId")
    suspend fun updateReadProgress(documentId: String, progress: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM document_metadata WHERE documentId = :documentId")
    suspend fun deleteMetadataForDocument(documentId: String)

    @Query("SELECT * FROM document_metadata WHERE isFavorite = 1 ORDER BY lastModifiedTimestamp DESC")
    fun getFavoriteMetadata(): Flow<List<DocumentMetadataEntity>>

    @Query("SELECT * FROM document_metadata WHERE isArchived = 1 ORDER BY lastModifiedTimestamp DESC")
    fun getArchivedMetadata(): Flow<List<DocumentMetadataEntity>>

    @Query("""
        SELECT folderCategory, 
               COUNT(*) as documentCount, 
               SUM(fileSizeBytes) as totalWords 
        FROM document_metadata 
        GROUP BY folderCategory
    """)
    fun getCategoryCounts(): Flow<List<CategoryDocCount>>
}
