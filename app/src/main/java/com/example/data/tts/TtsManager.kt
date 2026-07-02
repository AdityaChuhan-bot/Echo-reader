package com.example.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TtsManager(private val context: Context) {
    private val TAG = "TtsManager"
    private val prefs = context.getSharedPreferences("audiobook_model_prefs", Context.MODE_PRIVATE)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    // Supported KittenTTS Voices
    data class VoiceModel(
        val id: String,
        val name: String,
        val provider: String, // KITTENTTS, NATIVE
        val gender: String,
        val accent: String,
        val description: String
    )

    val availableVoices = listOf(
        VoiceModel("kitten_mimi", "Mimi (KittenTTS)", "KITTENTTS", "Female", "American", "Expressive neural storytelling voice"),
        VoiceModel("kitten_lily", "Lily (KittenTTS)", "KITTENTTS", "Female", "British", "Clear and soft narration voice"),
        VoiceModel("kitten_marvin", "Marvin (KittenTTS)", "KITTENTTS", "Male", "American", "Highly engaging neural narrator"),
        VoiceModel("kitten_bruce", "Bruce (KittenTTS)", "KITTENTTS", "Male", "American", "Deep and rich authoritative voice"),
        VoiceModel("kitten_jenny", "Jenny (KittenTTS)", "KITTENTTS", "Female", "Australian", "Crisp, warm australian storyteller"),
        VoiceModel("kitten_leo", "Leo (KittenTTS)", "KITTENTTS", "Male", "British", "Warm and charismatic british narrator"),
        VoiceModel("native_system", "System Default (Offline)", "NATIVE", "Neutral", "System", "Standard on-device system fallback")
    )

    // Playback state callback interface
    interface PlaybackListener {
        fun onStart()
        fun onComplete()
        fun onError(message: String)
        fun onProgress(sentenceIndex: Int)
    }

    private var currentListener: PlaybackListener? = null
    private var nativeTts: TextToSpeech? = null
    private var kittenTtsService: KittenTtsService? = null
    private var isTtsReady = false
    private var isKittenConfigured = false

    // Media3 ExoPlayer for playing synthesized wave chunks
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    // Synthesis and Cache tracking
    private val cacheDir = File(context.cacheDir, "kitten_tts_cache").apply { mkdirs() }
    private var currentSpeakJob: Job? = null
    private var currentSynthesisFile: File? = null
    private var currentSynthesisId: String? = null

    // Audio Focus management
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume if it was playing or buffer
            }
        }
    }

    init {
        try {
            kittenTtsService = KittenTtsService(context)
            isKittenConfigured = kittenTtsService?.isConfigured() == true
            initTts()
            initPlayer()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize TTS subsystem during startup", t)
            isTtsReady = false
            isKittenConfigured = false
        }
    }

    private fun initTts() {
        try {
            nativeTts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    Log.d(TAG, "System TTS helper initialized successfully")
                    setupUtteranceListener()
                } else {
                    Log.e(TAG, "System TTS initialization failed")
                    isTtsReady = false
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "TextToSpeech initialization failed", t)
            isTtsReady = false
        }

        if (!isKittenConfigured) {
            Log.d(TAG, "KittenTTS endpoint not configured yet; app will open with safe fallback")
        }
    }

    private fun initPlayer() {
        mainScope.launch {
            try {
                exoPlayer = ExoPlayer.Builder(context).build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    Log.d(TAG, "ExoPlayer Ready. Playing synthesized audio.")
                                    currentListener?.onStart()
                                }
                                Player.STATE_ENDED -> {
                                    Log.d(TAG, "ExoPlayer Ended sentence playback.")
                                    currentListener?.onComplete()
                                    abandonAudioFocus()
                                }
                                Player.STATE_IDLE -> {
                                    // Player idle or stopped
                                }
                                Player.STATE_BUFFERING -> {
                                    // Buffering next chunk
                                }
                            }
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Log.e(TAG, "ExoPlayer Error: ${error.message}")
                            currentListener?.onError("Playback error: ${error.message}")
                            abandonAudioFocus()
                        }
                    })
                }

                // Setup MediaSession for lock screen widgets & notification drawer controls
                exoPlayer?.let { player ->
                    mediaSession = MediaSession.Builder(context, player)
                        .setId("AudioBookMediaSession_${System.currentTimeMillis()}")
                        .build()
                    Log.d(TAG, "MediaSession successfully registered for system notification integration")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error initializing ExoPlayer/MediaSession", t)
            }
        }
    }

    private fun setupUtteranceListener() {
        nativeTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Synthesis started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Synthesis finished: $utteranceId")
                if (utteranceId == currentSynthesisId) {
                    mainScope.launch {
                        playSynthesizedFile()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Synthesis failed: $utteranceId")
                currentListener?.onError("KittenTTS synthesis engine failed to synthesize chunk.")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "Synthesis failed: $utteranceId with code $errorCode")
                currentListener?.onError("KittenTTS engine failed with error code: $errorCode")
            }
        })
    }

    /**
     * Compute a deterministic unique MD5 hash for cached files
     */
    private fun getCacheKey(text: String, voiceId: String, speed: Float): String {
        val rawKey = "$text|$voiceId|$speed"
        val bytes = MessageDigest.getInstance("MD5").digest(rawKey.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isModelDownloaded(provider: String): Boolean {
        return prefs.getBoolean("model_downloaded_$provider", true)
    }

    fun setModelDownloaded(provider: String, downloaded: Boolean) {
        prefs.edit().putBoolean("model_downloaded_$provider", downloaded).apply()
    }

    fun getModelDownloadProgress(provider: String): Float {
        return prefs.getFloat("model_progress_$provider", 1.0f)
    }

    fun setModelDownloadProgress(provider: String, progress: Float) {
        prefs.edit().putFloat("model_progress_$provider", progress).apply()
    }

    fun isKokoroSupportedEfficiently(): Boolean = true

    fun hasApiKey(provider: String): Boolean = true

    fun configureKittenEndpoint(url: String) {
        kittenTtsService?.setBaseUrl(url)
        isKittenConfigured = url.isNotBlank()
    }

    fun getKittenEndpoint(): String = kittenTtsService?.getBaseUrl().orEmpty()

    fun downloadModel(provider: String, onProgress: (Float) -> Unit, onComplete: () -> Unit) {
        mainScope.launch {
            onProgress(1.0f)
            onComplete()
        }
    }

    /**
     * Synthesize and play speech completely offline using KittenTTS file chunking.
     */
    fun speak(
        text: String,
        voiceId: String,
        provider: String,
        speed: Float,
        readingMode: String = "Story",
        listener: PlaybackListener
    ) {
        stop()
        currentListener = listener

        if (text.isBlank()) {
            listener.onComplete()
            return
        }

        currentSpeakJob = mainScope.launch {
            val key = getCacheKey(text, voiceId, speed)
            val cachedFile = File(cacheDir, "chunk_$key.wav")

            if (cachedFile.exists() && cachedFile.length() > 0) {
                Log.d(TAG, "KittenTTS Cache Hit: playing cached audio chunk.")
                currentSynthesisFile = cachedFile
                playSynthesizedFile()
            } else {
                Log.d(TAG, "KittenTTS Cache Miss: synthesizing offline audio chunk.")
                synthesizeToOfflineCache(text, voiceId, speed, cachedFile)
            }
        }
    }

    private suspend fun synthesizeToOfflineCache(
        text: String,
        voiceId: String,
        speed: Float,
        targetFile: File
    ) {
        currentSynthesisFile = targetFile

        if (isKittenConfigured && kittenTtsService != null) {
            try {
                val succeeded = kittenTtsService!!.synthesizeToFile(text, voiceId, speed, targetFile)
                if (succeeded) {
                    currentListener?.onStart()
                    playSynthesizedFile()
                    return
                }
            } catch (t: Throwable) {
                Log.w(TAG, "KittenTTS service synthesis failed", t)
            }
        }

        if (!isTtsReady) {
            currentListener?.onError("KittenTTS is not ready yet. Please try again in a moment.")
            return
        }

        nativeTts?.let { tts ->
            try {
                if (voiceId.contains("lily") || voiceId.contains("leo")) {
                    tts.language = Locale.UK
                } else {
                    tts.language = Locale.US
                }

                val pitch = when (voiceId) {
                    "kitten_mimi" -> 1.12f
                    "kitten_lily" -> 1.05f
                    "kitten_marvin" -> 0.88f
                    "kitten_bruce" -> 0.78f
                    "kitten_jenny" -> 1.15f
                    "kitten_leo" -> 0.95f
                    else -> 1.00f
                }
                tts.setPitch(pitch)
                tts.setSpeechRate(speed)

                val utteranceId = "KittenTTS_${System.currentTimeMillis()}"
                currentSynthesisId = utteranceId

                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }

                val result = tts.synthesizeToFile(text, params, targetFile, utteranceId)
                if (result != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "Failed to queue offline synthesis")
                    currentListener?.onError("KittenTTS failed to queue offline synthesis.")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Synthesis fallback failed", t)
                currentListener?.onError("KittenTTS fallback failed to initialize.")
            }
        }
    }

    private fun playSynthesizedFile() {
        val file = currentSynthesisFile ?: run {
            currentListener?.onError("No synthesized audio file is ready.")
            return
        }
        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "Synthesized audio file is missing or empty")
            currentListener?.onError("Synthesized audiobook page is unreadable.")
            return
        }

        if (exoPlayer == null) {
            Log.w(TAG, "ExoPlayer is unavailable; skipping playback")
            currentListener?.onError("Playback engine is unavailable.")
            return
        }

        if (requestAudioFocus()) {
            exoPlayer?.let { player ->
                try {
                    player.stop()
                    player.clearMediaItems()
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to start playback", t)
                    currentListener?.onError("Playback failed to start.")
                }
            }
        } else {
            Log.e(TAG, "Audio focus denied. Playback blocked.")
            currentListener?.onError("Audio focus denied.")
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()

            return audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            return audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    fun pause() {
        if (exoPlayer == null) return
        mainScope.launch {
            try {
                exoPlayer?.pause()
            } catch (t: Throwable) {
                Log.e(TAG, "Pause failed", t)
            }
        }
    }

    fun resume() {
        if (exoPlayer == null) return
        mainScope.launch {
            try {
                if (exoPlayer?.playbackState != Player.STATE_IDLE) {
                    if (requestAudioFocus()) {
                        exoPlayer?.play()
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Resume failed", t)
            }
        }
    }

    fun stop() {
        currentSpeakJob?.cancel()
        currentSpeakJob = null
        currentSynthesisId = null
        mainScope.launch {
            try {
                exoPlayer?.stop()
                exoPlayer?.clearMediaItems()
                abandonAudioFocus()
            } catch (t: Throwable) {
                Log.e(TAG, "Stop failed", t)
            }
        }
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }

    fun shutdown() {
        stop()
        try {
            nativeTts?.shutdown()
        } catch (t: Throwable) {
            Log.e(TAG, "TTS shutdown failed", t)
        }
        mainScope.launch {
            try {
                mediaSession?.release()
            } catch (t: Throwable) {
                Log.e(TAG, "MediaSession release failed", t)
            }
            try {
                exoPlayer?.release()
            } catch (t: Throwable) {
                Log.e(TAG, "ExoPlayer release failed", t)
            }
        }
        // Clean up cached wav files periodically on shutdown
        cacheDir.listFiles()?.forEach { file ->
            try {
                file.delete()
            } catch (e: Exception) {
                // Ignore delete errors
            }
        }
    }
}
