package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.DocuMindDatabase
import com.example.data.db.DocumentDao
import com.example.data.db.DocumentMetadataDao
import com.example.data.db.OcrRecordDao
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentMetadataEntity
import com.example.data.model.OcrRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {

    private lateinit var db: DocuMindDatabase
    private lateinit var documentDao: DocumentDao
    private lateinit var ocrRecordDao: OcrRecordDao
    private lateinit var metadataDao: DocumentMetadataDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DocuMindDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        documentDao = db.documentDao()
        ocrRecordDao = db.ocrRecordDao()
        metadataDao = db.documentMetadataDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPersistAndRetrieveExtractedOcrText() = runBlocking {
        val docId = "doc-mlkit-101"
        val extractedOcr = "Quantum Superposition & Entanglement: Qubits exist in linear combination of states."

        val doc = DocumentEntity(
            id = docId,
            title = "Quantum Physics Notes",
            fileType = "IMAGE",
            fileName = "camera_scan_page1.jpg",
            extractedText = extractedOcr,
            summary = "Summary of quantum computing principles",
            folderCategory = "Physics",
            tags = "quantum, ml-kit, ocr",
            wordCount = 10,
            lineCount = 1,
            ocrEngine = "ML_KIT"
        )
        documentDao.insertDocument(doc)

        // Persist extracted OCR record
        val ocrRecord = OcrRecordEntity(
            id = "ocr-rec-1",
            documentId = docId,
            rawOcrText = extractedOcr,
            searchableText = extractedOcr,
            ocrEngine = "ML_KIT",
            executionTimeMs = 45L,
            wordCount = 10,
            lineCount = 1,
            blockCount = 2
        )
        ocrRecordDao.insertOcrRecord(ocrRecord)

        // Verify retrieval via DocumentDao
        val retrievedDoc = documentDao.getDocumentById(docId)
        assertNotNull(retrievedDoc)
        assertEquals("Quantum Physics Notes", retrievedDoc?.title)
        assertEquals("ML_KIT", retrievedDoc?.ocrEngine)

        // Verify retrieval via OcrRecordDao
        val ocrRecords = ocrRecordDao.getOcrRecordsForDocument(docId).first()
        assertEquals(1, ocrRecords.size)
        assertEquals(extractedOcr, ocrRecords[0].rawOcrText)
        assertEquals("ML_KIT", ocrRecords[0].ocrEngine)
        assertEquals(45L, ocrRecords[0].executionTimeMs)

        // Verify OCR text search
        val searchResults = ocrRecordDao.searchOcrRecords("Superposition").first()
        assertEquals(1, searchResults.size)
        assertEquals(docId, searchResults[0].documentId)
    }

    @Test
    fun testDocumentMetadataManagement() = runBlocking {
        val docId = "doc-meta-202"
        val doc = DocumentEntity(
            id = docId,
            title = "Organic Chemistry Reactions",
            fileType = "PDF",
            fileName = "chem202_reactions.pdf",
            extractedText = "Electrophilic aromatic substitution mechanism with benzene ring.",
            summary = "EAS mechanisms",
            folderCategory = "Chemistry",
            tags = "chemistry, reactions"
        )
        documentDao.insertDocument(doc)

        val metadata = DocumentMetadataEntity(
            documentId = docId,
            fileName = "chem202_reactions.pdf",
            fileSizeBytes = 204800L,
            mimeType = "application/pdf",
            pageCount = 12,
            folderCategory = "Chemistry",
            tags = "organic, mechanism",
            userNotes = "Review before exam next Friday",
            isFavorite = false
        )
        metadataDao.insertOrUpdateMetadata(metadata)

        // Retrieve metadata
        val retrievedMeta = metadataDao.getMetadataSync(docId)
        assertNotNull(retrievedMeta)
        assertEquals("Review before exam next Friday", retrievedMeta?.userNotes)
        assertEquals(12, retrievedMeta?.pageCount)

        // Update user notes and favorite status
        metadataDao.updateUserNotes(docId, "Updated notes: memorize nitration reaction conditions")
        metadataDao.setFavorite(docId, true)

        val updatedMeta = metadataDao.getMetadataSync(docId)
        assertEquals("Updated notes: memorize nitration reaction conditions", updatedMeta?.userNotes)
        assertTrue(updatedMeta?.isFavorite == true)

        // Test favorites query
        val favorites = metadataDao.getFavoriteMetadata().first()
        assertEquals(1, favorites.size)
        assertEquals(docId, favorites[0].documentId)
    }

    @Test
    fun testCascadingDeletionRemovesOcrAndMetadata() = runBlocking {
        val docId = "doc-cascade-303"
        val doc = DocumentEntity(
            id = docId,
            title = "Temporary Scratchpad",
            fileType = "IMAGE",
            fileName = "scratch.jpg",
            extractedText = "Temporary scratchpad formula calculations.",
            summary = "Scratchpad",
            folderCategory = "General",
            tags = "temp"
        )
        documentDao.insertDocument(doc)

        ocrRecordDao.insertOcrRecord(
            OcrRecordEntity(
                id = "ocr-303",
                documentId = docId,
                rawOcrText = doc.extractedText,
                ocrEngine = "ML_KIT"
            )
        )

        metadataDao.insertOrUpdateMetadata(
            DocumentMetadataEntity(
                documentId = docId,
                fileName = "scratch.jpg",
                userNotes = "Draft only"
            )
        )

        // Verify inserted
        assertEquals(1, ocrRecordDao.getOcrRecordsForDocument(docId).first().size)
        assertNotNull(metadataDao.getMetadataSync(docId))

        // Delete parent document
        documentDao.deleteDocumentById(docId)

        // Foreign keys with CASCADE will remove child records
        val remainingOcr = ocrRecordDao.getOcrRecordsForDocument(docId).first()
        assertEquals(0, remainingOcr.size)
    }
}
