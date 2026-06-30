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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
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
     * Extracts text from PDF. Prioritizes local, high-speed on-device offline extraction.
     * Falls back to Gemini API only if local text extraction yields no content and a valid API key is available.
     */
    suspend fun extractTextFromPdf(file: File): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting local 100% offline text extraction first...")
        try {
            val localText = extractTextLocally(file)
            if (localText.isNotBlank() && !localText.startsWith("Could not extract local text")) {
                Log.d(TAG, "Successfully extracted ${localText.length} characters using 100% offline local parser.")
                return@withContext localText
            }
        } catch (e: Exception) {
            Log.w(TAG, "Local offline extraction failed: ${e.message}. Trying cloud API fallback if key exists.", e)
        }

        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is not configured. Returning local parser status/fallback.")
            return@withContext extractTextLocally(file)
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

    /**
     * Extracts plain text from a PDF file locally and 100% offline.
     */
    private fun extractTextLocally(file: File): String {
        val bytes = file.readBytes()
        val textBuilder = StringBuilder()

        var index = 0
        while (index < bytes.size) {
            // Search for "stream" keyword
            if (index + 6 < bytes.size &&
                bytes[index] == 's'.code.toByte() &&
                bytes[index + 1] == 't'.code.toByte() &&
                bytes[index + 2] == 'r'.code.toByte() &&
                bytes[index + 3] == 'e'.code.toByte() &&
                bytes[index + 4] == 'a'.code.toByte() &&
                bytes[index + 5] == 'm'.code.toByte()
            ) {
                // Find start of stream data (usually after stream\r\n or stream\n)
                var streamDataStart = index + 6
                while (streamDataStart < bytes.size && (bytes[streamDataStart] == '\r'.code.toByte() || bytes[streamDataStart] == '\n'.code.toByte())) {
                    streamDataStart++
                }

                // Find end of stream ("endstream")
                var streamDataEnd = streamDataStart
                while (streamDataEnd + 9 < bytes.size) {
                    if (bytes[streamDataEnd] == 'e'.code.toByte() &&
                        bytes[streamDataEnd + 1] == 'n'.code.toByte() &&
                        bytes[streamDataEnd + 2] == 'd'.code.toByte() &&
                        bytes[streamDataEnd + 3] == 's'.code.toByte() &&
                        bytes[streamDataEnd + 4] == 't'.code.toByte() &&
                        bytes[streamDataEnd + 5] == 'r'.code.toByte() &&
                        bytes[streamDataEnd + 6] == 'e'.code.toByte() &&
                        bytes[streamDataEnd + 7] == 'a'.code.toByte() &&
                        bytes[streamDataEnd + 8] == 'm'.code.toByte()
                    ) {
                        break
                    }
                    streamDataEnd++
                }

                if (streamDataEnd > streamDataStart) {
                    val streamBytes = bytes.copyOfRange(streamDataStart, streamDataEnd)

                    // Look back from index to find if it is FlateDecode
                    val lookbackStart = maxOf(0, index - 150)
                    val lookbackString = String(bytes.copyOfRange(lookbackStart, index), Charsets.US_ASCII)
                    val isFlateDecoded = lookbackString.contains("/FlateDecode") || lookbackString.contains("/Fl")

                    val decompressedBytes = if (isFlateDecoded) {
                        try {
                            decompressFlate(streamBytes)
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        streamBytes
                    }

                    if (decompressedBytes != null) {
                        val parsedText = parseTextFromContentStream(decompressedBytes)
                        if (parsedText.isNotEmpty()) {
                            textBuilder.append(parsedText).append("\n")
                        }
                    }
                }

                index = streamDataEnd + 9
            } else {
                index++
            }
        }

        val result = textBuilder.toString().trim()
        if (result.isEmpty()) {
            return fallbackRegexExtract(bytes)
        }
        return result
    }

    private fun decompressFlate(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        try {
            val iis = InflaterInputStream(ByteArrayInputStream(data))
            val buffer = ByteArray(4096)
            var len: Int
            while (iis.read(buffer).also { len = it } > 0) {
                bos.write(buffer, 0, len)
            }
            return bos.toByteArray()
        } catch (e1: Exception) {
            val bos2 = ByteArrayOutputStream()
            try {
                val iis2 = InflaterInputStream(ByteArrayInputStream(data))
                val buffer = ByteArray(4096)
                var len: Int
                while (iis2.read(buffer).also { len = it } > 0) {
                    bos2.write(buffer, 0, len)
                }
                return bos2.toByteArray()
            } catch (e2: Exception) {
                val bos3 = ByteArrayOutputStream()
                val inf = Inflater()
                inf.setInput(data)
                val buffer = ByteArray(4096)
                try {
                    while (!inf.finished()) {
                        val count = inf.inflate(buffer)
                        if (count == 0) break
                        bos3.write(buffer, 0, count)
                    }
                    inf.end()
                    return bos3.toByteArray()
                } catch (e3: Exception) {
                    inf.end()
                    throw e3
                }
            }
        }
    }

    private fun parseTextFromContentStream(streamBytes: ByteArray): String {
        val textBuilder = StringBuilder()
        val content = String(streamBytes, Charsets.UTF_8)

        // Scan for BT ... ET blocks
        var btIndex = 0
        while (true) {
            btIndex = content.indexOf("BT", btIndex)
            if (btIndex == -1) break

            val etIndex = content.indexOf("ET", btIndex)
            if (etIndex == -1) break

            val textBlock = content.substring(btIndex + 2, etIndex)

            var i = 0
            while (i < textBlock.length) {
                if (textBlock[i] == '(') {
                    i++
                    val start = i
                    var escape = false
                    while (i < textBlock.length) {
                        if (escape) {
                            escape = false
                        } else if (textBlock[i] == '\\') {
                            escape = true
                        } else if (textBlock[i] == ')') {
                            break
                        }
                        i++
                    }
                    if (i < textBlock.length) {
                        val extracted = textBlock.substring(start, i)
                        val cleaned = extracted
                            .replace("\\(", "(")
                            .replace("\\)", ")")
                            .replace("\\r", "")
                            .replace("\\n", "\n")
                        textBuilder.append(cleaned)
                    }
                } else if (textBlock[i] == 'T' && i + 1 < textBlock.length && (textBlock[i+1] == 'j' || textBlock[i+1] == 'J')) {
                    textBuilder.append(" ")
                }
                i++
            }
            textBuilder.append("\n")
            btIndex = etIndex + 2
        }

        return textBuilder.toString()
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
            .trim()
    }

    private fun fallbackRegexExtract(bytes: ByteArray): String {
        val content = String(bytes, Charsets.ISO_8859_1)
        val matches = Regex("\\(([^)]+)\\)").findAll(content)
        val sb = StringBuilder()
        for (match in matches) {
            val text = match.groupValues[1]
            if (text.length > 2 && text.any { it.isLetter() }) {
                sb.append(text).append(" ")
            }
        }
        val result = sb.toString().trim()
        return if (result.isNotEmpty()) result else "Could not extract local text from PDF. Ensure it is a text-based PDF."
    }
}
