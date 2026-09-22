package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.viewmodel.AiStudyMode
import com.example.ui.viewmodel.DocuMindViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    document: DocumentEntity,
    viewModel: DocuMindViewModel,
    onBack: () -> Unit,
    onNavigateToCollab: () -> Unit,
    onNavigateToDiagram: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Extracted Text", "Summary & Meta", "High-Thinking Tutor")

    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val isTtsLoading by viewModel.isTtsLoading.collectAsState()
    val isAskingAi by viewModel.isAskingAi.collectAsState()

    var studyQuestionInput by remember { mutableStateOf("") }
    var directAnswer by remember { mutableStateOf<String?>(null) }
    var thinkingProcess by remember { mutableStateOf<String?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val categories = listOf(
        "Computer Science",
        "Biology",
        "Mathematics",
        "Literature",
        "Physics",
        "Exam Prep",
        "Confidential / Offline"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = document.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "${document.fileName} • ${document.fileType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Category selector chip
                    Box {
                        AssistChip(
                            onClick = { showCategoryMenu = true },
                            label = { Text(document.folderCategory) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (document.isOfflinePrivacy) Icons.Default.Lock else Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        viewModel.updateDocumentCategory(document.id, cat)
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // TTS Audio Action Banner (Using gemini-3.1-flash-tts-preview)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTtsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Audio Playback",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Listen with TTS Voice",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Model: gemini-3.1-flash-tts-preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.playTts(document.extractedText) },
                        modifier = Modifier.testTag("detail_tts_play_btn")
                    ) {
                        Text(if (isAudioPlaying) "Stop" else "Listen")
                    }
                }
            }

            // Secondary Action Row (Diagram Generator & Study Collab)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onNavigateToDiagram,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_gen_diagram_btn")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Illustrate Diagram", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = onNavigateToCollab,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_collab_btn")
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Study Collab", fontSize = 12.sp)
                }
            }

            // Tab Row
            SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // Extracted Text Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Searchable OCR Content",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${document.wordCount} words",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = document.extractedText,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                1 -> {
                    // Summary & Metadata Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Executive Summary",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = document.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp),
                                lineHeight = 22.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Categorization & Tags",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            document.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Privacy & Storage Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (document.isOfflinePrivacy) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (document.isOfflinePrivacy) "Offline Privacy Protected" else "Cloud Sync Eligible",
                                    fontWeight = FontWeight.Bold,
                                    color = if (document.isOfflinePrivacy) Color(0xFF92400E) else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (document.isOfflinePrivacy)
                                        "This document is stored strictly in the device's local Room database and will never be uploaded to external servers without explicit consent."
                                    else
                                        "Stored in local Room database and synced to Firestore cloud across authorized devices.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (document.isOfflinePrivacy) Color(0xFF78350F) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // High-Thinking Mode Tab (Using gemini-3.1-pro-preview with thinkingLevel = HIGH)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "High Thinking Mode Enabled",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "Uses gemini-3.1-pro-preview with ThinkingLevel.HIGH for complex academic proofs, logic, and deep analysis.",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = studyQuestionInput,
                            onValueChange = { studyQuestionInput = it },
                            placeholder = { Text("Ask a difficult academic or conceptual question...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("doc_study_question_input"),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (studyQuestionInput.isNotBlank()) {
                                            viewModel.askDocumentDirect(
                                                question = studyQuestionInput,
                                                context = document.extractedText,
                                                onResult = { ans, think ->
                                                    directAnswer = ans
                                                    thinkingProcess = think
                                                }
                                            )
                                        }
                                    },
                                    enabled = studyQuestionInput.isNotBlank() && !isAskingAi,
                                    modifier = Modifier.testTag("doc_submit_question_btn")
                                ) {
                                    if (isAskingAi) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Submit")
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (thinkingProcess != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Deep Thinking Reasoning Chain:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = thinkingProcess ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (directAnswer != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Tutor Answer",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = directAnswer ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

