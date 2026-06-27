package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.BookDao
import com.example.data.database.BookEntity
import com.example.data.database.BookmarkEntity
import com.example.data.database.ChapterEntity
import com.example.data.parser.BookParser
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class BookRepository(private val bookDao: BookDao) {
    private val TAG = "BookRepository"

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBookById(id: Int): Flow<BookEntity?> {
        return bookDao.getBookById(id)
    }

    suspend fun getBookByIdSync(id: Int): BookEntity? {
        return bookDao.getBookByIdSync(id)
    }

    suspend fun updateBook(book: BookEntity) {
        bookDao.updateBook(book)
    }

    suspend fun deleteBook(book: BookEntity) {
        // Delete the associated local file if it's in the app's files directory
        try {
            val file = File(book.filePath)
            if (file.exists() && file.parentFile?.name == "books") {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${e.message}")
        }
        bookDao.deleteBook(book)
    }

    suspend fun getChapters(bookId: Int): List<ChapterEntity> {
        return bookDao.getChaptersForBook(bookId)
    }

    suspend fun getChapter(bookId: Int, chapterIndex: Int): ChapterEntity? {
        return bookDao.getChapter(bookId, chapterIndex)
    }

    fun getBookmarks(bookId: Int): Flow<List<BookmarkEntity>> {
        return bookDao.getBookmarksForBook(bookId)
    }

    suspend fun addBookmark(bookmark: BookmarkEntity): Long {
        return bookDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmarkId: Int) {
        bookDao.deleteBookmark(bookmarkId)
    }

    suspend fun searchBook(bookId: Int, query: String): List<ChapterEntity> {
        return bookDao.searchChapters(bookId, query)
    }

    /**
     * Imports a file from an input stream (e.g. from FilePicker Uri) into the local database.
     * Copys the file to internal storage for lifetime management, parses the text page-by-page,
     * splits into chapters, and saves to Room.
     */
    suspend fun importBook(
        context: Context,
        fileName: String,
        inputStream: InputStream
    ): Int {
        // 1. Copy file to internal books folder
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val localFile = File(booksDir, "${System.currentTimeMillis()}_$fileName")
        
        FileOutputStream(localFile).use { output ->
            inputStream.copyTo(output)
        }

        // 2. Select a premium glass card color scheme (Teal, Blue, Lavender, Emerald, Coral, Charcoal)
        val coverColors = listOf(
            0xFF1E3A8A.toInt(), // Deep Royal Blue
            0xFF0F766E.toInt(), // Emerald Teal
            0xFF6B21A8.toInt(), // Dark Amethyst
            0xFF991B1B.toInt(), // Velvet Red
            0xFF854D0E.toInt(), // Amber Gold
            0xFF065F46.toInt(), // Forest Green
            0xFF374151.toInt()  // Matte Carbon
        )
        val randomColor = coverColors.random()

        // 3. Parse the file
        val parsedBook = BookParser.parseFile(context, localFile, fileName.substringBeforeLast("."))

        // 4. Save Book metadata to get ID
        val estimatedTimeMin = (parsedBook.totalSentences * 3) / 60 // Roughly 3 seconds per sentence
        val bookEntity = BookEntity(
            title = parsedBook.title,
            author = parsedBook.author,
            filePath = localFile.absolutePath,
            coverColor = randomColor,
            totalChapters = parsedBook.chapters.size,
            totalSentences = parsedBook.totalSentences,
            estimatedTimeMinutes = estimatedTimeMin,
            speed = 1.0f,
            voiceId = "alloy",
            voiceProvider = "NATIVE"
        )
        val bookId = bookDao.insertBook(bookEntity).toInt()

        // 5. Save Chapters
        val chapterEntities = parsedBook.chapters.mapIndexed { idx, parsedChap ->
            ChapterEntity(
                bookId = bookId,
                chapterIndex = idx,
                title = parsedChap.title,
                textContent = parsedChap.textContent
            )
        }
        bookDao.insertChapters(chapterEntities)

        return bookId
    }

    /**
     * Imports a PDF file and extracts text using server-side Gemini AI.
     */
    suspend fun importBookViaGemini(
        context: Context,
        fileName: String,
        inputStream: InputStream
    ): Int {
        // 1. Copy file to internal books folder
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val localFile = File(booksDir, "${System.currentTimeMillis()}_$fileName")
        
        java.io.FileOutputStream(localFile).use { output ->
            inputStream.copyTo(output)
        }

        // 2. Select a premium gold/lavender cover color for Gemini-processed files
        val geminiCoverColors = listOf(
            0xFF8B5CF6.toInt(), // Neon Purple / Violet
            0xFF06B6D4.toInt(), // Neon Cyan
            0xFFD97706.toInt()  // Premium Amber/Gold
        )
        val coverColor = geminiCoverColors.random()

        // 3. Extract text using Gemini API
        val extractedText = try {
            com.example.data.parser.GeminiPdfExtractor.extractTextFromPdf(localFile)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini text extraction failed: ${e.message}", e)
            if (localFile.exists()) {
                localFile.delete()
            }
            throw e
        }

        // 4. Split into chapters using BookParser's chapter detector
        val chapters = BookParser.autoDetectChapters(extractedText)
        val cleanedChapters = chapters.map { chapter ->
            val cleanedText = chapter.textContent.trim()
            com.example.data.parser.BookParser.ParsedChapter(chapter.title, cleanedText)
        }.filter { it.textContent.isNotBlank() }

        val finalChapters = if (cleanedChapters.isEmpty()) {
            listOf(com.example.data.parser.BookParser.ParsedChapter("Chapter 1", extractedText))
        } else {
            cleanedChapters
        }

        // Count sentences
        var sentenceCount = 0
        for (chap in finalChapters) {
            sentenceCount += BookParser.splitIntoSentences(chap.textContent).size
        }

        // 5. Save Book metadata to Room
        val estimatedTimeMin = (sentenceCount * 3) / 60
        val bookEntity = BookEntity(
            title = fileName.substringBeforeLast("."),
            author = "Gemini AI Cloud Extracted",
            filePath = localFile.absolutePath,
            coverColor = coverColor,
            totalChapters = finalChapters.size,
            totalSentences = sentenceCount,
            estimatedTimeMinutes = estimatedTimeMin,
            speed = 1.0f,
            voiceId = "alloy",
            voiceProvider = "NATIVE"
        )
        val bookId = bookDao.insertBook(bookEntity).toInt()

        // 6. Save Chapters to Room
        val chapterEntities = finalChapters.mapIndexed { idx, parsedChap ->
            ChapterEntity(
                bookId = bookId,
                chapterIndex = idx,
                title = parsedChap.title,
                textContent = parsedChap.textContent
            )
        }
        bookDao.insertChapters(chapterEntities)

        return bookId
    }
}
