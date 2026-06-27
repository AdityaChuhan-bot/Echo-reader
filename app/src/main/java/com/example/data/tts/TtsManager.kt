package com.example.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine

class TtsManager(private val context: Context) {
    private val TAG = "TtsManager"

    // Supported Voices
    data class VoiceModel(
        val id: String,
        val name: String,
        val provider: String, // NATIVE, OPENAI, ELEVENLABS, GOOGLE
        val gender: String,
        val accent: String,
        val description: String
    )

    val availableVoices = listOf(
        // Native voices
        VoiceModel("native_us_male", "Default Male (US)", "NATIVE", "Male", "American", "Android offline system voice"),
        VoiceModel("native_uk_female", "Default Female (UK)", "NATIVE", "Female", "British", "Android offline system voice"),
        VoiceModel("native_in_female", "Default Female (IN)", "NATIVE", "Female", "Indian English", "Android offline system voice"),
        
        // OpenAI Voices
        VoiceModel("alloy", "Alloy (OpenAI)", "OPENAI", "Male", "American", "Natural, versatile, balanced voice"),
        VoiceModel("shimmer", "Shimmer (OpenAI)", "OPENAI", "Female", "American", "Clear, professional, documentary feel"),
        VoiceModel("nova", "Nova (OpenAI)", "OPENAI", "Female", "British", "Warm storyteller, conversational tone"),
        VoiceModel("onyx", "Onyx (OpenAI)", "OPENAI", "Male", "American", "Deep cinematic, rich baritone"),

        // ElevenLabs Voices
        VoiceModel("21m00Tcm4TlvDq8ikWAM", "Rachel (Eleven)", "ELEVENLABS", "Female", "American", "Calm, narrative, high-fidelity storyteller"),
        VoiceModel("AZnzlk1XvdvUeBnXmlld", "Domi (Eleven)", "ELEVENLABS", "Female", "British", "Crisp, expressive narrator voice"),
        VoiceModel("EXAVITQu4vr4xnSDxMaL", "Bella (Eleven)", "ELEVENLABS", "Female", "American", "Soft, warm, documentary narrator"),
        VoiceModel("ErXwobaYiN019PkySvjV", "Antoni (Eleven)", "ELEVENLABS", "Male", "American", "Warm, emotional storyteller"),

        // Google Cloud AI Voices
        VoiceModel("en-US-Neural2-F", "Neural F (Google)", "GOOGLE", "Female", "American", "Ultra-smooth Google Neural2 voice"),
        VoiceModel("en-US-Neural2-D", "Neural D (Google)", "GOOGLE", "Male", "American", "Clear and natural documentary tone"),
        VoiceModel("en-GB-Neural2-F", "Neural F (UK)", "GOOGLE", "Female", "British", "Sophisticated British storytelling accent"),
        VoiceModel("en-IN-Neural2-D", "Neural D (IN)", "GOOGLE", "Female", "Indian English", "Clear and natural Indian English speaker")
    )

    // State callbacks
    interface PlaybackListener {
        fun onStart()
        fun onComplete()
        fun onError(message: String)
        fun onProgress(sentenceIndex: Int)
    }

