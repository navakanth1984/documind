package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.DocumentEntity

data class SampleUnstructuredFile(
    val title: String,
    val fileName: String,
    val fileType: String,
    val content: String,
    val description: String
)

@Composable
fun ExtractDocumentDialog(
    onDismiss: () -> Unit,
    onExtract: (fileName: String, fileType: String, content: String, isBase64Image: Boolean, isDeepAnalysis: Boolean, isPrivacyOffline: Boolean) -> Unit,
    isExtracting: Boolean,
    onLaunchCamera: () -> Unit = {}
) {
    val sampleFiles = listOf(
        SampleUnstructuredFile(
            title = "Research Paper Excerpt",
            fileName = "attention_is_all_you_need_nlp.pdf",
            fileType = "PDF",
            content = "The dominant sequence transduction models are based on complex recurrent or convolutional neural networks that include an encoder and a decoder. We propose a new simple network architecture, the Transformer, based solely on attention mechanisms, dispensing with recurrence and convolutions entirely. Multi-Head Attention allows the model to jointly attend to information from different representation subspaces.",
            description = "Academic PDF with dense scientific terminology"
        ),
        SampleUnstructuredFile(
            title = "Classroom Whiteboard Photo",
            fileName = "chemistry_equilibrium_reactions.png",
            fileType = "IMAGE",
            content = "Chemical Equilibrium: aA + bB <=> cC + dD. Equilibrium Constant Kc = [C]^c [D]^d / ([A]^a [B]^b). Le Chatelier's Principle: If an equilibrium is disturbed by changing the conditions (pressure, concentration, temperature), the position of equilibrium shifts to counteract the change.",
            description = "Handwritten blackboard formulas & equations"
        ),
        SampleUnstructuredFile(
            title = "EPUB Textbook Chapter",
            fileName = "world_history_renaissance_art.epub",
            fileType = "EPUB",
            content = "Chapter 4: The High Renaissance in Florence and Rome. The revival of classical antiquity, humanism, and linear perspective revolutionized European visual arts. Leonardo da Vinci's chiaroscuro and sfumato techniques merged empirical observation with idealized form, influencing contemporaries including Michelangelo and Raphael.",
            description = "Unstructured digital book chapter"
        ),
        SampleUnstructuredFile(
            title = "Recorded Lecture Clip",
            fileName = "mit_physics_electromagnetism_lec12.mp4",
            fileType = "VIDEO",
            content = "Video Lecture Key Points: 02:15 Gauss's Law for Magnetism - magnetic monopoles do not exist. 08:30 Faraday's Law of Induction - changing magnetic flux induces electromotive force (EMF = -dPhi/dt). 15:45 Maxwell-Ampere law displacement current correction.",
            description = "Video recording with timestamps & lecture notes"
        ),
        SampleUnstructuredFile(
            title = "Confidential Patient Study",
            fileName = "clinical_trial_blinded_cohort_b.txt",
            fileType = "TEXT",
            content = "PATIENT HEALTH INFORMATION - CONFIDENTIAL: Double-blind cohort assessment of novel peptide therapy. Subject 104 showed 28% reduction in inflammatory markers. Strictly local offline storage mandated by privacy protocols.",
            description = "Sensitive private document for offline Room storage"
        )
    )

    var selectedSample by remember { mutableStateOf(sampleFiles[0]) }
    var customFileName by remember { mutableStateOf(selectedSample.fileName) }
    var customFileType by remember { mutableStateOf(selectedSample.fileType) }
    var customContent by remember { mutableStateOf(selectedSample.content) }
    var isDeepModel by remember { mutableStateOf(false) } // False = gemini-3.1-flash-lite, True = gemini-3.1-pro-preview
    var isPrivacyOffline by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Extract Unstructured File",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "OCR • Auto-Categorize • Folder Sort",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onLaunchCamera()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_scan_camera_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan Document with Camera (CameraX)",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets / Samples picker
                Text(
                    text = "Select Document Source / Template",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                sampleFiles.forEach { sample ->
                    val isCurrent = selectedSample == sample
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                selectedSample = sample
                                customFileName = sample.fileName
                                customFileType = sample.fileType
                                customContent = sample.content
                                if (sample.fileName.contains("clinical") || sample.fileName.contains("confidential")) {
                                    isPrivacyOffline = true
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when (sample.fileType) {
                                "PDF" -> Icons.Default.PictureAsPdf
                                "IMAGE" -> Icons.Default.Image
                                "EPUB" -> Icons.AutoMirrored.Filled.MenuBook
                                "VIDEO" -> Icons.Default.VideoLibrary
                                else -> Icons.Default.Description
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sample.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${sample.fileType} • ${sample.description}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Editable text / OCR content
                OutlinedTextField(
                    value = customContent,
                    onValueChange = { customContent = it },
                    label = { Text("Raw Document / OCR Input Data") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("extract_content_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Engine Selection
                Text(
                    text = "Extraction & OCR Engine",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isDeepModel,
                        onClick = { isDeepModel = false },
                        label = { Text("Fast OCR (gemini-3.1-flash-lite)") },
                        leadingIcon = {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("engine_fast_chip")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isDeepModel,
                        onClick = { isDeepModel = true },
                        label = { Text("Deep Reasoning (gemini-3.1-pro-preview)") },
                        leadingIcon = {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("engine_deep_chip")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Privacy Switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPrivacyOffline) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isPrivacyOffline) Color(0xFF92400E) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline Privacy Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isPrivacyOffline) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Keep strictly local in Room DB; bypass cloud sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPrivacyOffline) Color(0xFF78350F) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPrivacyOffline,
                            onCheckedChange = { isPrivacyOffline = it },
                            modifier = Modifier.testTag("privacy_mode_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Submit button
                Button(
                    onClick = {
                        onExtract(
                            customFileName,
                            customFileType,
                            customContent,
                            customFileType == "IMAGE",
                            isDeepModel,
                            isPrivacyOffline
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_extraction_button"),
                    enabled = !isExtracting && customContent.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Extracting & Categorizing...")
                    } else {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extract & Auto-Categorize", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
