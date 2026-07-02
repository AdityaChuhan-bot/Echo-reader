package com.example.data.tts

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class KittenTtsService(private val context: Context) {
    private val TAG = "KittenTtsService"
    private val prefs = context.getSharedPreferences("audiobook_model_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun setBaseUrl(url: String) {
        prefs.edit().putString("kitten_tts_base_url", url.trim()).apply()
    }

    fun getBaseUrl(): String = prefs.getString("kitten_tts_base_url", "")?.trim().orEmpty()

    fun isConfigured(): Boolean = getBaseUrl().isNotBlank()

    suspend fun synthesizeToFile(
        text: String,
        voiceId: String,
        speed: Float,
        targetFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext false

        val baseUrl = getBaseUrl()
        if (baseUrl.isBlank()) {
            Log.d(TAG, "No KittenTTS endpoint configured; skipping remote synthesis")
            return@withContext false
        }

        val candidateUrls = buildCandidateUrls(baseUrl)
        for (candidateUrl in candidateUrls) {
            val success = trySynthesize(candidateUrl, text, voiceId, speed, targetFile)
            if (success) {
                return@withContext true
            }
        }

        false
    }

    private fun trySynthesize(
        endpoint: String,
        text: String,
        voiceId: String,
        speed: Float,
        targetFile: File
    ): Boolean {
        val requestBody = createJsonPayload(text, voiceId, speed).toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .header("Accept", "audio/wav")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "KittenTTS request failed for $endpoint with code ${response.code}")
                    return false
                }

                val body = response.body ?: run {
                    Log.w(TAG, "KittenTTS returned no audio body for $endpoint")
                    return false
                }

                body.byteStream().use { input ->
                    targetFile.parentFile?.mkdirs()
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (!targetFile.exists() || targetFile.length() == 0L) {
                    Log.w(TAG, "KittenTTS endpoint returned an empty file for $endpoint")
                    false
                } else {
                    Log.d(TAG, "KittenTTS audio synthesized successfully from $endpoint")
                    true
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "KittenTTS remote synthesis failed for $endpoint", t)
            false
        }
    }

    private fun createJsonPayload(text: String, voiceId: String, speed: Float): String {
        val escapedText = escapeJson(text)
        val escapedVoice = escapeJson(voiceId)
        return "{\"text\":\"$escapedText\",\"voice\":\"$escapedVoice\",\"speed\":$speed,\"format\":\"wav\"}"
    }

    private fun escapeJson(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}

internal object KittenTtsEndpointBuilder {
    fun buildCandidateUrls(baseUrl: String): List<String> {
        val trimmed = baseUrl.trim().removeSuffix("/")
        if (trimmed.isBlank()) return emptyList()

        val direct = trimmed
        val synthesize = if (trimmed.endsWith("/synthesize")) trimmed else "$trimmed/synthesize"
        val apiSynthesize = if (trimmed.endsWith("/api/synthesize")) trimmed else "$trimmed/api/synthesize"
        return listOf(direct, synthesize, apiSynthesize).distinct()
    }
}
