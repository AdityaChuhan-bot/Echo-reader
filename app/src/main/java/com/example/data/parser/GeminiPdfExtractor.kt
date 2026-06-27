package com.example.data.parser

import android.content.Context
import android.util.Base64
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiPdfExtractor {
    private const val TAG = "GeminiPdfExtractor"

    // Configure OkHttpClient with a 60-second timeout as recommended for Gemini multimodal processing
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a PDF document to Gemini API via Direct REST and extracts its full text content.
     */
    suspend fun extractTextFromPdf(file: File): String = withContext(Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API Key is not configured. Please add your key in the AI Studio Secrets panel.")
        }

        Log.d(TAG, "Reading PDF file for Gemini extraction: ${file.name}")
        val fileBytes = try {
            FileInputStream(file).use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read file bytes: ${e.message}")
            throw IOException("Failed to read PDF file.", e)
        }

        Log.d(TAG, "Encoding PDF file to Base64 (Size: ${fileBytes.size} bytes)")
        val base64Data = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

        // Construct Request JSON
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        // Text Instruction Part
                        val textPart = JSONObject().apply {
                            put("text", "You are a professional PDF document reader and high-accuracy text extractor. Extract all textual content from the provided PDF document. Keep the exact text, preserve headings, paragraph breaks, lists, and document flow. Do not summarize or paraphrase. Return ONLY the transcribed text of the document from beginning to end.")
                        }
                        put(textPart)

                        // Inline Data Part
                        val inlineDataPart = JSONObject().apply {
                            val inlineDataObj = JSONObject().apply {
                                put("mimeType", "application/pdf")
                                put("data", base64Data)
                            }
                            put("inlineData", inlineDataObj)
                        }
                        put(inlineDataPart)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            // Dynamic generation configuration
            val generationConfig = JSONObject().apply {
                put("temperature", 0.1) // Low temperature for high-accuracy direct transcription
            }
            put("generationConfig", generationConfig)
        }

        val mediaType = "application/json".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        // Direct REST endpoint for gemini-3.5-flash
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        Log.d(TAG, "Sending PDF to Gemini API server-side...")
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Gemini API returned error code ${response.code}: $errorMsg")
                throw IOException("Gemini API error (HTTP ${response.code}): $errorMsg")
            }

            val responseBodyString = response.body?.string()
                ?: throw IOException("Empty response from Gemini API.")

            try {
                val jsonResponse = JSONObject(responseBodyString)
                val candidates = jsonResponse.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val firstPart = parts.getJSONObject(0)
                val extractedText = firstPart.getString("text")

                if (extractedText.isBlank()) {
                    throw IOException("Gemini API returned empty text.")
                }

                Log.d(TAG, "Successfully extracted ${extractedText.length} characters from PDF using Gemini.")
                extractedText
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse Gemini response JSON: ${e.message}", e)
                throw IOException("Failed to parse Gemini server response. Details: ${e.message}", e)
            }
        }
    }
}
