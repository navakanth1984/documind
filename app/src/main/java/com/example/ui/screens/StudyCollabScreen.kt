package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.StudyChatMessage
import com.example.data.model.StudySubject
import com.example.ui.viewmodel.AiStudyMode
import com.example.ui.viewmodel.DocuMindViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCollabScreen(
    viewModel: DocuMindViewModel,
    onBack: () -> Unit
) {
    val subjects by viewModel.studySubjects.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val messages by viewModel.currentSubjectMessages.collectAsState()
    val isAskingAi by viewModel.isAskingAi.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    var selectedMode by remember { mutableStateOf(AiStudyMode.HIGH_THINKING) }
    var inputQuestion by remember { mutableStateOf("") }
    var senderRole by remember { mutableStateOf("Student") } // "Student" or "Teacher"
    var showNewSubjectDialog by remember { mutableStateOf(false) }

    // Ensure first subject selected if none
    if (selectedSubject == null && subjects.isNotEmpty()) {
        viewModel.selectSubject(subjects.first())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Collaborative Study Hub",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Real-Time Student & Teacher Discussion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("collab_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AssistChip(
                        onClick = { showNewSubjectDialog = true },
                        label = { Text("+ Upload Subject") },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("add_subject_chip")
                    )
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
            // Subject selector horizontal row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { subject ->
                    val isSelected = selectedSubject?.id == subject.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectSubject(subject) },
                        label = { Text(subject.subjectName) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("subject_chip_${subject.id}")
                    )
                }
            }

            // Subject Info Banner
            selectedSubject?.let { sub ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${sub.participantCount} active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Text(
                            text = sub.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // AI Model Mode Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedMode == AiStudyMode.HIGH_THINKING,
                    onClick = { selectedMode = AiStudyMode.HIGH_THINKING },
                    label = { Text("High Thinking (pro)") },
                    leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("mode_high_thinking")
                )
                FilterChip(
                    selected = selectedMode == AiStudyMode.SEARCH_GROUNDING,
                    onClick = { selectedMode = AiStudyMode.SEARCH_GROUNDING },
                    label = { Text("Google Search Grounding") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("mode_search_grounding")
                )
                FilterChip(
                    selected = selectedMode == AiStudyMode.MAPS_GROUNDING,
                    onClick = { selectedMode = AiStudyMode.MAPS_GROUNDING },
                    label = { Text("Google Maps Grounding") },
                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("mode_maps_grounding")
                )
                FilterChip(
                    selected = selectedMode == AiStudyMode.QUICK_LITE,
                    onClick = { selectedMode = AiStudyMode.QUICK_LITE },
                    label = { Text("Fast Lite (flash-lite)") },
                    leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("mode_flash_lite")
                )
            }

            // Chat Messages Feed
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    StudyMessageBubble(
                        msg = msg,
                        onPlayTts = { viewModel.playTts(msg.message) },
                        isAudioPlaying = isAudioPlaying
                    )
                }
            }

            // Question Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Role Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Posting as:", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = senderRole == "Student",
                                onClick = { senderRole = "Student" },
                                label = { Text("Student Alex") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = senderRole == "Teacher",
                                onClick = { senderRole = "Teacher" },
                                label = { Text("Prof. Davis") },
                                leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputQuestion,
                            onValueChange = { inputQuestion = it },
                            placeholder = { Text("Ask question or provide study guidance...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("collab_input_field"),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (inputQuestion.isNotBlank() && selectedSubject != null) {
                                    val q = inputQuestion
                                    inputQuestion = ""
                                    viewModel.askStudyQuestion(
                                        question = q,
                                        mode = selectedMode,
                                        subjectId = selectedSubject!!.id,
                                        senderName = if (senderRole == "Teacher") "Prof. Davis" else "Student Alex"
                                    )
                                }
                            },
                            enabled = inputQuestion.isNotBlank() && !isAskingAi && selectedSubject != null,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("collab_send_button"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (isAskingAi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewSubjectDialog) {
        CreateSubjectDialog(
            documents = allDocs,
            onDismiss = { showNewSubjectDialog = false },
            onCreate = { name, title, desc, linkedDocId ->
                viewModel.createSubject(name, title, desc, linkedDocId)
                showNewSubjectDialog = false
            }
        )
    }
}

@Composable
fun StudyMessageBubble(
    msg: StudyChatMessage,
    onPlayTts: () -> Unit,
    isAudioPlaying: Boolean
) {
    var isThinkingExpanded by remember { mutableStateOf(false) }

    val isAi = msg.senderRole == "AI Tutor"
    val isTeacher = msg.senderRole == "Teacher"

    val containerColor = when {
        isAi -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isTeacher -> Color(0xFFFEF3C7) // Warm amber for teacher
        else -> MaterialTheme.colorScheme.surfaceVariant // Student
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Sender name, badge, time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = when {
                        isAi -> MaterialTheme.colorScheme.primary
                        isTeacher -> Color(0xFFB45309)
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(badgeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when {
                            isAi -> Icons.Default.AutoAwesome
                            isTeacher -> Icons.Default.School
                            else -> Icons.Default.Person
                        }
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${msg.senderRole}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isAi) {
                    IconButton(
                        onClick = onPlayTts,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "TTS Voice",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main message body
            Text(
                text = msg.message,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )

            // Deep Thinking accordion for AI messages
            if (!msg.thinkingText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isThinkingExpanded = !isThinkingExpanded }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Thinking Process (High Reasoning)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                if (isThinkingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AnimatedVisibility(visible = isThinkingExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = msg.thinkingText,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Grounding sources if present
            if (!msg.searchSources.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Grounding References:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = msg.searchSources,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateSubjectDialog(
    documents: List<com.example.data.model.DocumentEntity>,
    onDismiss: () -> Unit,
    onCreate: (name: String, title: String, description: String, linkedDocId: String?) -> Unit
) {
    var subjectName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDocId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Create Study Subject",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Upload a subject for real-time collaborative Q&A",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject (e.g. Astrophysics, Organic Chemistry)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Title (e.g. Midterm Group Study)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Topics & Goals") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Link to Extracted Document (Optional):", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    documents.forEach { doc ->
                        FilterChip(
                            selected = selectedDocId == doc.id,
                            onClick = { selectedDocId = if (selectedDocId == doc.id) null else doc.id },
                            label = { Text(doc.title, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (subjectName.isNotBlank() && title.isNotBlank()) {
                                onCreate(subjectName, title, description, selectedDocId)
                            }
                        },
                        enabled = subjectName.isNotBlank() && title.isNotBlank()
                    ) {
                        Text("Start Session")
                    }
                }
            }
        }
    }
}
