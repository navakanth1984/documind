package com.example.data.repository

import android.graphics.Bitmap
import com.example.ai.GeminiImageResult
import com.example.ai.GeminiOcrResult
import com.example.ai.GeminiService
import com.example.ai.GeminiStudyResponse
import com.example.ai.GeminiTtsResult
import com.example.data.db.DocumentDao
import com.example.data.db.DocumentMetadataDao
import com.example.data.db.OcrRecordDao
import com.example.data.db.StudyDao
import com.example.data.model.CategoryDocCount
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentMetadataEntity
import com.example.data.model.DocumentWithFullDetails
import com.example.data.model.DocumentWithOcrRecords
import com.example.data.model.OcrRecordEntity
import com.example.data.model.StudyChatMessage
import com.example.data.model.StudySubject
import com.example.ocr.MlKitTextRecognitionService
import com.example.ocr.TextRecognitionResult
import com.example.sync.CloudSyncManager
import com.example.sync.SyncResult
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class DocuMindRepository(
    private val documentDao: DocumentDao,
    private val ocrRecordDao: OcrRecordDao,
    private val documentMetadataDao: DocumentMetadataDao,
    private val studyDao: StudyDao,
    private val geminiService: GeminiService,
    private val cloudSyncManager: CloudSyncManager,
    private val mlKitService: MlKitTextRecognitionService = MlKitTextRecognitionService.getInstance()
) {
    val cloudSyncManagerRef: CloudSyncManager get() = cloudSyncManager

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val offlineDocuments: Flow<List<DocumentEntity>> = documentDao.getOfflineDocuments()
    val favoriteDocuments: Flow<List<DocumentEntity>> = documentDao.getFavoriteDocuments()
    val studySubjects: Flow<List<StudySubject>> = studyDao.getAllSubjects()
    val totalDocumentCount: Flow<Int> = documentDao.getDocumentCount()
    val totalWordCount: Flow<Int?> = documentDao.getTotalWordCount()
    val categoryCounts: Flow<List<CategoryDocCount>> = documentMetadataDao.getCategoryCounts()

    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>> =
        documentDao.getDocumentsByCategory(category)

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> =
        documentDao.searchDocuments(query)

    fun getSubjectMessages(subjectId: String): Flow<List<StudyChatMessage>> =
        studyDao.getMessagesForSubject(subjectId)

    suspend fun getDocumentById(id: String): DocumentEntity? =
        documentDao.getDocumentById(id)

    fun getDocumentByIdFlow(id: String): Flow<DocumentEntity?> =
        documentDao.getDocumentByIdFlow(id)

    fun getDocumentWithOcrRecords(documentId: String): Flow<DocumentWithOcrRecords?> =
        documentDao.getDocumentWithOcrRecords(documentId)

    fun getDocumentWithFullDetails(documentId: String): Flow<DocumentWithFullDetails?> =
        documentDao.getDocumentWithFullDetails(documentId)

    fun getOcrRecordsForDocument(documentId: String): Flow<List<OcrRecordEntity>> =
        ocrRecordDao.getOcrRecordsForDocument(documentId)

    fun getLatestOcrRecordForDocument(documentId: String): Flow<OcrRecordEntity?> =
        ocrRecordDao.getLatestOcrRecordForDocument(documentId)

    fun getMetadataForDocument(documentId: String): Flow<DocumentMetadataEntity?> =
        documentMetadataDao.getMetadataForDocument(documentId)

    suspend fun saveDocument(doc: DocumentEntity) {
        documentDao.insertDocument(doc)
        // Also ensure OCR record and metadata exist for persistence
        if (doc.extractedText.isNotBlank()) {
            ocrRecordDao.insertOcrRecord(
                OcrRecordEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = doc.id,
                    rawOcrText = doc.extractedText,
                    searchableText = doc.extractedText,
                    ocrEngine = doc.ocrEngine,
                    wordCount = doc.wordCount,
                    lineCount = doc.lineCount,
                    timestamp = doc.timestamp
                )
            )
        }
        documentMetadataDao.insertOrUpdateMetadata(
            DocumentMetadataEntity(
                documentId = doc.id,
                fileName = doc.fileName,
                fileSizeBytes = doc.fileSizeBytes,
                mimeType = when (doc.fileType) {
                    "PDF" -> "application/pdf"
                    "IMAGE" -> "image/jpeg"
                    "EPUB" -> "application/epub+zip"
                    "VIDEO" -> "video/mp4"
                    else -> "text/plain"
                },
                folderCategory = doc.folderCategory,
                tags = doc.tags,
                isFavorite = doc.isFavorite,
                isArchived = doc.isArchived,
                userNotes = doc.userNotes,
                creationTimestamp = doc.timestamp,
                lastModifiedTimestamp = doc.lastModifiedTimestamp
            )
        )

        if (!doc.isOfflinePrivacy && cloudSyncManager.isAutoSyncActive()) {
            cloudSyncManager.backupSingleDocument(doc)
            documentDao.updateSyncStatus(doc.id, true)
        }
    }

    suspend fun deleteDocument(id: String) {
        documentDao.deleteDocumentById(id)
        ocrRecordDao.deleteOcrRecordsForDocument(id)
        documentMetadataDao.deleteMetadataForDocument(id)
        cloudSyncManager.deleteDocumentFromCloud(id)
    }

    suspend fun deleteDocuments(ids: List<String>) {
        documentDao.deleteDocumentsByIds(ids)
        ids.forEach { id ->
            ocrRecordDao.deleteOcrRecordsForDocument(id)
            documentMetadataDao.deleteMetadataForDocument(id)
            cloudSyncManager.deleteDocumentFromCloud(id)
        }
    }

    private suspend fun syncUpdatedDocToCloud(id: String) {
        if (cloudSyncManager.isAutoSyncActive()) {
            val doc = documentDao.getDocumentById(id)
            if (doc != null && !doc.isOfflinePrivacy) {
                cloudSyncManager.backupSingleDocument(doc)
            }
        }
    }

    suspend fun updateCategory(id: String, newCategory: String) {
        documentDao.updateCategory(id, newCategory)
        documentMetadataDao.updateCategory(id, newCategory)
        syncUpdatedDocToCloud(id)
    }

    suspend fun renameDocument(id: String, newTitle: String) {
        documentDao.renameTitle(id, newTitle)
        syncUpdatedDocToCloud(id)
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        documentDao.toggleFavorite(id, isFavorite)
        documentMetadataDao.setFavorite(id, isFavorite)
        syncUpdatedDocToCloud(id)
    }

    suspend fun setArchived(id: String, isArchived: Boolean) {
        documentDao.setArchived(id, isArchived)
        documentMetadataDao.setArchived(id, isArchived)
        syncUpdatedDocToCloud(id)
    }

    suspend fun updateNotes(id: String, notes: String) {
        documentDao.updateNotes(id, notes)
        documentMetadataDao.updateUserNotes(id, notes)
        syncUpdatedDocToCloud(id)
    }

    suspend fun updateTags(id: String, tags: String) {
        documentMetadataDao.updateTags(id, tags)
    }

    suspend fun updateReadProgress(id: String, progress: Int) {
        documentMetadataDao.updateReadProgress(id, progress)
    }

    suspend fun extractAndSaveDocument(
        fileName: String,
        fileType: String,
        rawContent: String,
        isBase64Image: Boolean,
        isDeepAnalysis: Boolean,
        isPrivacyOffline: Boolean
    ): DocumentEntity {
        val startTime = System.currentTimeMillis()
        val ocrResult: GeminiOcrResult = if (isDeepAnalysis && isBase64Image) {
            geminiService.analyzeImageDeep(rawContent, "Analyze document notes and transcribe full text accurately.")
        } else if (fileType == "VIDEO") {
            geminiService.analyzeVideoContent(fileName, rawContent)
        } else {
            geminiService.extractDocumentFast(fileName, fileType, rawContent, isBase64Image)
        }
        val elapsedMs = System.currentTimeMillis() - startTime

        val lines = ocrResult.extractedText.lines()
        val wordCount = ocrResult.extractedText.split(Regex("\\s+")).count { it.isNotBlank() }
        val lineCount = lines.size
        val characterCount = ocrResult.extractedText.length
        val finalCategory = if (isPrivacyOffline) "Confidential / Offline" else ocrResult.suggestedCategory
        val docId = UUID.randomUUID().toString()
        val engine = if (isDeepAnalysis) "GEMINI_DEEP" else "GEMINI_FAST"
        val now = System.currentTimeMillis()

        val entity = DocumentEntity(
            id = docId,
            title = ocrResult.title,
            fileType = fileType,
            fileName = fileName,
            extractedText = ocrResult.extractedText,
            summary = ocrResult.summary,
            folderCategory = finalCategory,
            tags = ocrResult.tags.joinToString(", "),
            isOfflinePrivacy = isPrivacyOffline,
            isCloudSynced = false,
            wordCount = wordCount,
            characterCount = characterCount,
            lineCount = lineCount,
            ocrEngine = engine,
            confidenceScore = 0.98f,
            timestamp = now,
            lastModifiedTimestamp = now,
            imageBase64 = if (isBase64Image) rawContent else null
        )

        // 1. Persist Document entity
        documentDao.insertDocument(entity)

        // 2. Persist dedicated OCR Text Record
        val ocrRecord = OcrRecordEntity(
            id = UUID.randomUUID().toString(),
            documentId = docId,
            rawOcrText = ocrResult.extractedText,
            searchableText = ocrResult.extractedText,
            ocrEngine = engine,
            executionTimeMs = elapsedMs,
            confidenceScore = 0.98f,
            wordCount = wordCount,
            lineCount = lineCount,
            detectedLanguage = "en",
            timestamp = now
        )
        ocrRecordDao.insertOcrRecord(ocrRecord)

        // 3. Persist dedicated Document Metadata Entity
        val metadata = DocumentMetadataEntity(
            documentId = docId,
            fileName = fileName,
            fileSizeBytes = rawContent.length.toLong(),
            mimeType = when (fileType) {
                "PDF" -> "application/pdf"
                "IMAGE" -> "image/jpeg"
                "EPUB" -> "application/epub+zip"
                "VIDEO" -> "video/mp4"
                else -> "text/plain"
            },
            pageCount = 1,
            folderCategory = finalCategory,
            tags = ocrResult.tags.joinToString(", "),
            creationTimestamp = now,
            lastModifiedTimestamp = now,
            lastViewedTimestamp = now
        )
        documentMetadataDao.insertOrUpdateMetadata(metadata)

        if (!entity.isOfflinePrivacy && cloudSyncManager.isAutoSyncActive()) {
            cloudSyncManager.backupSingleDocument(entity)
            documentDao.updateSyncStatus(entity.id, true)
        }

        return entity
    }

    /**
     * Process an image using Google ML Kit on-device Text Recognition
     * to extract searchable string text without network access and persist locally in Room DB.
     */
    suspend fun extractDocumentWithMlKit(
        fileName: String,
        base64Image: String,
        isPrivacyOffline: Boolean
    ): DocumentEntity {
        val result = mlKitService.processBase64(base64Image)
        val textResult = result.getOrNull()
        val extractedText = textResult?.text ?: ""
        val elapsedMs = textResult?.executionTimeMs ?: 0L

        val lines = extractedText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val title = lines.firstOrNull()?.take(60)?.ifBlank { null }
            ?: fileName.substringBeforeLast(".")
        val wordCount = extractedText.split(Regex("\\s+")).count { it.isNotBlank() }
        val lineCount = lines.size
        val characterCount = extractedText.length
        val summary = if (extractedText.length > 200) {
            extractedText.take(197) + "..."
        } else {
            extractedText.ifBlank { "Extracted document via on-device ML Kit OCR." }
        }

        val category = if (isPrivacyOffline) {
            "Confidential / Offline"
        } else {
            when {
                extractedText.contains("math", true) || extractedText.contains("equation", true) || extractedText.contains("integral", true) -> "Mathematics"
                extractedText.contains("biology", true) || extractedText.contains("cell", true) || extractedText.contains("dna", true) -> "Biology"
                extractedText.contains("code", true) || extractedText.contains("algorithm", true) || extractedText.contains("function", true) -> "Computer Science"
                extractedText.contains("physics", true) || extractedText.contains("force", true) || extractedText.contains("energy", true) -> "Physics"
                extractedText.contains("history", true) || extractedText.contains("century", true) || extractedText.contains("war", true) -> "History"
                else -> "Document Scans"
            }
        }

        val docId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val entity = DocumentEntity(
            id = docId,
            title = title,
            fileType = "IMAGE",
            fileName = fileName,
            extractedText = extractedText,
            summary = summary,
            folderCategory = category,
            tags = "ml-kit, on-device, camera-scan",
            isOfflinePrivacy = isPrivacyOffline,
            isCloudSynced = false,
            wordCount = wordCount,
            characterCount = characterCount,
            lineCount = lineCount,
            ocrEngine = "ML_KIT",
            confidenceScore = 1.0f,
            timestamp = now,
            lastModifiedTimestamp = now,
            imageBase64 = base64Image
        )

        // 1. Persist Document Entity in Room
        documentDao.insertDocument(entity)

        // 2. Persist OCR Record in Room
        val ocrRecord = OcrRecordEntity(
            id = UUID.randomUUID().toString(),
            documentId = docId,
            rawOcrText = extractedText,
            searchableText = extractedText,
            ocrEngine = "ML_KIT",
            executionTimeMs = elapsedMs,
            confidenceScore = 1.0f,
            wordCount = wordCount,
            lineCount = lineCount,
            blockCount = textResult?.blocks?.size ?: 0,
            detectedLanguage = "en",
            timestamp = now
        )
        ocrRecordDao.insertOcrRecord(ocrRecord)

        // 3. Persist Document Metadata Entity in Room
        val metadata = DocumentMetadataEntity(
            documentId = docId,
            fileName = fileName,
            fileSizeBytes = (base64Image.length * 3L) / 4L,
            mimeType = "image/jpeg",
            pageCount = 1,
            scanResolution = if (textResult?.blocks?.isNotEmpty() == true) "CameraX High-Res" else null,
            folderCategory = category,
            tags = "ml-kit, on-device, camera-scan",
            creationTimestamp = now,
            lastModifiedTimestamp = now,
            lastViewedTimestamp = now
        )
        documentMetadataDao.insertOrUpdateMetadata(metadata)

        if (!entity.isOfflinePrivacy && cloudSyncManager.isAutoSyncActive()) {
            cloudSyncManager.backupSingleDocument(entity)
            documentDao.updateSyncStatus(entity.id, true)
        }

        return entity
    }


    suspend fun recognizeTextWithMlKit(base64Image: String): Result<TextRecognitionResult> =
        mlKitService.processBase64(base64Image)

    suspend fun recognizeTextFromBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): Result<TextRecognitionResult> =
        mlKitService.processBitmap(bitmap, rotationDegrees)

    suspend fun recognizeTextFromFile(file: File, rotationDegrees: Int = 0): Result<TextRecognitionResult> =
        mlKitService.processFile(file, rotationDegrees)

    suspend fun generateTts(text: String): GeminiTtsResult =
        geminiService.textToSpeech(text)

    suspend fun generateOrEditDiagram(prompt: String, inputImageBase64: String? = null): GeminiImageResult =
        geminiService.generateOrEditDiagram(prompt, inputImageBase64)

    suspend fun askHighThinking(question: String, documentContext: String): GeminiStudyResponse =
        geminiService.askWithHighThinking(question, documentContext)

    suspend fun askSearchGrounding(question: String, subject: String): GeminiStudyResponse =
        geminiService.askWithSearchGrounding(question, subject)

    suspend fun askMapsGrounding(query: String): GeminiStudyResponse =
        geminiService.askWithMapsGrounding(query)

    suspend fun addStudySubject(subject: StudySubject) {
        studyDao.insertSubject(subject)
    }

    suspend fun addChatMessage(message: StudyChatMessage) {
        studyDao.insertMessage(message)
    }

    suspend fun syncWithCloud(documents: List<DocumentEntity>): SyncResult {
        val res = cloudSyncManager.syncDocumentsToCloud(documents)
        if (res.success) {
            documents.filter { !it.isOfflinePrivacy }.forEach { doc ->
                documentDao.updateSyncStatus(doc.id, true)
            }
        }
        return res
    }

    /**
     * Initializes bidirectional real-time Firestore listener for live cross-device sync.
     */
    fun initRealtimeCloudSync() {
        cloudSyncManager.startRealtimeSync(
            onDocumentReceived = { remoteDoc ->
                val localDoc = documentDao.getDocumentById(remoteDoc.id)
                if (localDoc == null || remoteDoc.lastModifiedTimestamp > localDoc.lastModifiedTimestamp) {
                    documentDao.insertDocument(remoteDoc.copy(isCloudSynced = true))
                    if (remoteDoc.extractedText.isNotBlank()) {
                        ocrRecordDao.insertOcrRecord(
                            OcrRecordEntity(
                                id = UUID.randomUUID().toString(),
                                documentId = remoteDoc.id,
                                rawOcrText = remoteDoc.extractedText,
                                searchableText = remoteDoc.extractedText,
                                ocrEngine = remoteDoc.ocrEngine,
                                wordCount = remoteDoc.wordCount,
                                lineCount = remoteDoc.lineCount,
                                timestamp = remoteDoc.timestamp
                            )
                        )
                    }
                    documentMetadataDao.insertOrUpdateMetadata(
                        DocumentMetadataEntity(
                            documentId = remoteDoc.id,
                            fileName = remoteDoc.fileName,
                            fileSizeBytes = remoteDoc.fileSizeBytes,
                            folderCategory = remoteDoc.folderCategory,
                            tags = remoteDoc.tags,
                            userNotes = remoteDoc.userNotes,
                            isFavorite = remoteDoc.isFavorite,
                            isArchived = remoteDoc.isArchived,
                            creationTimestamp = remoteDoc.timestamp,
                            lastModifiedTimestamp = remoteDoc.lastModifiedTimestamp
                        )
                    )
                }
            },
            onDocumentRemoved = { docId ->
                documentDao.deleteDocumentById(docId)
                ocrRecordDao.deleteOcrRecordsForDocument(docId)
                documentMetadataDao.deleteMetadataForDocument(docId)
            }
        )
    }

    /**
     * Restores all remote documents from Firestore for the user and merges them into Room.
     */
    suspend fun restoreFromCloud(): SyncResult {
        val remoteDocs = cloudSyncManager.restoreDocumentsFromCloud()
        if (remoteDocs.isEmpty()) {
            return SyncResult(true, 0, "No remote documents found in Firestore cloud.")
        }
        var restored = 0
        for (doc in remoteDocs) {
            val local = documentDao.getDocumentById(doc.id)
            if (local == null || doc.lastModifiedTimestamp > local.lastModifiedTimestamp) {
                documentDao.insertDocument(doc.copy(isCloudSynced = true))
                if (doc.extractedText.isNotBlank()) {
                    ocrRecordDao.insertOcrRecord(
                        OcrRecordEntity(
                            id = UUID.randomUUID().toString(),
                            documentId = doc.id,
                            rawOcrText = doc.extractedText,
                            searchableText = doc.extractedText,
                            ocrEngine = doc.ocrEngine,
                            wordCount = doc.wordCount,
                            lineCount = doc.lineCount,
                            timestamp = doc.timestamp
                        )
                    )
                }
                documentMetadataDao.insertOrUpdateMetadata(
                    DocumentMetadataEntity(
                        documentId = doc.id,
                        fileName = doc.fileName,
                        fileSizeBytes = doc.fileSizeBytes,
                        folderCategory = doc.folderCategory,
                        tags = doc.tags,
                        userNotes = doc.userNotes,
                        isFavorite = doc.isFavorite,
                        isArchived = doc.isArchived,
                        creationTimestamp = doc.timestamp,
                        lastModifiedTimestamp = doc.lastModifiedTimestamp
                    )
                )
                restored++
            }
        }
        return SyncResult(
            success = true,
            syncedCount = restored,
            message = "Restored $restored document(s) from Firestore cloud to local Room DB."
        )
    }

    suspend fun populateInitialDataIfEmpty(currentCount: Int) {
        if (currentCount > 0) return

        val sample1 = DocumentEntity(
            id = "sample-doc-1",
            title = "Neural Networks & Backpropagation Notes",
            fileType = "PDF",
            fileName = "cs441_neural_nets_lecture4.pdf",
            extractedText = """
                Lecture 4: Gradient Descent and Multilayer Perceptrons.
                Definition: Backpropagation is an efficient algorithm to compute gradients of the loss function with respect to weights using the chain rule of calculus.
                Forward Pass: z^[l] = W^[l] * a^[l-1] + b^[l], a^[l] = sigma(z^[l]).
                Backward Pass: delta^[l] = (W^[l+1]^T * delta^[l+1]) * sigma'(z^[l]).
                Key Applications: Deep computer vision, sequence models, and transformer attention matrices.
                Recommended reading: Goodfellow Deep Learning Chapter 6.
            """.trimIndent(),
            summary = "Comprehensive lecture transcription detailing backpropagation chain rule computations and forward-pass layer activations.",
            folderCategory = "Computer Science",
            tags = "machine-learning, backpropagation, neural-networks, cs441",
            isOfflinePrivacy = false,
            isCloudSynced = true,
            wordCount = 74,
            timestamp = System.currentTimeMillis() - 86400000L * 2
        )

        val sample2 = DocumentEntity(
            id = "sample-doc-2",
            title = "Cellular Respiration & Krebs Cycle",
            fileType = "IMAGE",
            fileName = "biology_lab_diagram_scan.png",
            extractedText = """
                Laboratory Diagram OCR Analysis:
                Phase 1: Glycolysis occurs in cytoplasm. 1 Glucose (6C) -> 2 Pyruvate (3C) + 2 ATP net + 2 NADH.
                Phase 2: Citric Acid Cycle (Krebs) in mitochondrial matrix. Acetyl-CoA combines with Oxaloacetate to form Citrate. Yields 2 ATP, 6 NADH, 2 FADH2 per glucose.
                Phase 3: Oxidative Phosphorylation via Electron Transport Chain (ETC) across cristae inner membrane. ATP Synthase produces ~32-34 ATP.
            """.trimIndent(),
            summary = "Handwritten diagram transcription outlining glycolysis, Krebs cycle biochemical steps, and ATP energy yields.",
            folderCategory = "Biology",
            tags = "krebs-cycle, cellular-respiration, atp, biology101",
            isOfflinePrivacy = false,
            isCloudSynced = true,
            wordCount = 72,
            timestamp = System.currentTimeMillis() - 86400000L
        )

        val sample3 = DocumentEntity(
            id = "sample-doc-3",
            title = "The Republic - Book VII Allegory of the Cave",
            fileType = "EPUB",
            fileName = "plato_republic_oxford_edition.epub",
            extractedText = """
                Chapter VII: The Allegory of the Cave.
                "Behold! Human beings living in an underground den, which has a mouth open towards the light and reaching across the whole den; here they have been from their childhood, and have their legs and necks chained so that they cannot move, and can only look before them, being prevented by the chains from turning round their heads..."
                Philosophical Analysis: Explores the journey from perceptual ignorance (shadows on the cave wall) to intellectual enlightenment through dialectic education.
            """.trimIndent(),
            summary = "Classic philosophical text analysis contrasting sensory illusions with intelligible truth in Platonic philosophy.",
            folderCategory = "Literature",
            tags = "philosophy, plato, literature, allegory",
            isOfflinePrivacy = false,
            isCloudSynced = false,
            wordCount = 89,
            timestamp = System.currentTimeMillis() - 43200000L
        )

        val sample4 = DocumentEntity(
            id = "sample-doc-4",
            title = "Confidential Final Exam Review & Answer Key",
            fileType = "TEXT",
            fileName = "math301_confidential_midterm_draft.txt",
            extractedText = """
                [RESTRICTED / PRIVACY PROTECTED]
                Calculus III Final Exam Draft Solutions:
                Problem 1: Stokes Theorem integral evaluation around closed boundary contour C.
                Line integral equals surface integral of curl(F) dot dS.
                Problem 2: Lagrange Multipliers with constrained optimization on ellipsoid x^2 + 2y^2 + 3z^2 = 1.
                Note: Kept locally in offline privacy mode. No cloud upload permitted.
            """.trimIndent(),
            summary = "Private calculus examination keys marked strictly for offline storage without cross-device cloud sync.",
            folderCategory = "Confidential / Offline",
            tags = "offline-only, private, calculus, exam-prep",
            isOfflinePrivacy = true,
            isCloudSynced = false,
            wordCount = 59,
            timestamp = System.currentTimeMillis() - 12000000L
        )

        val sample5 = DocumentEntity(
            id = "sample-doc-5",
            title = "Quantum Computing Lecture - Qubit Superposition",
            fileType = "VIDEO",
            fileName = "mit_lecture_quantum_state_vectors.mp4",
            extractedText = """
                Video Transcription & Key Moments:
                00:00 - Introduction to Hilbert Space and state vectors |0> and |1>.
                04:15 - Bloch Sphere representation and Hadamard Gate transformation: H|0> = (|0> + |1>) / sqrt(2).
                12:40 - Measurement postulate and wave function collapse.
                Key Question for Students: How does quantum parallelism differentiate from classical probabilistic algorithms?
            """.trimIndent(),
            summary = "Video lecture summary breaking down quantum state vectors, superposition principles, and Bloch sphere geometry.",
            folderCategory = "Physics",
            tags = "quantum, physics, video-lecture, mit",
            isOfflinePrivacy = false,
            isCloudSynced = true,
            wordCount = 67,
            timestamp = System.currentTimeMillis() - 3600000L
        )

        val samples = listOf(sample1, sample2, sample3, sample4, sample5)
        documentDao.insertDocuments(samples)

        // Persist initial OCR text records
        val initialOcrRecords = samples.map { doc ->
            OcrRecordEntity(
                id = UUID.randomUUID().toString(),
                documentId = doc.id,
                rawOcrText = doc.extractedText,
                searchableText = doc.extractedText,
                ocrEngine = if (doc.fileType == "IMAGE") "ML_KIT" else "GEMINI_FAST",
                executionTimeMs = 120L,
                confidenceScore = 0.99f,
                wordCount = doc.wordCount,
                lineCount = doc.extractedText.lines().size,
                detectedLanguage = "en",
                timestamp = doc.timestamp
            )
        }
        ocrRecordDao.insertOcrRecords(initialOcrRecords)

        // Persist initial document metadata
        val initialMetadata = samples.map { doc ->
            DocumentMetadataEntity(
                documentId = doc.id,
                fileName = doc.fileName,
                fileSizeBytes = doc.extractedText.length * 2L,
                mimeType = when (doc.fileType) {
                    "PDF" -> "application/pdf"
                    "IMAGE" -> "image/jpeg"
                    "EPUB" -> "application/epub+zip"
                    "VIDEO" -> "video/mp4"
                    else -> "text/plain"
                },
                pageCount = 1,
                folderCategory = doc.folderCategory,
                tags = doc.tags,
                creationTimestamp = doc.timestamp,
                lastModifiedTimestamp = doc.timestamp,
                lastViewedTimestamp = doc.timestamp,
                isFavorite = (doc.id == "sample-doc-1")
            )
        }
        documentMetadataDao.insertAllMetadata(initialMetadata)


        // Populate initial study subject
        val subject = StudySubject(
            id = "study-sub-1",
            subjectName = "Advanced AI & Machine Learning",
            title = "Neural Networks Exam Preparation Group",
            description = "Collaborative Q&A session discussing Backpropagation, Loss functions, and Gradient Descent convergence.",
            linkedDocumentId = "sample-doc-1",
            hostRole = "Teacher",
            participantCount = 6,
            timestamp = System.currentTimeMillis()
        )
        studyDao.insertSubject(subject)

        studyDao.insertMessage(
            StudyChatMessage(
                id = UUID.randomUUID().toString(),
                subjectId = "study-sub-1",
                senderName = "Prof. Davis",
                senderRole = "Teacher",
                message = "Welcome everyone. Please review Lecture 4 extracted notes on the chain rule backward pass. What questions do you have on computing delta^[l]?",
                timestamp = System.currentTimeMillis() - 3600000L
            )
        )

        studyDao.insertMessage(
            StudyChatMessage(
                id = UUID.randomUUID().toString(),
                subjectId = "study-sub-1",
                senderName = "Student Alex",
                senderRole = "Student",
                message = "Why do we multiply by sigma'(z^[l]) component-wise (Hadamard product) rather than standard matrix multiplication during backprop?",
                timestamp = System.currentTimeMillis() - 1800000L
            )
        )

        studyDao.insertMessage(
            StudyChatMessage(
                id = UUID.randomUUID().toString(),
                subjectId = "study-sub-1",
                senderName = "DocuMind AI Tutor",
                senderRole = "AI Tutor",
                message = "Because each activation unit a_i^[l] only depends on its corresponding net input z_i^[l]. By the multivariable chain rule, ∂L/∂z_i = (∂L/∂a_i) * (da_i/dz_i), which maps entry-to-entry.",
                thinkingText = "Chain rule breakdown:\n1. ∂L/∂z_i = sum_k (∂L/∂z_k^[l+1] * ∂z_k^[l+1]/∂a_i) * ∂a_i/∂z_i\n2. The activation function a_i = sigma(z_i) is evaluated element-wise.\n3. Hence the derivative is diagonal / Hadamard product.",
                searchSources = "Deep Learning (Goodfellow et al., MIT Press), Backpropagation Mechanics",
                timestamp = System.currentTimeMillis() - 600000L
            )
        )
    }
}
