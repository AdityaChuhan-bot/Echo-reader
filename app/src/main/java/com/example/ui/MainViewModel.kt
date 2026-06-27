package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.BookDao
import com.example.data.database.BookEntity
import com.example.data.database.BookmarkEntity
import com.example.data.database.ChapterEntity
import com.example.data.parser.BookParser
import com.example.data.repository.BookRepository
import com.example.data.tts.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"

    private val db = AppDatabase.getDatabase(application)
    private val bookDao: BookDao = db.bookDao()
    private val repository = BookRepository(bookDao)
    val ttsManager = TtsManager(application)

    // UI States
    val books: StateFlow<List<BookEntity>> = repository.allBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentBook = MutableStateFlow<BookEntity?>(null)
    val currentBook: StateFlow<BookEntity?> = _currentBook.asStateFlow()

    private val _currentChapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val currentChapters: StateFlow<List<ChapterEntity>> = _currentChapters.asStateFlow()

    private val _currentSentences = MutableStateFlow<List<String>>(emptyList())
    val currentSentences: StateFlow<List<String>> = _currentSentences.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _importProgress = MutableStateFlow<String?>(null)
    val importProgress: StateFlow<String?> = _importProgress.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    // Sleep Timer
    private val _sleepTimeRemaining = MutableStateFlow<Long?>(null) // In milliseconds
    val sleepTimeRemaining: StateFlow<Long?> = _sleepTimeRemaining.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Search results data class
    data class SearchResult(
        val chapterIndex: Int,
        val chapterTitle: String,
        val sentenceIndex: Int,
        val sentenceText: String
    )

    private var bookmarkCollectorJob: Job? = null

    // Player listener
    private val ttsListener = object : TtsManager.PlaybackListener {
        override fun onStart() {
            _isBuffering.value = false
            _isPlaying.value = true
        }

        override fun onComplete() {
            _isPlaying.value = false
            playNextSentence()
        }

        override fun onError(message: String) {
            _isBuffering.value = false
            _isPlaying.value = false
            Log.e(TAG, "TTS Error: $message")
        }

        override fun onProgress(sentenceIndex: Int) {
            // Not used for direct native progress
        }
    }

    init {
        // Automatically preheat voices and restore state if needed
    }

    fun selectBook(book: BookEntity) {
        viewModelScope.launch {
            _currentBook.value = book
            
            // Fetch chapters
            val chaps = repository.getChapters(book.id)
            _currentChapters.value = chaps

            // Update sentences for current chapter
            if (chaps.isNotEmpty() && book.currentChapterIndex < chaps.size) {
                val currentText = chaps[book.currentChapterIndex].textContent
                _currentSentences.value = BookParser.splitIntoSentences(currentText)
            } else {
                _currentSentences.value = emptyList()
            }

            // Collect bookmarks for this book
            bookmarkCollectorJob?.cancel()
            bookmarkCollectorJob = viewModelScope.launch {
                repository.getBookmarks(book.id).collect {
                    _bookmarks.value = it
                }
            }

            // Record last opened
            val updated = book.copy(lastOpened = System.currentTimeMillis())
            repository.updateBook(updated)
            _currentBook.value = updated
        }
    }

    fun play() {
        val book = _currentBook.value ?: return
        val sentences = _currentSentences.value
        if (sentences.isEmpty()) return

        val sentenceIndex = book.currentSentenceIndex
        if (sentenceIndex >= sentences.size) {
            // Check if there is a next chapter
            if (book.currentChapterIndex + 1 < book.totalChapters) {
                skipToChapter(book.currentChapterIndex + 1)
                return
            } else {
                // End of book
                stop()
                return
            }
        }

        val textToSpeak = sentences[sentenceIndex]
        _isBuffering.value = true
        _isPlaying.value = true

        ttsManager.speak(
            text = textToSpeak,
            voiceId = book.voiceId,
            provider = book.voiceProvider,
            speed = book.speed,
            listener = ttsListener
        )
    }

    fun pause() {
        ttsManager.pause()
        _isPlaying.value = false
    }

    fun resume() {
        if (ttsManager.isPlaying()) {
            ttsManager.resume()
            _isPlaying.value = true
        } else {
            play()
        }
    }

    fun stop() {
        ttsManager.stop()
        _isPlaying.value = false
        _isBuffering.value = false
    }

    private fun playNextSentence() {
        viewModelScope.launch {
            val book = _currentBook.value ?: return@launch
            val nextSentenceIdx = book.currentSentenceIndex + 1
            if (nextSentenceIdx < _currentSentences.value.size) {
                // Increment sentence
                val updated = book.copy(
                    currentSentenceIndex = nextSentenceIdx,
                    playbackPositionMs = System.currentTimeMillis()
                )
                repository.updateBook(updated)
                _currentBook.value = updated
                play()
            } else {
                // Next chapter
                val nextChapterIdx = book.currentChapterIndex + 1
                if (nextChapterIdx < book.totalChapters) {
                    val updated = book.copy(
                        currentChapterIndex = nextChapterIdx,
                        currentSentenceIndex = 0,
                        playbackPositionMs = System.currentTimeMillis()
                    )
                    repository.updateBook(updated)
                    _currentBook.value = updated
                    
                    // Reload sentences
                    val chaps = _currentChapters.value
                    if (nextChapterIdx < chaps.size) {
                        _currentSentences.value = BookParser.splitIntoSentences(chaps[nextChapterIdx].textContent)
                    }
                    
                    // If sleep timer is set to "End of Chapter", handle it!
                    if (sleepTimerJob != null && _sleepTimeRemaining.value == -1L) {
                        stopSleepTimer()
                        stop()
                    } else {
                        play()
                    }
                } else {
                    // End of Audiobook
                    val updated = book.copy(
                        currentChapterIndex = 0,
                        currentSentenceIndex = 0
                    )
                    repository.updateBook(updated)
                    _currentBook.value = updated
                    stop()
                }
            }
        }
    }

    fun skipForwardSentence() {
        viewModelScope.launch {
            val book = _currentBook.value ?: return@launch
            val nextIndex = book.currentSentenceIndex + 1
            if (nextIndex < _currentSentences.value.size) {
                val updated = book.copy(currentSentenceIndex = nextIndex)
                repository.updateBook(updated)
                _currentBook.value = updated
                if (_isPlaying.value) {
                    play()
                }
            } else {
                // Next Chapter
                skipToChapter(book.currentChapterIndex + 1)
            }
        }
    }

    fun skipBackwardSentence() {
        viewModelScope.launch {
            val book = _currentBook.value ?: return@launch
            val prevIndex = book.currentSentenceIndex - 1
            if (prevIndex >= 0) {
                val updated = book.copy(currentSentenceIndex = prevIndex)
                repository.updateBook(updated)
                _currentBook.value = updated
                if (_isPlaying.value) {
                    play()
                }
            } else {
                // Prev Chapter (at end)
                if (book.currentChapterIndex > 0) {
                    val prevChapterIdx = book.currentChapterIndex - 1
                    val chaps = _currentChapters.value
                    val prevSentences = BookParser.splitIntoSentences(chaps[prevChapterIdx].textContent)
                    val updated = book.copy(
                        currentChapterIndex = prevChapterIdx,
                        currentSentenceIndex = maxOf(0, prevSentences.size - 1)
                    )
                    repository.updateBook(updated)
                    _currentBook.value = updated
                    _currentSentences.value = prevSentences
                    if (_isPlaying.value) {
                        play()
                    }
                }
            }
        }
    }

    fun seekToSentence(index: Int) {
        viewModelScope.launch {
            val book = _currentBook.value ?: return@launch
            if (index in _currentSentences.value.indices) {
                val updated = book.copy(currentSentenceIndex = index)
                repository.updateBook(updated)
                _currentBook.value = updated
                if (_isPlaying.value) {
                    play()
                }
            }
        }
    }

    fun skipToChapter(chapterIndex: Int) {
        viewModelScope.launch {
            val book = _currentBook.value ?: return@launch
            val chaps = _currentChapters.value
            if (chapterIndex in chaps.indices) {
                val updated = book.copy(
                    currentChapterIndex = chapterIndex,
                    currentSentenceIndex = 0
                )
                repository.updateBook(updated)
                _currentBook.value = updated
                _currentSentences.value = BookParser.splitIntoSentences(chaps[chapterIndex].textContent)
                if (_isPlaying.value) {
                    play()
                }
            }
        }
    }

    fun changeSpeed(speed: Float) {
        viewModelScope.launch {
            val book = _currentBook.value ?: return@launch
            val updated = book.copy(speed = speed)
            repository.updateBook(updated)
            _currentBook.value = updated
            if (_isPlaying.value) {
                play()
            }
        }
    }

    fun changeVoice(voiceId: String, provider: String) {
        viewModelScope.launch {
            val book = _currentBook.value ?: return@launch
            val updated = book.copy(voiceId = voiceId, voiceProvider = provider)
            repository.updateBook(updated)
            _currentBook.value = updated
            if (_isPlaying.value) {
                play()
            }
        }
    }

    // Bookmarks
    fun addBookmark(note: String = "") {
        viewModelScope.launch {
            val book = _currentBook.value ?: return@launch
            val sentences = _currentSentences.value
            if (sentences.isEmpty() || book.currentSentenceIndex >= sentences.size) return@launch

            val quote = sentences[book.currentSentenceIndex]
            val bookmark = BookmarkEntity(
                bookId = book.id,
                chapterIndex = book.currentChapterIndex,
                sentenceIndex = book.currentSentenceIndex,
                quoteText = quote,
                note = note
            )
            repository.addBookmark(bookmark)
        }
    }

    fun deleteBookmark(bookmarkId: Int) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
        }
    }

    // Search
    fun search(query: String) {
        val book = _currentBook.value ?: return
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val chapters = repository.searchBook(book.id, query)
            val results = mutableListOf<SearchResult>()
            
            for (chapter in chapters) {
                val sentences = BookParser.splitIntoSentences(chapter.textContent)
                sentences.forEachIndexed { sIdx, sentence ->
                    if (sentence.contains(query, ignoreCase = true)) {
                        results.add(
                            SearchResult(
                                chapterIndex = chapter.chapterIndex,
                                chapterTitle = chapter.title,
                                sentenceIndex = sIdx,
                                sentenceText = sentence
                            )
                        )
                    }
                }
            }
            _searchResults.value = results
        }
    }

    // Import Document
    fun importBook(context: Context, uri: Uri) {
        _importProgress.value = "Copying file..."
        viewModelScope.launch {
            try {
                val fileName = getFileName(context, uri) ?: "imported_book.pdf"
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    _importProgress.value = "Extracting chapters & OCR pages..."
                    val bookId = repository.importBook(context, fileName, inputStream)
                    _importProgress.value = null
                    
                    // Auto select the newly imported book
                    val newBook = repository.getBookByIdSync(bookId)
                    if (newBook != null) {
                        selectBook(newBook)
                    }
                } else {
                    _importProgress.value = null
                }
            } catch (e: Exception) {
                _importProgress.value = null
                Log.e(TAG, "Failed importing book: ${e.message}", e)
            }
        }
    }

    /**
     * Imports a PDF via server-side Gemini text extraction API.
     */
    fun importBookViaGemini(context: Context, uri: Uri) {
        _importProgress.value = "Copying PDF file..."
        viewModelScope.launch {
            try {
                val fileName = getFileName(context, uri) ?: "imported_book.pdf"
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    _importProgress.value = "Uploading to Gemini AI..."
                    val bookId = repository.importBookViaGemini(context, fileName, inputStream)
                    _importProgress.value = null
                    
                    // Auto select the newly imported book
                    val newBook = repository.getBookByIdSync(bookId)
                    if (newBook != null) {
                        selectBook(newBook)
                    }
                } else {
                    _importProgress.value = null
                }
            } catch (e: Exception) {
                _importProgress.value = null
                Log.e(TAG, "Failed importing book via Gemini: ${e.message}", e)
            }
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            if (_currentBook.value?.id == book.id) {
                stop()
                _currentBook.value = null
                _currentChapters.value = emptyList()
                _currentSentences.value = emptyList()
            }
            repository.deleteBook(book)
        }
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int) {
        stopSleepTimer()
        if (minutes == -1) {
            // End of Chapter mode
            _sleepTimeRemaining.value = -1L
            sleepTimerJob = viewModelScope.launch {
                // Wait indefinitely until chapter end callback triggers stopping
            }
            return
        }

        val durationMs = minutes * 60 * 1000L
        _sleepTimeRemaining.value = durationMs
        sleepTimerJob = viewModelScope.launch {
            var remaining = durationMs
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepTimeRemaining.value = remaining
            }
            // Timer finished! Pause playback
            _sleepTimeRemaining.value = null
            pause()
        }
    }

    fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimeRemaining.value = null
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        stopSleepTimer()
    }
}