    private var currentListener: PlaybackListener? = null
    private var nativeTts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isNativeTtsReady = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        initNativeTts()
    }

    private fun initNativeTts() {
        nativeTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isNativeTtsReady = true
                Log.d(TAG, "Native TTS initialized successfully")
                // Setup utterance listener
                nativeTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "Native TTS started")
                        currentListener?.onStart()
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "Native TTS completed")
                        currentListener?.onComplete()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "Native TTS error")
                        currentListener?.onError("Native TTS playback error")
                    }
                })
            } else {
                Log.e(TAG, "Failed to initialize Native TTS")
            }
        }
    }

    /**
     * Synthesizes and plays a sentence.
     */
    fun speak(
        text: String,
        voiceId: String,
        provider: String,
        speed: Float,
        listener: PlaybackListener
    ) {
        stop()
        currentListener = listener

        if (text.isBlank()) {
            listener.onComplete()
            return
        }

        when (provider) {
            "NATIVE" -> {
                speakNative(text, voiceId, speed)
            }
            "OPENAI" -> {
                speakPremium(text, voiceId, "OPENAI", speed)
            }
            "ELEVENLABS" -> {
                speakPremium(text, voiceId, "ELEVENLABS", speed)
            }
            "GOOGLE" -> {
                speakPremium(text, voiceId, "GOOGLE", speed)
            }
            else -> {
                speakNative(text, "native_us_male", speed)
            }
        }
    }

    private fun speakNative(text: String, voiceId: String, speed: Float) {
        if (!isNativeTtsReady) {
            currentListener?.onError("Native TTS system is initializing. Please try again.")
            return
        }

        nativeTts?.let { tts ->
            // Set locale/accent
            when (voiceId) {
                "native_uk_female" -> tts.language = Locale.UK
                "native_in_female" -> tts.language = Locale("en", "IN")
                else -> tts.language = Locale.US
            }
            
            // Set speed
            tts.setSpeechRate(speed)
            
            // Speak
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "EchoReaderUtterance")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "EchoReaderUtterance")
        }
    }

    private fun speakPremium(text: String, voiceId: String, provider: String, speed: Float) {
        // Run synthesis and playback
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Get or download cached file
                val cachedFile = getSynthesizedFile(text, voiceId, provider)
                
                if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                    playAudioFile(cachedFile, speed)
                } else {
                    // Fallback to Native TTS if API fails or key is missing
                    Log.w(TAG, "Premium synthesis failed, falling back to Native TTS")
                    speakNative(text, "native_us_male", speed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Premium speak failed: ${e.message}")
                speakNative(text, "native_us_male", speed)
            }
        }
    }

    /**
     * Returns true if keys are configured. Otherwise fallbacks can be shown.
     */
    fun hasApiKey(provider: String): Boolean {
        return when (provider) {
            "OPENAI" -> com.example.BuildConfig.OPENAI_API_KEY.isNotEmpty() && com.example.BuildConfig.OPENAI_API_KEY != "MY_OPENAI_API_KEY"
            "ELEVENLABS" -> com.example.BuildConfig.ELEVENLABS_API_KEY.isNotEmpty() && com.example.BuildConfig.ELEVENLABS_API_KEY != "MY_ELEVENLABS_API_KEY"
            "GOOGLE" -> com.example.BuildConfig.GOOGLE_CLOUD_API_KEY.isNotEmpty() && com.example.BuildConfig.GOOGLE_CLOUD_API_KEY != "MY_GOOGLE_CLOUD_API_KEY"
            else -> true
        }
    }

    /**
     * Gets a cached file of the synthesis, or fetches it from the respective provider.
     */
    private suspend fun getSynthesizedFile(text: String, voiceId: String, provider: String): File? {
        val hash = md5(text)
        val cacheDir = File(context.cacheDir, "tts_cache").apply { mkdirs() }
        val filename = "${provider}_${voiceId}_${hash}.mp3"
        val cachedFile = File(cacheDir, filename)

        // 1. Return from cache if it exists
        if (cachedFile.exists() && cachedFile.length() > 0) {
            Log.d(TAG, "Cache HIT for sentence: $filename")
            return cachedFile
        }

        // 2. Fetch from network
        Log.d(TAG, "Cache MISS, synthesizing from network: $provider")
        
        val apiKey = when (provider) {
            "OPENAI" -> com.example.BuildConfig.OPENAI_API_KEY
            "ELEVENLABS" -> com.example.BuildConfig.ELEVENLABS_API_KEY
            "GOOGLE" -> com.example.BuildConfig.GOOGLE_CLOUD_API_KEY
            else -> ""
        }

        if (apiKey.isEmpty() || apiKey.startsWith("MY_")) {
            Log.w(TAG, "No valid API key for $provider, cannot synthesize premium")
            return null
        }

        return try {
            val success = when (provider) {
                "OPENAI" -> fetchOpenAiTts(text, voiceId, apiKey, cachedFile)
                "ELEVENLABS" -> fetchElevenLabsTts(text, voiceId, apiKey, cachedFile)
                "GOOGLE" -> fetchGoogleCloudTts(text, voiceId, apiKey, cachedFile)
                else -> false
            }
            if (success) cachedFile else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TTS from $provider: ${e.message}")
            if (cachedFile.exists()) cachedFile.delete()
            null
        }
    }

    private fun fetchOpenAiTts(text: String, voiceId: String, apiKey: String, targetFile: File): Boolean {
        val mediaType = "application/json".toMediaType()
        val json = JSONObject().apply {
            put("model", "tts-1")
            put("input", text)
            put("voice", voiceId)
            put("response_format", "mp3")
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/audio/speech")
            .post(json.toString().toRequestBody(mediaType))
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "OpenAI TTS returned error: ${response.code} ${response.message}")
                return false
            }

            response.body?.let { body ->
                saveStreamToFile(body.byteStream(), targetFile)
                return true
            }
        }
        return false
    }

    private fun fetchElevenLabsTts(text: String, voiceId: String, apiKey: String, targetFile: File): Boolean {
        val mediaType = "application/json".toMediaType()
        val json = JSONObject().apply {
            put("text", text)
            put("model_id", "eleven_monolingual_v1")
            val settings = JSONObject().apply {
                put("stability", 0.5)
                put("similarity_boost", 0.75)
            }
            put("voice_settings", settings)
        }

        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
            .post(json.toString().toRequestBody(mediaType))
            .addHeader("xi-api-key", apiKey)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "ElevenLabs TTS returned error: ${response.code}")
                return false
            }

            response.body?.let { body ->
                saveStreamToFile(body.byteStream(), targetFile)
                return true
            }
        }
        return false
    }

    private fun fetchGoogleCloudTts(text: String, voiceId: String, apiKey: String, targetFile: File): Boolean {
        val mediaType = "application/json".toMediaType()
        val json = JSONObject().apply {
            put("input", JSONObject().apply { put("text", text) })
            put("voice", JSONObject().apply {
                put("languageCode", "en-US")
                put("name", voiceId)
            })
            put("audioConfig", JSONObject().apply {
                put("audioEncoding", "MP3")
                put("speakingRate", 1.0)
            })
        }

        val request = Request.Builder()
            .url("https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey")
            .post(json.toString().toRequestBody(mediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Google Cloud TTS returned error: ${response.code}")
                return false
            }

            response.body?.let { body ->
                val responseJson = JSONObject(body.string())
                val audioContentBase64 = responseJson.optString("audioContent")
                if (audioContentBase64.isNotEmpty()) {
                    val bytes = android.util.Base64.decode(audioContentBase64, android.util.Base64.DEFAULT)
                    targetFile.writeBytes(bytes)
                    return true
                }
            }
        }
        return false
    }

    private fun saveStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }
    }

    private suspend fun playAudioFile(file: File, speed: Float) = withContext(kotlinx.coroutines.Dispatchers.Main) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                
                // Adjust speed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = PlaybackParams().apply {
                        this.speed = speed
                    }
                }
                
                setOnPreparedListener {
                    currentListener?.onStart()
                    start()
                }

                setOnCompletionListener {
                    currentListener?.onComplete()
                    release()
                    mediaPlayer = null
                }

                setOnErrorListener { _, what, extra ->
                    currentListener?.onError("MediaPlayer error: what=$what extra=$extra")
                    release()
                    mediaPlayer = null
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer setup failed: ${e.message}")
            currentListener?.onError("MediaPlayer playback failure")
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun resume() {
        if (mediaPlayer != null) {
            mediaPlayer?.start()
        }
    }

    fun stop() {
        // Stop premium player
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer: ${e.message}")
        }

        // Stop native TTS
        try {
            nativeTts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Native TTS: ${e.message}")
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true || nativeTts?.isSpeaking == true
    }

    fun shutdown() {
        stop()
        nativeTts?.shutdown()
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") {
            String.format("%02x", it)
        }
    }
}
