package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ocr.TextRecognitionResult
import com.example.ui.viewmodel.DocuMindViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScanOcrMode {
    ML_KIT_ON_DEVICE,
    GEMINI_FAST,
    GEMINI_DEEP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    viewModel: DocuMindViewModel,
    onBack: () -> Unit,
    onScanComplete: (documentId: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager = LocalClipboardManager.current

    val isExtracting by viewModel.isExtracting.collectAsState()
    val extractStatusMessage by viewModel.extractStatusMessage.collectAsState()
    val mlKitResult by viewModel.mlKitOcrResult.collectAsState()
    val isMlKitProcessing by viewModel.isMlKitProcessing.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedBase64 by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Extraction settings
    var scanOcrMode by remember { mutableStateOf(ScanOcrMode.ML_KIT_ON_DEVICE) }
    var isPrivacyOffline by remember { mutableStateOf(false) }
    var recognizedSearchQuery by remember { mutableStateOf("") }
    var copyNotice by remember { mutableStateOf(false) }

    // Photo picker for fallback/gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        capturedBitmap = bitmap
                        capturedBase64 = base64
                        viewModel.processImageWithMlKit(bitmap)
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraScanScreen", "Error reading selected media", e)
            }
        }
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl: Camera? by remember { mutableStateOf(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // Bind camera lifecycle
    LaunchedEffect(hasCameraPermission, lensFacing) {
        if (hasCameraPermission && capturedBitmap == null) {
            try {
                val cameraProvider = withContext(Dispatchers.IO) {
                    ProcessCameraProvider.getInstance(context).get()
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                cameraControl = camera
            } catch (e: Exception) {
                Log.e("CameraScanScreen", "Camera binding failed", e)
            }
        }
    }

    // Handle Torch
    LaunchedEffect(isTorchOn) {
        cameraControl?.cameraControl?.enableTorch(isTorchOn)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (capturedBitmap != null) "Review Document Photo" else "Document Scanner (CameraX)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = if (capturedBitmap != null) "Verify framing before OCR extraction" else "Align document, notes, or whiteboard in viewfinder",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("camera_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (capturedBitmap == null && hasCameraPermission) {
                        IconButton(
                            onClick = { isTorchOn = !isTorchOn },
                            modifier = Modifier.testTag("toggle_torch_btn")
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Torch",
                                tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier.testTag("switch_camera_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.85f))
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasCameraPermission) {
                // Camera Permission Needed State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "DocuMind uses CameraX to capture high-clarity photos of textbooks, study notes, handwritten equations, and research diagrams for automated OCR transcription.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("grant_camera_permission_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Camera Access", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pick_photo_fallback_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Photo from Gallery")
                    }
                }
            } else if (capturedBitmap != null) {
                // Photo Review & ML Kit Searchable Text Extraction
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Document Image Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Document",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        // Resolution / Status tag
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "${capturedBitmap!!.width}×${capturedBitmap!!.height}px",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ML Kit On-Device Searchable Text Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            .testTag("mlkit_searchable_text_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TextSnippet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Searchable String Text",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                if (isMlKitProcessing) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ML Kit running...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else if (mlKitResult != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B)
                                    ) {
                                        Text(
                                            text = "⚡ ${mlKitResult!!.executionTimeMs}ms • ML Kit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (mlKitResult != null) {
                                val result = mlKitResult!!
                                // Stats pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E293B)
                                    ) {
                                        Text(
                                            text = "${result.wordCount} words",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E293B)
                                    ) {
                                        Text(
                                            text = "${result.lineCount} lines",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E293B)
                                    ) {
                                        Text(
                                            text = "${result.blocks.size} blocks",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // In-Card Search filter for searchable string text verification
                                OutlinedTextField(
                                    value = recognizedSearchQuery,
                                    onValueChange = { recognizedSearchQuery = it },
                                    placeholder = {
                                        Text("Search in recognized text...", fontSize = 12.sp, color = Color.Gray)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    },
                                    trailingIcon = {
                                        if (recognizedSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { recognizedSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("mlkit_text_search_field"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Text Display
                                val displayedText = remember(result.text, recognizedSearchQuery) {
                                    if (recognizedSearchQuery.isBlank()) {
                                        result.text
                                    } else {
                                        val matchingLines = result.text.lines().filter {
                                            it.contains(recognizedSearchQuery, ignoreCase = true)
                                        }
                                        if (matchingLines.isEmpty()) {
                                            "No lines matching \"$recognizedSearchQuery\""
                                        } else {
                                            matchingLines.joinToString("\n")
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = if (displayedText.isBlank()) "(No text detected in frame)" else displayedText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (displayedText.isBlank()) Color.Gray else Color(0xFFE2E8F0),
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (copyNotice) "Copied to clipboard!" else "Ready for search & indexing",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (copyNotice) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.6f)
                                    )

                                    FilledTonalButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(result.text))
                                            copyNotice = true
                                        },
                                        modifier = Modifier.testTag("copy_mlkit_text_btn")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Text", fontSize = 11.sp)
                                    }
                                }
                            } else {
                                Text(
                                    text = if (isMlKitProcessing) "Analyzing text characters on device..." else "Processing OCR with Google ML Kit...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // OCR Engine Selection Chips
                    Text(
                        text = "Extraction Engine",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = scanOcrMode == ScanOcrMode.ML_KIT_ON_DEVICE,
                            onClick = { scanOcrMode = ScanOcrMode.ML_KIT_ON_DEVICE },
                            label = { Text("⚡ ML Kit (Offline)", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f).testTag("camera_mlkit_ocr_chip")
                        )

                        FilterChip(
                            selected = scanOcrMode == ScanOcrMode.GEMINI_FAST,
                            onClick = { scanOcrMode = ScanOcrMode.GEMINI_FAST },
                            label = { Text("Fast Cloud", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f).testTag("camera_fast_ocr_chip")
                        )

                        FilterChip(
                            selected = scanOcrMode == ScanOcrMode.GEMINI_DEEP,
                            onClick = { scanOcrMode = ScanOcrMode.GEMINI_DEEP },
                            label = { Text("Deep Reasoning", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f).testTag("camera_deep_ocr_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Offline Privacy Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPrivacyOffline) Color(0xFF78350F) else Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPrivacyOffline) Icons.Default.Lock else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isPrivacyOffline) Color(0xFFFDE68A) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline Privacy Mode",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Store locally in Room DB; exclude from cloud sync",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = isPrivacyOffline,
                                onCheckedChange = { isPrivacyOffline = it },
                                modifier = Modifier.testTag("camera_privacy_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Retake & Extract Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                capturedBitmap = null
                                capturedBase64 = null
                                viewModel.clearMlKitResult()
                                recognizedSearchQuery = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("retake_photo_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retake")
                        }

                        Button(
                            onClick = {
                                capturedBase64?.let { base64 ->
                                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    val fileName = "scan_doc_$timeStamp.jpg"
                                    if (scanOcrMode == ScanOcrMode.ML_KIT_ON_DEVICE) {
                                        viewModel.extractDocumentWithMlKit(
                                            fileName = fileName,
                                            base64Image = base64,
                                            isPrivacyOffline = isPrivacyOffline,
                                            onSuccess = { doc ->
                                                onScanComplete(doc.id)
                                            }
                                        )
                                    } else {
                                        viewModel.extractDocument(
                                            fileName = fileName,
                                            fileType = "IMAGE",
                                            content = base64,
                                            isBase64Image = true,
                                            isDeepAnalysis = (scanOcrMode == ScanOcrMode.GEMINI_DEEP),
                                            isPrivacyOffline = isPrivacyOffline,
                                            onSuccess = { doc ->
                                                onScanComplete(doc.id)
                                            }
                                        )
                                    }
                                }
                            },
                            enabled = !isExtracting && capturedBase64 != null,
                            modifier = Modifier
                                .weight(1.6f)
                                .height(50.dp)
                                .testTag("confirm_extract_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isExtracting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(extractStatusMessage?.ifBlank { "Indexing..." } ?: "Indexing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (scanOcrMode == ScanOcrMode.ML_KIT_ON_DEVICE) "Save & Index (ML Kit)" else "Extract OCR",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Live Camera Viewfinder
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Viewfinder Framing Guide Overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Calculate centered rectangular document scan frame (aspect ratio 3:4)
                        val frameWidth = canvasWidth * 0.85f
                        val frameHeight = frameWidth * 1.35f
                        val left = (canvasWidth - frameWidth) / 2
                        val top = (canvasHeight - frameHeight) / 2.3f

                        // Draw corner brackets
                        val bracketLength = 36.dp.toPx()
                        val strokeWidth = 3.dp.toPx()
                        val bracketColor = androidx.compose.ui.graphics.Color(0xFF818CF8) // Indigo highlight

                        // Top Left
                        drawLine(bracketColor, Offset(left, top), Offset(left + bracketLength, top), strokeWidth)
                        drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLength), strokeWidth)

                        // Top Right
                        drawLine(bracketColor, Offset(left + frameWidth, top), Offset(left + frameWidth - bracketLength, top), strokeWidth)
                        drawLine(bracketColor, Offset(left + frameWidth, top), Offset(left + frameWidth, top + bracketLength), strokeWidth)

                        // Bottom Left
                        drawLine(bracketColor, Offset(left, top + frameHeight), Offset(left + bracketLength, top + frameHeight), strokeWidth)
                        drawLine(bracketColor, Offset(left, top + frameHeight), Offset(left, top + frameHeight - bracketLength), strokeWidth)

                        // Bottom Right
                        drawLine(bracketColor, Offset(left + frameWidth, top + frameHeight), Offset(left + frameWidth - bracketLength, top + frameHeight), strokeWidth)
                        drawLine(bracketColor, Offset(left + frameWidth, top + frameHeight), Offset(left + frameWidth, top + frameHeight - bracketLength), strokeWidth)
                    }

                    // Framing instruction pill & ML Kit ready badge
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Align document within bounding frame",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Google ML Kit OCR Ready",
                                    color = Color(0xFF38BDF8),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Bottom Camera Controls
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gallery Picker fallback
                            FilledTonalIconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("gallery_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Pick Photo from Gallery",
                                    tint = Color.White
                                )
                            }

                            // Capture Shutter Button
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, Color.White, CircleShape)
                                    .padding(5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = {
                                        if (!isCapturing) {
                                            isCapturing = true
                                            takePhoto(
                                                context = context,
                                                imageCapture = imageCapture,
                                                onImageCaptured = { bitmap, base64 ->
                                                    isCapturing = false
                                                    capturedBitmap = bitmap
                                                    capturedBase64 = base64
                                                    viewModel.processImageWithMlKit(bitmap)
                                                },
                                                onError = {
                                                    isCapturing = false
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("shutter_capture_button"),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCapturing) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                ) {
                                    if (isCapturing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }

                            // Close / Cancel button
                            FilledTonalIconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("cancel_scanner_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Scan",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Bitmap, String) -> Unit,
    onError: (Exception) -> Unit
) {
    val tempFile = try {
        File.createTempFile("camera_scan_", ".jpg", context.cacheDir)
    } catch (e: Exception) {
        onError(e)
        return
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                try {
                    val bytes = tempFile.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    tempFile.delete()
                    if (bitmap != null) {
                        onImageCaptured(bitmap, base64)
                    } else {
                        onError(IllegalStateException("Failed to decode captured image bitmap"))
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScanScreen", "CameraX photo capture failed", exception)
                onError(exception)
            }
        }
    )
}
