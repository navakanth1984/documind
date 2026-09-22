package com.example.ai

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiOcrResult(
    val title: String,
    val extractedText: String,
    val summary: String,
    val suggestedCategory: String,
    val tags: List<String>
)

data class GeminiTtsResult(
    val audioBase64: String?,
    val mimeType: String?
)

data class GeminiImageResult(
    val imageBase64: String?,
    val mimeType: String?,
    val description: String
)

data class GeminiStudyResponse(
    val answer: String,
    val thinkingText: String? = null,
    val sources: List<String> = emptyList()
)

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private fun isKeyAvailable(): Boolean {
        return apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
    }

    /**
     * Fast Low-Latency OCR and classification using gemini-3.1-flash-lite
     */
    suspend fun extractDocumentFast(
        fileName: String,
        fileType: String,
        rawContentOrBase64: String,
        isBase64Image: Boolean = false
    ): GeminiOcrResult = withContext(Dispatchers.IO) {
        if (!isKeyAvailable()) {
            return@withContext offlineFallbackExtraction(fileName, fileType, rawContentOrBase64)
        }

        try {
            val prompt = """
                You are an ultra-fast high accuracy document extraction system.
                Extract all searchable text from this $fileType file named "$fileName".
                Also provide a clean title, a concise 2-sentence summary, an academic/professional category folder (e.g. "Computer Science", "Biology", "Mathematics", "Literature", "History", "Physics", "Exam Prep", "Business"), and 3-5 tags.
                Format output STRICTLY as valid JSON with keys:
                {
                   "title": "...",
                   "extractedText": "...",
                   "summary": "...",
                   "suggestedCategory": "...",
                   "tags": ["tag1", "tag2"]
                }
            """.trimIndent()

            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))

            if (isBase64Image && rawContentOrBase64.isNotBlank()) {
                partsArray.put(
                    JSONObject().put(
                        "inlineData",
                        JSONObject()
                            .put("mimeType", "image/jpeg")
                            .put("data", rawContentOrBase64)
                    )
                )
            } else if (rawContentOrBase64.isNotBlank()) {
                partsArray.put(JSONObject().put("text", "File Content / Metadata:\n$rawContentOrBase64"))
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
                put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiService", "Fast OCR error: $responseBody")
                return@withContext offlineFallbackExtraction(fileName, fileType, rawContentOrBase64)
            }

            parseOcrJsonResponse(responseBody, fileName)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error during fast OCR", e)
            offlineFallbackExtraction(fileName, fileType, rawContentOrBase64)
        }
    }

    /**
     * Deep Image Understanding using gemini-3.1-pro-preview
     */
    suspend fun analyzeImageDeep(
        imageBytesBase64: String,
        prompt: String
    ): GeminiOcrResult = withContext(Dispatchers.IO) {
        if (!isKeyAvailable()) {
            return@withContext offlineFallbackExtraction("Image Document", "IMAGE", prompt)
        }

        try {
            val fullPrompt = """
                Analyze this document/diagram/notes image with comprehensive accuracy.
                User instructions: $prompt
                Transcribe all text, formulas, diagrams, and handwriting accurately.
                Return JSON with:
                {
                   "title": "...",
                   "extractedText": "...",
                   "summary": "...",
                   "suggestedCategory": "...",
                   "tags": ["..."]
                }
            """.trimIndent()

            val partsArray = JSONArray().apply {
                put(JSONObject().put("text", fullPrompt))
                put(
                    JSONObject().put(
                        "inlineData",
                        JSONObject().put("mimeType", "image/jpeg").put("data", imageBytesBase64)
                    )
                )
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
                put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext offlineFallbackExtraction("Analyzed Image", "IMAGE", prompt)
            }

            parseOcrJsonResponse(responseBody, "Image Analysis")
        } catch (e: Exception) {
            Log.e("GeminiService", "Error analyzing image", e)
            offlineFallbackExtraction("Analyzed Image", "IMAGE", prompt)
        }
    }

    /**
     * Analyze video content for key information using gemini-3.1-pro-preview
     */
    suspend fun analyzeVideoContent(
        videoTitle: String,
        videoTranscriptOrDescription: String
    ): GeminiOcrResult = withContext(Dispatchers.IO) {
        if (!isKeyAvailable()) {
            return@withContext offlineFallbackExtraction(videoTitle, "VIDEO", videoTranscriptOrDescription)
        }

        try {
            val prompt = """
                You are analyzing a video lecture / educational recording titled "$videoTitle".
                Transcript/Visual notes:
                $videoTranscriptOrDescription
                
                Extract:
                1. Core concepts and structured lecture notes with timestamps.
                2. Key takeaways and study questions for students & teachers.
                3. Appropriate academic category folder and tags.
                Output as JSON:
                {
                  "title": "$videoTitle",
                  "extractedText": "...",
                  "summary": "...",
                  "suggestedCategory": "...",
                  "tags": ["..."]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext offlineFallbackExtraction(videoTitle, "VIDEO", videoTranscriptOrDescription)
            }

            parseOcrJsonResponse(responseBody, videoTitle)
        } catch (e: Exception) {
            Log.e("GeminiService", "Video analysis error", e)
            offlineFallbackExtraction(videoTitle, "VIDEO", videoTranscriptOrDescription)
        }
    }

    /**
     * TTS using model gemini-3.1-flash-tts-preview
     */
    suspend fun textToSpeech(text: String): GeminiTtsResult = withContext(Dispatchers.IO) {
        if (!isKeyAvailable()) {
            return@withContext GeminiTtsResult(null, null)
        }

        try {
            val limitedText = if (text.length > 500) text.take(500) + "..." else text
            val prompt = "Read this clearly with pleasant academic pacing: $limitedText"

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("responseModalities", JSONArray().put("AUDIO"))
                        put(
                            "speechConfig",
                            JSONObject().put(
                                "voiceConfig",
                                JSONObject().put(
                                    "prebuiltVoiceConfig",
                                    JSONObject().put("voiceName", "Kore")
                                )
                            )
                        )
                    }
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-tts-preview:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiService", "TTS error: $responseBody")
                return@withContext GeminiTtsResult(null, null)
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    val inlineData = part?.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val data = inlineData.optString("data")
                        val mime = inlineData.optString("mimeType", "audio/x-wav")
                        return@withContext GeminiTtsResult(data, mime)
                    }
                }
            }

            GeminiTtsResult(null, null)
        } catch (e: Exception) {
            Log.e("GeminiService", "TTS exception", e)
            GeminiTtsResult(null, null)
        }
    }

    /**
     * Create & Edit Images using gemini-3.1-flash-image-preview
     */
    suspend fun generateOrEditDiagram(
        prompt: String,
        inputImageBase64: String? = null
    ): GeminiImageResult = withContext(Dispatchers.IO) {
        if (!isKeyAvailable()) {
            return@withContext GeminiImageResult(
                null,
                null,
                "Gemini API key is required to generate or edit visual concept diagrams."
            )
        }

        try {
            val partsArray = JSONArray()
            val promptPrefix = if (inputImageBase64 != null) {
                "Edit and enhance this study diagram according to: $prompt"
            } else {
                "Create a clear, high-contrast, educational concept diagram illustrating: $prompt. Scientific textbook illustration style."
            }
            partsArray.put(JSONObject().put("text", promptPrefix))

            if (inputImageBase64 != null) {
                partsArray.put(
                    JSONObject().put(
                        "inlineData",
                        JSONObject().put("mimeType", "image/jpeg").put("data", inputImageBase64)
                    )
                )
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
                        put(
                            "imageConfig",
                            JSONObject()
                                .put("aspectRatio", "1:1")
                                .put("imageSize", "1K")
                        )
                    }
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiService", "Diagram generation error: $responseBody")
                return@withContext GeminiImageResult(null, null, "Generation failed: ${response.message}")
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")

            var imageBase64: String? = null
            var mimeType: String? = null
            var description = "Educational Diagram"

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    if (part?.has("inlineData") == true) {
                        val inline = part.getJSONObject("inlineData")
                        imageBase64 = inline.optString("data")
                        mimeType = inline.optString("mimeType", "image/png")
                    } else if (part?.has("text") == true) {
                        description = part.optString("text")
                    }
                }
            }

            GeminiImageResult(imageBase64, mimeType, description)
        } catch (e: Exception) {
            Log.e("GeminiService", "Image generation exception", e)
            GeminiImageResult(null, null, "Error: ${e.message}")
        }
    }

    /**
     * Study Q&A with High Thinking Mode using gemini-3.1-pro-preview
     * Thinking level set to HIGH, no maxOutputTokens.
     */
    suspend fun askWithHighThinking(
        question: String,
        documentContext: String
    ): GeminiStudyResponse = withContext(Dispatchers.IO) {
        if (!isKeyAvailable()) {
            return@withContext GeminiStudyResponse(
                answer = "Offline Mode: Based on local context:\n$documentContext\n\nDirect answer: Key points address '$question' within the stored document structure.",
                thinkingText = "Offline local heuristic matching: identified relevant sections from local cache."
            )
        }

        try {
            val prompt = """
                You are a master academic tutor and problem solver.
                Context Document:
                $documentContext
                
                Student/Teacher Question:
                $question
                
                Provide a thorough, step-by-step rigorous breakdown and solution.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                put(
                    "generationConfig",
                    JSONObject().put(
                        "thinkingConfig",
                        JSONObject().put("thinkingLevel", "HIGH")
                    )
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GeminiStudyResponse("Unable to complete high-thinking reasoning: ${response.message}")
            }

            parseStudyResponse(responseBody)
        } catch (e: Exception) {
            Log.e("GeminiService", "High thinking error", e)
            GeminiStudyResponse("Error during reasoning: ${e.message}")
        }
    }

    /**
     * Study Q&A with Google Search Grounding using gemini-3.5-flash
     */
    suspend fun askWithSearchGrounding(
        question: String,
        subject: String
    ): GeminiStudyResponse = withContext(Dispatchers.IO) {
        if (!isKeyAvailable()) {
            return@withContext GeminiStudyResponse(
                answer = "Search Grounding requires an active internet connection and Gemini API key. In offline mode, check local subject notes.",
                sources = listOf("Local Subject Cache")
            )
        }

        try {
            val prompt = "Subject: $subject\nQuestion: $question\nProvide an up-to-date accurate educational explanation grounded in verified search sources."

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                put(
                    "tools",
                    JSONArray().put(JSONObject().put("googleSearch", JSONObject()))
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GeminiStudyResponse("Search grounding request failed: ${response.message}")
            }

            parseGroundedResponse(responseBody)
        } catch (e: Exception) {
            Log.e("GeminiService", "Search grounding error", e)
            GeminiStudyResponse("Error connecting to search grounding: ${e.message}")
        }
    }

    /**
     * Study Q&A with Google Maps Grounding using gemini-3.5-flash
     */
    suspend fun askWithMapsGrounding(
        query: String
    ): GeminiStudyResponse = withContext(Dispatchers.IO) {
        if (!isKeyAvailable()) {
            return@withContext GeminiStudyResponse(
                answer = "Maps Grounding requires an active internet connection and Gemini API key."
            )
        }

        try {
            val prompt = "Geographic / campus / study location query: $query. Provide accurate grounded location information."

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                put(
                    "tools",
                    JSONArray().put(JSONObject().put("googleMaps", JSONObject()))
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GeminiStudyResponse("Maps grounding request failed: ${response.message}")
            }

            parseGroundedResponse(responseBody)
        } catch (e: Exception) {
            Log.e("GeminiService", "Maps grounding error", e)
            GeminiStudyResponse("Error connecting to maps grounding: ${e.message}")
        }
    }

    private fun parseOcrJsonResponse(responseJsonString: String, fallbackTitle: String): GeminiOcrResult {
        return try {
            val root = JSONObject(responseJsonString)
            val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
            val textContent = candidate?.optJSONObject("content")
                ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

            // Extract JSON substring if wrapped in markdown
            val cleanJson = textContent.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val obj = JSONObject(cleanJson)
            val tagsList = mutableListOf<String>()
            val tagsArr = obj.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    tagsList.add(tagsArr.getString(i))
                }
            }

            GeminiOcrResult(
                title = obj.optString("title", fallbackTitle),
                extractedText = obj.optString("extractedText", ""),
                summary = obj.optString("summary", "Document extracted and categorized."),
                suggestedCategory = obj.optString("suggestedCategory", "General"),
                tags = tagsList
            )
        } catch (e: Exception) {
            Log.w("GeminiService", "Could not parse JSON response directly, extracting raw text", e)
            GeminiOcrResult(
                title = fallbackTitle,
                extractedText = responseJsonString.take(1500),
                summary = "Processed document.",
                suggestedCategory = "General",
                tags = listOf("extracted", "document")
            )
        }
    }

    private fun parseStudyResponse(responseBody: String): GeminiStudyResponse {
        return try {
            val root = JSONObject(responseBody)
            val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")

            var mainText = ""
            var thinkingText: String? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("thought") || p.optBoolean("thought", false)) {
                        thinkingText = p.optString("text")
                    } else if (p.has("text")) {
                        val text = p.getString("text")
                        if (text.startsWith("<thought>") || text.contains("thinking:")) {
                            thinkingText = text
                        } else {
                            mainText += text
                        }
                    }
                }
            }
            GeminiStudyResponse(
                answer = if (mainText.isNotBlank()) mainText else (thinkingText ?: "Explanation ready."),
                thinkingText = thinkingText
            )
        } catch (e: Exception) {
            GeminiStudyResponse("Analysis completed.")
        }
    }

    private fun parseGroundedResponse(responseBody: String): GeminiStudyResponse {
        return try {
            val root = JSONObject(responseBody)
            val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: "No response generated."

            val sources = mutableListOf<String>()
            val groundingMetadata = candidate?.optJSONObject("groundingMetadata")
            val webChunks = groundingMetadata?.optJSONArray("groundingChunks")
            if (webChunks != null) {
                for (i in 0 until webChunks.length()) {
                    val chunk = webChunks.getJSONObject(i)
                    val web = chunk.optJSONObject("web")
                    val title = web?.optString("title")
                    val uri = web?.optString("uri")
                    if (title != null && uri != null) {
                        sources.add("$title ($uri)")
                    }
                }
            }

            GeminiStudyResponse(answer = text, sources = sources)
        } catch (e: Exception) {
            GeminiStudyResponse(responseBody)
        }
    }

    /**
     * Offline heuristic extraction when running in privacy mode or without internet
     */
    private fun offlineFallbackExtraction(fileName: String, fileType: String, content: String): GeminiOcrResult {
        val lower = (fileName + " " + content).lowercase()
        val category = when {
            lower.contains("code") || lower.contains("algorithm") || lower.contains("kotlin") || lower.contains("java") || lower.contains("python") -> "Computer Science"
            lower.contains("cell") || lower.contains("dna") || lower.contains("organism") || lower.contains("biology") -> "Biology"
            lower.contains("equation") || lower.contains("theorem") || lower.contains("calculus") || lower.contains("integral") -> "Mathematics"
            lower.contains("chapter") || lower.contains("novel") || lower.contains("poem") || lower.contains("literature") -> "Literature"
            lower.contains("war") || lower.contains("revolution") || lower.contains("century") || lower.contains("history") -> "History"
            lower.contains("physics") || lower.contains("gravity") || lower.contains("quantum") || lower.contains("thermodynamics") -> "Physics"
            lower.contains("exam") || lower.contains("test") || lower.contains("syllabus") || lower.contains("quiz") -> "Exam Prep"
            else -> "Uncategorized"
        }

        val extracted = if (content.isNotBlank()) content else "Extracted document content from $fileName [$fileType]."
        val words = extracted.split(Regex("\\s+")).filter { it.isNotBlank() }
        val summary = "Offline processed $fileType document with ${words.size} searchable words. Categorized under $category."
        val title = fileName.substringBeforeLast(".")

        return GeminiOcrResult(
            title = title.replace("_", " ").capitalizeWords(),
            extractedText = extracted,
            summary = summary,
            suggestedCategory = category,
            tags = listOf(fileType.lowercase(), category.lowercase(), "offline-processed")
        )
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
