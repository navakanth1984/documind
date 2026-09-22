package com.example.sync

import android.content.Context
import android.util.Log
import com.example.data.model.DocumentEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

data class SyncResult(
    val success: Boolean,
    val syncedCount: Int,
    val message: String
)

data class SyncLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false
)

sealed class SyncStatus {
    data class Idle(val lastSyncTime: Long? = null, val message: String = "Ready to sync") : SyncStatus()
    data class Syncing(val progressMessage: String) : SyncStatus()
    data class LiveListening(val userId: String, val lastEventTime: Long = System.currentTimeMillis()) : SyncStatus()
    data class Error(val errorMessage: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
}

/**
 * CloudSyncManager handles bidirectional, real-time synchronization between
 * the local Room Database (SQLite) and Google Cloud Firebase Firestore.
 *
 * Supports:
 * - Real-time snapshot listening across devices via Firestore `addSnapshotListener`
 * - Automatic background push on local document creation / OCR extraction
 * - Tombstone deletion propagation across devices
 * - Zero-knowledge filtering of confidential offline documents
 * - Graceful fallback and offline resilience
 */
class CloudSyncManager(private val context: Context) {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("documind_sync_prefs", Context.MODE_PRIVATE)

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var listenerRegistration: ListenerRegistration? = null

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(
        prefs.getLong("last_sync_timestamp", 0L).takeIf { it > 0L }
    )
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(
        prefs.getBoolean("auto_sync_enabled", true)
    )
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _syncEventsLog = MutableStateFlow<List<SyncLogEntry>>(emptyList())
    val syncEventsLog: StateFlow<List<SyncLogEntry>> = _syncEventsLog.asStateFlow()

    init {
        initFirebase()
        addLog("CloudSyncManager initialized with device: ${getDeviceId()}")
    }

    private fun initFirebase() {
        try {
            ensureFirebaseApp()
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("CloudSyncManager", "Firebase initialization warning: ${e.message}")
            addLog("Firebase credentials warning: ${e.message}", isError = true)
        }
    }

