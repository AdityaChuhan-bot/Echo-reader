package com.example.data.tts

import android.content.Context
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * High-performance on-device Audio Cache Manager.
 * Prevents re-synthesis of sentences, optimizes battery consumption,
 * and enables instant offline replay.
 */
class AudioCacheManager(private val context: Context) {
    private val TAG = "AudioCacheManager"
    private val cacheDir = File(context.cacheDir, "kitten_tts_cache").apply {
        if (!exists()) mkdirs()
    }
    private val maxCacheSize = 100 * 1024 * 1024 // 100 MB limit for devices with 4-8GB RAM

    init {
        Log.d(TAG, "Initialized cache directory at: ${cacheDir.absolutePath}")
        trimCacheIfNeeded()
    }

    /**
     * Generates a unique, deterministic key for a sentence given its parameters.
     */
    fun getCacheFile(text: String, voiceId: String, speed: Float, readingMode: String): File {
        val rawKey = "$text|$voiceId|$speed|$readingMode"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(rawKey.toByteArray())
        val hexKey = digest.joinToString("") { "%02x".format(it) }
        return File(cacheDir, "tts_$hexKey.wav")
    }

    /**
     * Checks if a sentence is already synthesized and cached.
     */
    fun isCached(text: String, voiceId: String, speed: Float, readingMode: String): Boolean {
        val file = getCacheFile(text, voiceId, speed, readingMode)
        return file.exists() && file.length() > 0
    }

    /**
     * Retains the file in the cache and registers its usage.
     */
    fun registerNewCacheFile() {
        trimCacheIfNeeded()
    }

    /**
     * Clears all cached voice segments.
     */
    fun clearCache() {
        try {
            val files = cacheDir.listFiles() ?: return
            for (file in files) {
                file.delete()
            }
            Log.d(TAG, "Audio cache successfully cleared.")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}")
        }
    }

    /**
     * Trims old files to stay within the 100MB limit.
     * Uses LRU (Least Recently Used) approximation based on lastModified.
     */
    private fun trimCacheIfNeeded() {
        try {
            val files = cacheDir.listFiles() ?: return
            val totalSize = files.sumOf { it.length() }
            if (totalSize > maxCacheSize) {
                Log.d(TAG, "Cache size is ${totalSize / (1024 * 1024)} MB. Exceeds limit of ${maxCacheSize / (1024 * 1024)} MB. Trimming...")
                // Sort by last modified (oldest first)
                val sortedFiles = files.sortedBy { it.lastModified() }
                var currentSize = totalSize
                for (file in sortedFiles) {
                    if (currentSize <= maxCacheSize * 0.7) break // Trim down to 70% capacity
                    val fileSize = file.length()
                    if (file.delete()) {
                        currentSize -= fileSize
                    }
                }
                Log.d(TAG, "Cache successfully trimmed. New size: ${currentSize / (1024 * 1024)} MB")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming cache: ${e.message}")
        }
    }
}
