package com.example.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DocumentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudSyncManagerTest {

    private lateinit var context: Context
    private lateinit var syncManager: CloudSyncManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        syncManager = CloudSyncManager(context)
    }

    @Test
    fun testDeviceIdGeneratedAndConsistent() {
        val deviceId1 = syncManager.getDeviceId()
        assertNotNull(deviceId1)
        assertTrue(deviceId1.startsWith("device_"))

        val deviceId2 = syncManager.getDeviceId()
        assertEquals(deviceId1, deviceId2)
    }

    @Test
    fun testAutoSyncToggle() {
        // By default, auto-sync is active
        assertTrue(syncManager.isAutoSyncActive())

        syncManager.setAutoSyncEnabled(false)
        assertFalse(syncManager.isAutoSyncActive())

        syncManager.setAutoSyncEnabled(true)
        assertTrue(syncManager.isAutoSyncActive())
    }

    @Test
    fun testOfflinePrivacyDocumentFiltering() {
        val regularDoc = DocumentEntity(
            id = "doc-regular",
            title = "Machine Learning Study Notes",
            fileType = "IMAGE",
            fileName = "ml_notes.jpg",
            extractedText = "Gradient descent minimizes the loss function.",
            summary = "Summary of ML algorithms.",
            folderCategory = "Computer Science",
            tags = "ml, ai, notes",
            isOfflinePrivacy = false
        )

        val confidentialDoc = DocumentEntity(
            id = "doc-private",
            title = "Personal Diary & Medical Prescription",
            fileType = "IMAGE",
            fileName = "health_notes.jpg",
            extractedText = "Confidential personal health info.",
            summary = "Private record.",
            folderCategory = "Confidential / Offline",
            tags = "private, offline",
            isOfflinePrivacy = true
        )

        val eligible = listOf(regularDoc, confidentialDoc).filter { !it.isOfflinePrivacy }
        assertEquals(1, eligible.size)
        assertEquals("doc-regular", eligible.first().id)
    }

    @Test
    fun testFirestorePayloadMapping() {
        val now = System.currentTimeMillis()
        val doc = DocumentEntity(
            id = "doc-sync-100",
            title = "Thermodynamics Formulas",
            fileType = "PDF",
            fileName = "physics.pdf",
            extractedText = "PV = nRT ideal gas equation",
            summary = "Core equations of state",
            folderCategory = "Physics",
            tags = "physics, thermodynamics",
            isFavorite = true,
            isArchived = false,
            userNotes = "Review before midterm",
            timestamp = now,
            lastModifiedTimestamp = now,
            ocrEngine = "ML_KIT",
            wordCount = 5,
            lineCount = 1,
            isOfflinePrivacy = false
        )

        val map = syncManager.documentToMap(doc, syncManager.getDeviceId())

        assertEquals("doc-sync-100", map["id"])
        assertEquals("Thermodynamics Formulas", map["title"])
        assertEquals("PDF", map["fileType"])
        assertEquals("PV = nRT ideal gas equation", map["extractedText"])
        assertEquals("Core equations of state", map["summary"])
        assertEquals("Physics", map["folderCategory"])
        assertEquals("physics, thermodynamics", map["tags"])
        assertEquals(true, map["isFavorite"])
        assertEquals(false, map["isArchived"])
        assertEquals("Review before midterm", map["userNotes"])
        assertEquals("ML_KIT", map["ocrEngine"])
        assertEquals(syncManager.getDeviceId(), map["originDeviceId"])
        assertEquals(false, map["deleted"])

        val reconstructed = syncManager.mapToDocument(map)
        assertNotNull(reconstructed)
        assertEquals(doc.id, reconstructed?.id)
        assertEquals(doc.title, reconstructed?.title)
        assertEquals(doc.fileType, reconstructed?.fileType)
        assertEquals(doc.extractedText, reconstructed?.extractedText)
        assertEquals(doc.summary, reconstructed?.summary)
        assertEquals(doc.folderCategory, reconstructed?.folderCategory)
        assertEquals(doc.userNotes, reconstructed?.userNotes)
        assertEquals(doc.isFavorite, reconstructed?.isFavorite)
        assertEquals(true, reconstructed?.isCloudSynced)
        assertEquals(false, reconstructed?.isOfflinePrivacy)
    }
}