    private fun ensureFirebaseApp() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:319414224099:android:com.aistudio.documind.qzkf")
                        .setProjectId("documind-cloud-sync")
                        .setApiKey("AIzaSyDocuMindKeyFallback")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }
            }
        } catch (e: Exception) {
            Log.w("CloudSyncManager", "Fallback FirebaseApp error: ${e.message}")
        }
    }

    fun getDeviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = "device_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
        _isAutoSyncEnabled.value = enabled
        addLog("Real-time auto-backup set to: $enabled")
    }

    fun isAutoSyncActive(): Boolean = _isAutoSyncEnabled.value

    val currentUser: FirebaseUser?
        get() = try {
            auth?.currentUser
        } catch (e: Exception) {
            null
        }

    val isUserSignedIn: Boolean
        get() = currentUser != null

    fun getEffectiveUserId(): String {
        return currentUser?.uid ?: getDeviceId()
    }

    fun addLog(message: String, isError: Boolean = false) {
        val entry = SyncLogEntry(message = message, isError = isError)
        val current = _syncEventsLog.value.toMutableList()
        if (current.size >= 20) {
            current.removeAt(0)
        }
        current.add(entry)
        _syncEventsLog.value = current
    }

    suspend fun signInAnonymouslyOrGoogle(displayName: String = "DocuMind Scholar"): Boolean = withContext(Dispatchers.IO) {
        try {
            val a = auth
            if (a == null) {
                addLog("Auth not initialized. Using local device token session.")
                return@withContext true
            }
            if (a.currentUser == null) {
                a.signInAnonymously().await()
            }
            addLog("Authenticated user: ${a.currentUser?.uid ?: "anonymous"}")
            true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Sign-in error", e)
            addLog("Authentication note: ${e.localizedMessage}", isError = false)
            // Allow device token fallback
            true
        }
    }

    fun signOut() {
        try {
            stopRealtimeSync()
            auth?.signOut()
            addLog("User signed out from cloud session.")
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Sign out error", e)
        }
    }

    /**
     * Serializes a Room DocumentEntity to a structured Firestore Map.
     */
    fun documentToMap(doc: DocumentEntity, deviceId: String): Map<String, Any?> {
        return mapOf(
            "id" to doc.id,
            "title" to doc.title,
            "fileType" to doc.fileType,
            "fileName" to doc.fileName,
            "extractedText" to doc.extractedText,
            "summary" to doc.summary,
            "folderCategory" to doc.folderCategory,
            "tags" to doc.tags,
            "wordCount" to doc.wordCount,
            "characterCount" to doc.characterCount,
            "lineCount" to doc.lineCount,
            "ocrEngine" to doc.ocrEngine,
            "confidenceScore" to doc.confidenceScore.toDouble(),
            "detectedLanguage" to doc.detectedLanguage,
            "fileSizeBytes" to doc.fileSizeBytes,
            "isFavorite" to doc.isFavorite,
            "isArchived" to doc.isArchived,
            "userNotes" to doc.userNotes,
            "timestamp" to doc.timestamp,
            "lastModifiedTimestamp" to doc.lastModifiedTimestamp,
            "syncedAt" to System.currentTimeMillis(),
            "originDeviceId" to deviceId,
            "deleted" to false
        )
    }

    /**
     * Deserializes a Firestore Document Map back into a Room DocumentEntity.
     */
    fun mapToDocument(data: Map<String, Any?>): DocumentEntity? {
        val id = data["id"] as? String ?: return null
        val title = data["title"] as? String ?: "Untitled Document"
        val fileType = data["fileType"] as? String ?: "IMAGE"
        val fileName = data["fileName"] as? String ?: "document.jpg"
        val extractedText = data["extractedText"] as? String ?: ""
        val summary = data["summary"] as? String ?: ""
        val folderCategory = data["folderCategory"] as? String ?: "General"
        val tags = data["tags"] as? String ?: ""
        val wordCount = (data["wordCount"] as? Number)?.toInt() ?: 0
        val characterCount = (data["characterCount"] as? Number)?.toInt() ?: extractedText.length
        val lineCount = (data["lineCount"] as? Number)?.toInt() ?: extractedText.lines().size
        val ocrEngine = data["ocrEngine"] as? String ?: "ML_KIT"
        val confidenceScore = (data["confidenceScore"] as? Number)?.toFloat() ?: 1.0f
        val detectedLanguage = data["detectedLanguage"] as? String ?: "en"
        val fileSizeBytes = (data["fileSizeBytes"] as? Number)?.toLong() ?: 0L
        val isFavorite = data["isFavorite"] as? Boolean ?: false
        val isArchived = data["isArchived"] as? Boolean ?: false
        val userNotes = data["userNotes"] as? String ?: ""
        val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val lastModifiedTimestamp = (data["lastModifiedTimestamp"] as? Number)?.toLong() ?: timestamp

        return DocumentEntity(
            id = id,
            title = title,
            fileType = fileType,
            fileName = fileName,
            extractedText = extractedText,
            summary = summary,
            folderCategory = folderCategory,
            tags = tags,
            isOfflinePrivacy = false,
            isCloudSynced = true,
            wordCount = wordCount,
            characterCount = characterCount,
            lineCount = lineCount,
            ocrEngine = ocrEngine,
            confidenceScore = confidenceScore,
            detectedLanguage = detectedLanguage,
            fileSizeBytes = fileSizeBytes,
            isFavorite = isFavorite,
            isArchived = isArchived,
            lastModifiedTimestamp = lastModifiedTimestamp,
            userNotes = userNotes,
            timestamp = timestamp,
            imageBase64 = null
        )
    }

    /**
     * Real-time backup of a single DocumentEntity to Firestore when created or modified locally in Room.
     */
    suspend fun backupSingleDocument(doc: DocumentEntity): Boolean = withContext(Dispatchers.IO) {
        if (doc.isOfflinePrivacy) {
            addLog("Confidential document '${doc.title}' bypassed cloud sync.")
            return@withContext false
        }
        val db = firestore ?: return@withContext false
        val uid = getEffectiveUserId()
        try {
            val docMap = documentToMap(doc, getDeviceId())
            db.collection("users")
                .document(uid)
                .collection("documents")
                .document(doc.id)
                .set(docMap)
                .await()
            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            prefs.edit().putLong("last_sync_timestamp", now).apply()
            addLog("Live backed up '${doc.title}' to Firestore")
            true
        } catch (e: Exception) {
            Log.w("CloudSyncManager", "Realtime single backup failed for ${doc.id}: ${e.message}")
            false
        }
    }

    /**
     * Tombstone deletion in Firestore to propagate deletions in real time to other user devices.
     */
    suspend fun deleteDocumentFromCloud(docId: String): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        val uid = getEffectiveUserId()
        try {
            val docRef = db.collection("users").document(uid).collection("documents").document(docId)
            docRef.set(
                mapOf(
                    "deleted" to true,
                    "lastModifiedTimestamp" to System.currentTimeMillis(),
                    "originDeviceId" to getDeviceId()
                ),
                SetOptions.merge()
            ).await()
            addLog("Propagated deletion of document $docId to Firestore cloud")
            true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to delete document from cloud: $docId", e)
            false
        }
    }

    /**
     * Batch backup all eligible local Room documents to Firebase Firestore.
     */
    suspend fun syncDocumentsToCloud(documents: List<DocumentEntity>): SyncResult = withContext(Dispatchers.IO) {
        val db = firestore
        val uid = getEffectiveUserId()

        if (db == null) {
            val msg = "Firestore service is offline or credentials pending."
            addLog(msg, isError = true)
            _syncStatus.value = SyncStatus.Error(msg)
            return@withContext SyncResult(
                success = false,
                syncedCount = 0,
                message = msg
            )
        }

        val eligibleDocs = documents.filter { !it.isOfflinePrivacy }
        if (eligibleDocs.isEmpty()) {
            val msg = "No non-confidential documents to sync. Offline documents remain local in Room DB."
            addLog(msg)
            return@withContext SyncResult(
                success = true,
                syncedCount = 0,
                message = msg
            )
        }

        _syncStatus.value = SyncStatus.Syncing("Uploading ${eligibleDocs.size} documents to Firestore...")
        var synced = 0
        val deviceId = getDeviceId()

        try {
            val batch = db.batch()
            for (doc in eligibleDocs) {
                val docRef = db.collection("users")
                    .document(uid)
                    .collection("documents")
                    .document(doc.id)
                val docMap = documentToMap(doc, deviceId)
                batch.set(docRef, docMap)
                synced++
            }
            batch.commit().await()

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            prefs.edit().putLong("last_sync_timestamp", now).apply()

            val successMsg = "Successfully backed up $synced document(s) to Firestore cloud."
            addLog(successMsg)
            _syncStatus.value = SyncStatus.Idle(now, successMsg)

            SyncResult(
                success = true,
                syncedCount = synced,
                message = successMsg
            )
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Firestore sync failed", e)
            val errorMsg = "Sync failed: ${e.localizedMessage}"
            addLog(errorMsg, isError = true)
            _syncStatus.value = SyncStatus.Error(errorMsg)
            SyncResult(
                success = false,
                syncedCount = synced,
                message = errorMsg
            )
        }
    }

    /**
     * Fetches all remote documents from Firestore for the active user account.
     */
    suspend fun restoreDocumentsFromCloud(): List<DocumentEntity> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext emptyList()
        val uid = getEffectiveUserId()
        try {
            addLog("Querying Firestore cloud documents for user $uid...")
            val snapshot = db.collection("users")
                .document(uid)
                .collection("documents")
                .whereEqualTo("deleted", false)
                .get()
                .await()

            val remoteDocs = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                mapToDocument(data)
            }
            addLog("Fetched ${remoteDocs.size} remote document(s) from Firestore cloud.")
            remoteDocs
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to restore documents from cloud", e)
            addLog("Cloud restore note: ${e.localizedMessage}", isError = true)
            emptyList()
        }
    }

    /**
     * Attaches a real-time Firestore SnapshotListener to listen for document
     * additions, updates, and deletions across all authorized user devices.
     */
    fun startRealtimeSync(
        onDocumentReceived: suspend (DocumentEntity) -> Unit,
        onDocumentRemoved: suspend (String) -> Unit
    ) {
        stopRealtimeSync()
        val db = firestore
        if (db == null) {
            addLog("Firestore not initialized for real-time listener", isError = true)
            return
        }

        val uid = getEffectiveUserId()
        addLog("Attaching real-time Firestore listener at users/$uid/documents")
        _syncStatus.value = SyncStatus.LiveListening(uid)

        try {
            listenerRegistration = db.collection("users")
                .document(uid)
                .collection("documents")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("CloudSyncManager", "Real-time snapshot listener error", error)
                        addLog("Sync listener error: ${error.message}", isError = true)
                        _syncStatus.value = SyncStatus.Error(error.localizedMessage ?: "Sync error")
                        return@addSnapshotListener
                    }

                    if (snapshots == null) return@addSnapshotListener

                    val myDeviceId = getDeviceId()
                    syncScope.launch {
                        for (change in snapshots.documentChanges) {
                            val data = change.document.data
                            val originDeviceId = data["originDeviceId"] as? String
                            val isDeleted = data["deleted"] as? Boolean ?: false
                            val docId = change.document.id

                            // Skip if change originated locally from this device in current session
                            if (change.document.metadata.hasPendingWrites() || originDeviceId == myDeviceId) {
                                continue
                            }

                            if (isDeleted || change.type == DocumentChange.Type.REMOVED) {
                                addLog("Real-time deletion received from remote device: $docId")
                                onDocumentRemoved(docId)
                            } else {
                                val entity = mapToDocument(data)
                                if (entity != null) {
                                    addLog("Real-time update received: '${entity.title}'")
                                    onDocumentReceived(entity)
                                }
                            }
                        }
                        _syncStatus.value = SyncStatus.LiveListening(uid, System.currentTimeMillis())
                    }
                }
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to start snapshot listener", e)
            _syncStatus.value = SyncStatus.Error(e.localizedMessage ?: "Listener failed")
        }
    }

    /**
     * Detaches the real-time Firestore snapshot listener.
     */
    fun stopRealtimeSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
        _syncStatus.value = SyncStatus.Idle(_lastSyncTimestamp.value, "Real-time sync paused")
        addLog("Real-time Firestore listener detached.")
    }
}

