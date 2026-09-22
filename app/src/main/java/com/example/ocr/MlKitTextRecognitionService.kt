package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Data structures representing structured recognition output from ML Kit.
 */
data class TextRecognitionResult(
    val text: String,
    val blocks: List<TextBlockInfo> = emptyList(),
    val wordCount: Int = 0,
    val lineCount: Int = 0,
    val executionTimeMs: Long = 0L
)

data class TextBlockInfo(
    val text: String,
    val boundingBox: Rect? = null,
    val cornerPoints: List<Point> = emptyList(),
    val lines: List<TextLineInfo> = emptyList()
)

data class TextLineInfo(
    val text: String,
    val boundingBox: Rect? = null,
    val elements: List<String> = emptyList()
)

/**
 * Service to process images captured by the CameraX module (or gallery/file)
 * into searchable string text using Google ML Kit on-device Text Recognition.
 */
class MlKitTextRecognitionService(
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
) {
    companion object {
        private const val TAG = "MlKitTextService"

        @Volatile
        private var INSTANCE: MlKitTextRecognitionService? = null

        fun getInstance(): MlKitTextRecognitionService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MlKitTextRecognitionService().also { INSTANCE = it }
            }
        }
    }

    /**
     * Process an Android [Bitmap] into searchable string text.
     */
    suspend fun processBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int = 0
    ): Result<TextRecognitionResult> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        try {
            val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
            processInputImage(inputImage, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create InputImage from Bitmap", e)
            Result.failure(e)
        }
    }

    /**
     * Process an image [File] captured by CameraX into searchable string text.
     */
    suspend fun processFile(
        file: File,
        rotationDegrees: Int = 0
    ): Result<TextRecognitionResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: return@withContext Result.failure(IllegalStateException("Could not decode file: ${file.absolutePath}"))
            val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
            processInputImage(inputImage, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image file: ${file.name}", e)
            Result.failure(e)
        }
    }

    /**
     * Process an image from [Uri] (e.g., from Android Photo Picker or MediaStore).
     */
    suspend fun processUri(
        context: Context,
        uri: Uri
    ): Result<TextRecognitionResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            processInputImage(inputImage, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create InputImage from Uri", e)
            Result.failure(e)
        }
    }

    /**
     * Process a Base64-encoded image string into searchable string text.
     */
    suspend fun processBase64(
        base64String: String,
        rotationDegrees: Int = 0
    ): Result<TextRecognitionResult> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                ?: return@withContext Result.failure(IllegalStateException("Failed to decode Base64 into Bitmap"))
            val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
            processInputImage(inputImage, startTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process Base64 image", e)
            Result.failure(e)
        }
    }

    /**
     * Process a live CameraX [ImageProxy] frame directly (e.g. from an ImageAnalysis use case).
     */
    @OptIn(ExperimentalGetImage::class)
    suspend fun processImageProxy(
        imageProxy: ImageProxy
    ): Result<TextRecognitionResult> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return@withContext Result.failure(IllegalStateException("ImageProxy contained null image"))
        }

        try {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val result = processInputImage(inputImage, startTime)
            imageProxy.close()
            result
        } catch (e: Exception) {
            imageProxy.close()
            Log.e(TAG, "Failed to process ImageProxy", e)
            Result.failure(e)
        }
    }

    /**
     * Core ML Kit recognition call wrapped in a coroutine.
     */
    private suspend fun processInputImage(
        inputImage: InputImage,
        startTime: Long
    ): Result<TextRecognitionResult> = suspendCancellableCoroutine { continuation ->
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val result = mapVisionTextToResult(visionText, startTime)
                continuation.resume(Result.success(result))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "ML Kit OCR failed", exception)
                continuation.resume(Result.failure(exception))
            }
    }

    /**
     * Map ML Kit's Vision [Text] object to our domain model.
     */
    private fun mapVisionTextToResult(visionText: Text, startTime: Long): TextRecognitionResult {
        val fullText = visionText.text
        val blockList = mutableListOf<TextBlockInfo>()
        var totalLines = 0

        for (block in visionText.textBlocks) {
            val lineList = mutableListOf<TextLineInfo>()
            for (line in block.lines) {
                totalLines++
                val elements = line.elements.map { it.text }
                lineList.add(
                    TextLineInfo(
                        text = line.text,
                        boundingBox = line.boundingBox,
                        elements = elements
                    )
                )
            }

            blockList.add(
                TextBlockInfo(
                    text = block.text,
                    boundingBox = block.boundingBox,
                    cornerPoints = block.cornerPoints?.toList() ?: emptyList(),
                    lines = lineList
                )
            )
        }

        val wordCount = fullText.split(Regex("\\s+")).count { it.isNotBlank() }
        val duration = System.currentTimeMillis() - startTime

        return TextRecognitionResult(
            text = fullText,
            blocks = blockList,
            wordCount = wordCount,
            lineCount = totalLines,
            executionTimeMs = duration
        )
    }

    /**
     * Release recognizer resources.
     */
    fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TextRecognizer", e)
        }
    }
}
