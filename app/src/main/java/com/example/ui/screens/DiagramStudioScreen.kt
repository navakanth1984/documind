package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DocuMindViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagramStudioScreen(
    viewModel: DocuMindViewModel,
    onBack: () -> Unit
) {
    val isGenerating by viewModel.isGeneratingDiagram.collectAsState()
    val diagramResult by viewModel.diagramResult.collectAsState()

    var promptInput by remember {
        mutableStateOf("High-resolution scientific diagram illustrating the Krebs cycle in mitochondria with ATP yield labels and enzymes.")
    }

    val samplePrompts = listOf(
        "Krebs Cycle & ATP Yield",
        "Transformer Multi-Head Attention",
        "Bloch Sphere Qubit Vectors",
        "Plato Cave Allegory Diagram",
        "DNA Replication Fork"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Diagram & Illustration Studio",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Model: gemini-3.1-flash-image-preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("diagram_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Info
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Generate or Refine Educational Visuals",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Visual diagrams reinforce student comprehension and can be attached to any study subject or document.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sample Prompts Horizontal Scroll
            Text(
                text = "Preset Prompts for Subjects:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                samplePrompts.forEach { prompt ->
                    FilterChip(
                        selected = promptInput.contains(prompt, ignoreCase = true),
                        onClick = {
                            promptInput = when (prompt) {
                                "Krebs Cycle & ATP Yield" -> "Detailed biochemical pathway flowchart of the Citric Acid Cycle with ATP, NADH, and FADH2 production labeled clearly."
                                "Transformer Multi-Head Attention" -> "Clear architectural diagram of Transformer Self-Attention matrix multiplication Q, K, V with softmax scaling."
                                "Bloch Sphere Qubit Vectors" -> "Geometric 3D sphere diagram showing quantum state |psi> with theta and phi angles on the Bloch sphere."
                                "Plato Cave Allegory Diagram" -> "Philosophical conceptual diagram showing the subterranean cave, fire, shadow puppets, and the ascent to sunlight."
                                else -> "Educational biology schematic of DNA double helix unzipping at the replication fork with DNA polymerase."
                            }
                        },
                        label = { Text(prompt, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prompt Input
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                label = { Text("Visual Prompt & Diagram Instructions") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("diagram_prompt_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.generateDiagram(promptInput) },
                enabled = promptInput.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_diagram_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating with gemini-3.1-flash-image-preview...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate / Edit Diagram", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Diagram Output Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val result = diagramResult
                    if (result?.imageBase64 != null) {
                        val bitmap = remember(result.imageBase64) {
                            try {
                                val bytes = Base64.decode(result.imageBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Generated Diagram",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = "Image rendered successfully (${result.mimeType})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else if (result?.description?.isNotBlank() == true) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Generated Diagram Preview",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Tap 'Generate / Edit Diagram' above to produce visual study aids.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
