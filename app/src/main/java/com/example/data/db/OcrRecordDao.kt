package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OcrRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for persisting and querying extracted OCR text records,
 * engine transcriptions, execution latency, and block analyses.
 */
@Dao
interface OcrRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrRecord(record: OcrRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrRecords(records: List<OcrRecordEntity>)

    @Update
    suspend fun updateOcrRecord(record: OcrRecordEntity)

    @Query("SELECT * FROM ocr_records WHERE id = :id LIMIT 1")
    suspend fun getOcrRecordById(id: String): OcrRecordEntity?

    @Query("SELECT * FROM ocr_records WHERE documentId = :documentId ORDER BY timestamp DESC")
    fun getOcrRecordsForDocument(documentId: String): Flow<List<OcrRecordEntity>>

    @Query("SELECT * FROM ocr_records WHERE documentId = :documentId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestOcrRecordForDocument(documentId: String): Flow<OcrRecordEntity?>

    @Query("SELECT * FROM ocr_records WHERE documentId = :documentId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestOcrRecordSync(documentId: String): OcrRecordEntity?

    @Query("SELECT * FROM ocr_records WHERE ocrEngine = :engine ORDER BY timestamp DESC")
    fun getOcrRecordsByEngine(engine: String): Flow<List<OcrRecordEntity>>

    @Query("""
        SELECT * FROM ocr_records 
        WHERE rawOcrText LIKE '%' || :query || '%' 
           OR searchableText LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC
    """)
    fun searchOcrRecords(query: String): Flow<List<OcrRecordEntity>>

    @Query("SELECT * FROM ocr_records ORDER BY timestamp DESC")
    fun getAllOcrRecords(): Flow<List<OcrRecordEntity>>

    @Query("DELETE FROM ocr_records WHERE id = :id")
    suspend fun deleteOcrRecordById(id: String)

    @Query("DELETE FROM ocr_records WHERE documentId = :documentId")
    suspend fun deleteOcrRecordsForDocument(documentId: String)

    @Query("SELECT COUNT(*) FROM ocr_records")
    fun getTotalRecordCount(): Flow<Int>

    @Query("SELECT AVG(executionTimeMs) FROM ocr_records WHERE executionTimeMs > 0")
    fun getAverageExecutionTimeMs(): Flow<Double?>

    @Query("SELECT SUM(wordCount) FROM ocr_records")
    fun getTotalWordCount(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM ocr_records WHERE ocrEngine = :engine")
    fun getCountByEngine(engine: String): Flow<Int>
}
