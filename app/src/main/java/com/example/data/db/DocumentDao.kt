package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentWithFullDetails
import com.example.data.model.DocumentWithOcrRecords
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY timestamp DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE folderCategory = :category ORDER BY timestamp DESC")
    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isOfflinePrivacy = 1 ORDER BY timestamp DESC")
    fun getOfflineDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isArchived = :archived ORDER BY timestamp DESC")
    fun getArchivedDocuments(archived: Boolean = false): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun getDocumentByIdFlow(id: String): Flow<DocumentEntity?>

    @Query("""
        SELECT * FROM documents 
        WHERE extractedText LIKE '%' || :query || '%' 
           OR title LIKE '%' || :query || '%' 
           OR tags LIKE '%' || :query || '%'
           OR userNotes LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId LIMIT 1")
    fun getDocumentWithOcrRecords(documentId: String): Flow<DocumentWithOcrRecords?>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId LIMIT 1")
    fun getDocumentWithFullDetails(documentId: String): Flow<DocumentWithFullDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("DELETE FROM documents WHERE id IN (:ids)")
    suspend fun deleteDocumentsByIds(ids: List<String>)

    @Query("UPDATE documents SET folderCategory = :newCategory, lastModifiedTimestamp = :lastModified WHERE id = :id")
    suspend fun updateCategory(id: String, newCategory: String, lastModified: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET title = :newTitle, lastModifiedTimestamp = :lastModified WHERE id = :id")
    suspend fun renameTitle(id: String, newTitle: String, lastModified: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isCloudSynced = :isSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: String, isSynced: Boolean)

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE documents SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: String, isArchived: Boolean)

    @Query("UPDATE documents SET userNotes = :notes, lastModifiedTimestamp = :lastModified WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String, lastModified: Long = System.currentTimeMillis())

    @Query("""
        UPDATE documents 
        SET extractedText = :text, 
            wordCount = :wordCount, 
            lineCount = :lineCount, 
            characterCount = :characterCount, 
            ocrEngine = :ocrEngine,
            lastModifiedTimestamp = :lastModified 
        WHERE id = :id
    """)
    suspend fun updateExtractedOcrText(
        id: String,
        text: String,
        wordCount: Int,
        lineCount: Int,
        characterCount: Int,
        ocrEngine: String,
        lastModified: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM documents")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT SUM(wordCount) FROM documents")
    fun getTotalWordCount(): Flow<Int?>

    @Query("SELECT DISTINCT folderCategory FROM documents ORDER BY folderCategory ASC")
    fun getAllCategories(): Flow<List<String>>
}

