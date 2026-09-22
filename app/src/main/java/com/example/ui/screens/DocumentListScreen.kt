package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.ui.components.DocuMindAppBar
import com.example.ui.components.DocumentItemCard
import com.example.ui.components.FolderFilterChips
import com.example.ui.viewmodel.DocuMindViewModel

@Composable
fun DocumentListScreen(
    viewModel: DocuMindViewModel,
    onDocumentClick: (DocumentEntity) -> Unit,
    onStudyClick: (DocumentEntity) -> Unit,
    onOpenSyncClick: () -> Unit,
    onOpenScanner: () -> Unit = {}
) {
    val documents by viewModel.filteredDocuments.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isPrivacyFilter by viewModel.isPrivacyOfflineFilter.collectAsState()
    val isTtsLoading by viewModel.isTtsLoading.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val isExtracting by viewModel.isExtracting.collectAsState()
    val statusMessage by viewModel.extractStatusMessage.collectAsState()

    var showExtractDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Compute category counts
    val categoryCounts = remember(allDocs) {
        val counts = mutableMapOf<String, Int>()
        allDocs.forEach { doc ->
            counts[doc.folderCategory] = (counts[doc.folderCategory] ?: 0) + 1
        }
        counts
    }

    Scaffold(
        topBar = {
            DocuMindAppBar(
                isPrivacyActive = isPrivacyFilter,
                isCloudSynced = allDocs.any { it.isCloudSynced },
                onTogglePrivacy = { viewModel.togglePrivacyFilter(it) },
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onSyncClick = onOpenSyncClick,
                onCameraClick = onOpenScanner
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingActionButton(
                    onClick = { showExtractDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(3.dp),
                    modifier = Modifier.testTag("add_document_fab")
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = "Extract text/samples")
                }

                ExtendedFloatingActionButton(
                    onClick = onOpenScanner,
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                    text = { Text("Camera Scan", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("scan_camera_fab")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Folder Category Chips
            FolderFilterChips(
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) },
                folderCounts = categoryCounts
            )

            // Privacy Active Banner
            if (selectedCategory == "Confidential / Offline" || isPrivacyFilter) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF92400E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy Protected: Stored strictly in local Room DB on this device.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF78350F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Results counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${documents.size} Document${if (documents.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "Filtered by \"$searchQuery\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No documents found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try another keyword or category filter" else "Tap '+ Extract OCR' to scan or load documents",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentItemCard(
                            document = doc,
                            onClick = { onDocumentClick(doc) },
                            onListenTts = {
                                viewModel.selectDocument(doc)
                                viewModel.playTts(doc.extractedText)
                            },
                            onStudyClick = {
                                viewModel.selectDocument(doc)
                                onStudyClick(doc)
                            },
                            onDeleteClick = {
                                viewModel.deleteDocument(doc.id)
                            },
                            isPlayingThis = isAudioPlaying && viewModel.selectedDocument.value?.id == doc.id,
                            isTtsLoading = isTtsLoading && viewModel.selectedDocument.value?.id == doc.id
                        )
                    }
                }
            }
        }
    }

    if (showExtractDialog) {
        ExtractDocumentDialog(
            onDismiss = { showExtractDialog = false },
            onExtract = { fileName, fileType, content, isBase64, isDeep, isPrivacy ->
                viewModel.extractDocument(
                    fileName = fileName,
                    fileType = fileType,
                    content = content,
                    isBase64Image = isBase64,
                    isDeepAnalysis = isDeep,
                    isPrivacyOffline = isPrivacy,
                    onSuccess = { doc ->
                        showExtractDialog = false
                    }
                )
            },
            isExtracting = isExtracting,
            onLaunchCamera = onOpenScanner
        )
    }
}
