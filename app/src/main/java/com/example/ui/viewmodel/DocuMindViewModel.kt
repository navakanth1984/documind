package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiImageResult
import com.example.ai.GeminiService
import com.example.ai.GeminiStudyResponse
import com.example.audio.AudioPlayer
import com.example.data.db.DocuMindDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.StudyChatMessage
import com.example.data.model.StudySubject
import com.example.data.repository.DocuMindRepository
import com.example.ocr.TextRecognitionResult
import com.example.sync.CloudSyncManager
import com.example.sync.SyncLogEntry
import com.example.sync.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class AiStudyMode {
    HIGH_THINKING,
    SEARCH_GROUNDING,
    MAPS_GROUNDING,
    QUICK_LITE
}

class DocuMindViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DocuMindDatabase.getDatabase(application)
    private val geminiService = GeminiService()
    private val cloudSyncManager = CloudSyncManager(application)
    val audioPlayer = AudioPlayer(application)

    val repository = DocuMindRepository(
        documentDao = db.documentDao(),
        ocrRecordDao = db.ocrRecordDao(),
        documentMetadataDao = db.documentMetadataDao(),
        studyDao = db.studyDao(),
        geminiService = geminiService,
        cloudSyncManager = cloudSyncManager
    )

    val favoriteDocuments: StateFlow<List<DocumentEntity>> = repository.favoriteDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryCounts = repository.categoryCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isPrivacyOfflineFilter = MutableStateFlow(false)
    val isPrivacyOfflineFilter: StateFlow<Boolean> = _isPrivacyOfflineFilter.asStateFlow()

    private val _selectedDocument = MutableStateFlow<DocumentEntity?>(null)
    val selectedDocument: StateFlow<DocumentEntity?> = _selectedDocument.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _extractStatusMessage = MutableStateFlow<String?>(null)
    val extractStatusMessage: StateFlow<String?> = _extractStatusMessage.asStateFlow()

    private val _isTtsLoading = MutableStateFlow(false)
    val isTtsLoading: StateFlow<Boolean> = _isTtsLoading.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _selectedSubject = MutableStateFlow<StudySubject?>(null)
    val selectedSubject: StateFlow<StudySubject?> = _selectedSubject.asStateFlow()

    private val _isAskingAi = MutableStateFlow(false)
    val isAskingAi: StateFlow<Boolean> = _isAskingAi.asStateFlow()

    private val _isGeneratingDiagram = MutableStateFlow(false)
    val isGeneratingDiagram: StateFlow<Boolean> = _isGeneratingDiagram.asStateFlow()

    private val _diagramResult = MutableStateFlow<GeminiImageResult?>(null)
    val diagramResult: StateFlow<GeminiImageResult?> = _diagramResult.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isUserSignedIn = MutableStateFlow(cloudSyncManager.isUserSignedIn)
    val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = cloudSyncManager.syncStatus
    val syncEventsLog: StateFlow<List<SyncLogEntry>> = cloudSyncManager.syncEventsLog
    val isAutoSyncEnabled: StateFlow<Boolean> = cloudSyncManager.isAutoSyncEnabled
    val lastSyncTimestamp: StateFlow<Long?> = cloudSyncManager.lastSyncTimestamp
    val deviceId: String get() = cloudSyncManager.getDeviceId()
    val effectiveUserId: String get() = cloudSyncManager.getEffectiveUserId()

    private val _mlKitOcrResult = MutableStateFlow<TextRecognitionResult?>(null)
    val mlKitOcrResult: StateFlow<TextRecognitionResult?> = _mlKitOcrResult.asStateFlow()

    private val _isMlKitProcessing = MutableStateFlow(false)
    val isMlKitProcessing: StateFlow<Boolean> = _isMlKitProcessing.asStateFlow()

    val allDocuments: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredDocuments: StateFlow<List<DocumentEntity>> = combine(
        allDocuments,
        _selectedCategory,
        _searchQuery,
        _isPrivacyOfflineFilter
    ) { docs, category, query, privacyFilter ->
        docs.filter { doc ->
            val matchesCategory = if (category == "All") true else doc.folderCategory.equals(category, ignoreCase = true)
            val matchesPrivacy = if (privacyFilter) doc.isOfflinePrivacy else true
            val matchesQuery = if (query.isBlank()) true else {
                doc.title.contains(query, ignoreCase = true) ||
                        doc.extractedText.contains(query, ignoreCase = true) ||
                        doc.tags.contains(query, ignoreCase = true)
            }
            matchesCategory && matchesPrivacy && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studySubjects: StateFlow<List<StudySubject>> = repository.studySubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSubjectMessages: StateFlow<List<StudyChatMessage>> = _selectedSubject
        .flatMapLatest { subject ->
            if (subject == null) MutableStateFlow(emptyList())
            else repository.getSubjectMessages(subject.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty(allDocuments.value.size)
        }
        if (cloudSyncManager.isAutoSyncActive()) {
            repository.initRealtimeCloudSync()
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePrivacyFilter(enabled: Boolean) {
        _isPrivacyOfflineFilter.value = enabled
    }

    fun selectDocument(doc: DocumentEntity?) {
        _selectedDocument.value = doc
    }

    fun selectSubject(subject: StudySubject?) {
        _selectedSubject.value = subject
    }

    fun extractDocument(
        fileName: String,
        fileType: String,
        content: String,
        isBase64Image: Boolean,
        isDeepAnalysis: Boolean,
        isPrivacyOffline: Boolean,
        onSuccess: (DocumentEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isExtracting.value = true
            _extractStatusMessage.value = "Extracting text with OCR..."
            try {
                val doc = repository.extractAndSaveDocument(
                    fileName = fileName,
                    fileType = fileType,
                    rawContent = content,
                    isBase64Image = isBase64Image,
                    isDeepAnalysis = isDeepAnalysis,
                    isPrivacyOffline = isPrivacyOffline
                )
                _extractStatusMessage.value = "Document categorized into ${doc.folderCategory}"
                selectDocument(doc)
                onSuccess(doc)
            } catch (e: Exception) {
                _extractStatusMessage.value = "Error: ${e.message}"
            } finally {
                _isExtracting.value = false
            }
        }
    }

    /**
     * Process image with ML Kit on-device Text Recognition
     */
    fun processImageWithMlKit(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        onComplete: (TextRecognitionResult?) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMlKitProcessing.value = true
            try {
                val result = repository.recognizeTextFromBitmap(bitmap, rotationDegrees)
                val recognition = result.getOrNull()
                _mlKitOcrResult.value = recognition
                onComplete(recognition)
            } catch (e: Exception) {
                _mlKitOcrResult.value = null
                onComplete(null)
            } finally {
                _isMlKitProcessing.value = false
            }
        }
    }

    /**
     * Process image file with ML Kit on-device Text Recognition
     */
    fun processFileWithMlKit(
        file: File,
        rotationDegrees: Int = 0,
        onComplete: (TextRecognitionResult?) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMlKitProcessing.value = true
            try {
                val result = repository.recognizeTextFromFile(file, rotationDegrees)
                val recognition = result.getOrNull()
                _mlKitOcrResult.value = recognition
                onComplete(recognition)
            } catch (e: Exception) {
                _mlKitOcrResult.value = null
                onComplete(null)
            } finally {
                _isMlKitProcessing.value = false
            }
        }
    }

    /**
     * Extract and save document directly using ML Kit Text Recognition on-device.
     */
    fun extractDocumentWithMlKit(
        fileName: String,
        base64Image: String,
        isPrivacyOffline: Boolean,
        onSuccess: (DocumentEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isExtracting.value = true
            _extractStatusMessage.value = "Processing image on-device with ML Kit Text Recognition..."
            try {
                val doc = repository.extractDocumentWithMlKit(
                    fileName = fileName,
                    base64Image = base64Image,
                    isPrivacyOffline = isPrivacyOffline
                )
                _extractStatusMessage.value = "Extracted ${doc.wordCount} words via on-device ML Kit"
                selectDocument(doc)
                onSuccess(doc)
            } catch (e: Exception) {
                _extractStatusMessage.value = "ML Kit Error: ${e.message}"
            } finally {
                _isExtracting.value = false
            }
        }
    }

    fun clearMlKitResult() {
        _mlKitOcrResult.value = null
    }

    fun playTts(text: String) {
        viewModelScope.launch {
            if (_isAudioPlaying.value) {
                audioPlayer.stop()
                _isAudioPlaying.value = false
                return@launch
            }

            _isTtsLoading.value = true
            try {
                val ttsResult = repository.generateTts(text)
                if (ttsResult.audioBase64 != null) {
                    _isTtsLoading.value = false
                    _isAudioPlaying.value = true
                    audioPlayer.playBase64Audio(
                        base64Audio = ttsResult.audioBase64,
                        mimeType = ttsResult.mimeType,
                        onCompletion = { _isAudioPlaying.value = false }
                    )
                } else {
                    _isTtsLoading.value = false
                    _extractStatusMessage.value = "TTS playback ready (preview synthetic voice)"
                }
            } catch (e: Exception) {
                _isTtsLoading.value = false
                _isAudioPlaying.value = false
            }
        }
    }

    fun stopAudio() {
        audioPlayer.stop()
        _isAudioPlaying.value = false
    }

    fun askStudyQuestion(
        question: String,
        mode: AiStudyMode,
        subjectId: String,
        senderName: String = "Student Alex"
    ) {
        viewModelScope.launch {
            _isAskingAi.value = true
            // Save student's question first
            val userMsg = StudyChatMessage(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                senderName = senderName,
                senderRole = "Student",
                message = question,
                timestamp = System.currentTimeMillis()
            )
            repository.addChatMessage(userMsg)

            // Context from linked document if available
            val docContext = _selectedDocument.value?.extractedText ?: "Collaborative Study Session on Subject."

            val response: GeminiStudyResponse = when (mode) {
                AiStudyMode.HIGH_THINKING -> repository.askHighThinking(question, docContext)
                AiStudyMode.SEARCH_GROUNDING -> repository.askSearchGrounding(question, _selectedSubject.value?.subjectName ?: "Academic Study")
                AiStudyMode.MAPS_GROUNDING -> repository.askMapsGrounding(question)
                AiStudyMode.QUICK_LITE -> repository.askHighThinking(question, docContext)
            }

            val aiMsg = StudyChatMessage(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                senderName = "DocuMind AI (${when (mode) {
                    AiStudyMode.HIGH_THINKING -> "Thinking: HIGH"
                    AiStudyMode.SEARCH_GROUNDING -> "Search Grounded"
                    AiStudyMode.MAPS_GROUNDING -> "Maps Grounded"
                    AiStudyMode.QUICK_LITE -> "Flash-Lite"
                }})",
                senderRole = "AI Tutor",
                message = response.answer,
                thinkingText = response.thinkingText,
                searchSources = if (response.sources.isNotEmpty()) response.sources.joinToString("\n• ") else null,
                timestamp = System.currentTimeMillis()
            )
            repository.addChatMessage(aiMsg)
            _isAskingAi.value = false
        }
    }

    fun askDocumentDirect(
        question: String,
        context: String,
        onResult: (String, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isAskingAi.value = true
            val res = repository.askHighThinking(question, context)
            onResult(res.answer, res.thinkingText)
            _isAskingAi.value = false
        }
    }

    fun createSubject(name: String, title: String, description: String, linkedDocId: String?) {
        viewModelScope.launch {
            val sub = StudySubject(
                id = UUID.randomUUID().toString(),
                subjectName = name,
                title = title,
                description = description,
                linkedDocumentId = linkedDocId,
                hostRole = "Teacher",
                participantCount = 5,
                timestamp = System.currentTimeMillis()
            )
            repository.addStudySubject(sub)
            selectSubject(sub)
        }
    }

    fun generateDiagram(prompt: String, inputImageBase64: String? = null) {
        viewModelScope.launch {
            _isGeneratingDiagram.value = true
            _diagramResult.value = null
            try {
                val res = repository.generateOrEditDiagram(prompt, inputImageBase64)
                _diagramResult.value = res
            } catch (e: Exception) {
                _diagramResult.value = GeminiImageResult(null, null, "Error: ${e.message}")
            } finally {
                _isGeneratingDiagram.value = false
            }
        }
    }

    fun syncCloudNow() {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            _syncMessage.value = "Syncing eligible documents to Firestore..."
            try {
                val res = repository.syncWithCloud(allDocuments.value)
                _syncMessage.value = res.message
            } catch (e: Exception) {
                _syncMessage.value = "Sync error: ${e.message}"
            } finally {
                _isSyncingCloud.value = false
            }
        }
    }

    fun signInUser(displayName: String) {
        viewModelScope.launch {
            val ok = cloudSyncManager.signInAnonymouslyOrGoogle(displayName)
            _isUserSignedIn.value = ok
            if (ok) {
                repository.initRealtimeCloudSync()
                syncCloudNow()
            }
        }
    }

    fun signOutUser() {
        cloudSyncManager.signOut()
        _isUserSignedIn.value = false
    }

    fun toggleAutoSync(enabled: Boolean) {
        cloudSyncManager.setAutoSyncEnabled(enabled)
        if (enabled) {
            repository.initRealtimeCloudSync()
        } else {
            cloudSyncManager.stopRealtimeSync()
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            _syncMessage.value = "Restoring documents from Firestore cloud to local Room DB..."
            try {
                val res = repository.restoreFromCloud()
                _syncMessage.value = res.message
            } catch (e: Exception) {
                _syncMessage.value = "Restore error: ${e.message}"
            } finally {
                _isSyncingCloud.value = false
            }
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
            if (_selectedDocument.value?.id == id) {
                _selectedDocument.value = null
            }
        }
    }

    fun updateDocumentCategory(id: String, newCategory: String) {
        viewModelScope.launch {
            repository.updateCategory(id, newCategory)
            val curr = _selectedDocument.value
            if (curr?.id == id) {
                _selectedDocument.value = curr.copy(folderCategory = newCategory)
            }
        }
    }

    fun toggleFavorite(id: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, isFavorite)
            val curr = _selectedDocument.value
            if (curr?.id == id) {
                _selectedDocument.value = curr.copy(isFavorite = isFavorite)
            }
        }
    }

    fun setArchived(id: String, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setArchived(id, isArchived)
            val curr = _selectedDocument.value
            if (curr?.id == id) {
                _selectedDocument.value = curr.copy(isArchived = isArchived)
            }
        }
    }

    fun updateUserNotes(id: String, notes: String) {
        viewModelScope.launch {
            repository.updateNotes(id, notes)
            val curr = _selectedDocument.value
            if (curr?.id == id) {
                _selectedDocument.value = curr.copy(userNotes = notes)
            }
        }
    }

    fun renameDocument(id: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameDocument(id, newTitle)
            val curr = _selectedDocument.value
            if (curr?.id == id) {
                _selectedDocument.value = curr.copy(title = newTitle)
            }
        }
    }

    fun getOcrRecordsForDocument(id: String): Flow<List<com.example.data.model.OcrRecordEntity>> =
        repository.getOcrRecordsForDocument(id)

    fun getMetadataForDocument(id: String): Flow<com.example.data.model.DocumentMetadataEntity?> =
        repository.getMetadataForDocument(id)

    override fun onCleared() {

        super.onCleared()
        audioPlayer.stop()
    }
}
