package com.example.data.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object BookParser {
    private const val TAG = "BookParser"

    data class ParsedBook(
        val title: String,
        val author: String,
        val chapters: List<ParsedChapter>,
        val totalSentences: Int
    )

    data class ParsedChapter(
        val title: String,
        val textContent: String
    )

    /**
     * Parses a document file (PDF, EPUB, TXT) and returns structured Chapters.
     */
    suspend fun parseFile(context: Context, file: File, title: String): ParsedBook = withContext(Dispatchers.IO) {
        val extension = file.extension.lowercase()
        Log.d(TAG, "Parsing file: ${file.name} with extension $extension")

        val chapters = when (extension) {
            "txt" -> parseTxt(file)
            "epub" -> parseEpub(file)
            "pdf" -> parsePdf(context, file)
            "docx" -> parseDocx(file)
            else -> parseTxt(file) // Fallback to txt
        }

        // Clean up chapters: remove duplicate empty lines, trim page numbers
        val cleanedChapters = chapters.map { chapter ->
            val cleanedText = cleanText(chapter.textContent)
            ParsedChapter(chapter.title, cleanedText)
        }.filter { it.textContent.isNotBlank() }

        val finalChapters = if (cleanedChapters.isEmpty()) {
            listOf(ParsedChapter("Chapter 1", "No readable text found in the document."))
        } else {
            cleanedChapters
        }

        // Count sentences
        var sentenceCount = 0
        for (chap in finalChapters) {
            sentenceCount += splitIntoSentences(chap.textContent).size
        }

        ParsedBook(
            title = title,
            author = "Unknown Author",
            chapters = finalChapters,
            totalSentences = sentenceCount
        )
    }

    /**
     * Parse plain text file
     */
    private fun parseTxt(file: File): List<ParsedChapter> {
        val reader = BufferedReader(InputStreamReader(FileInputStream(file), "UTF-8"))
        val fullText = reader.use { it.readText() }

        return autoDetectChapters(fullText)
    }

    /**
     * Parses an EPUB file (basically a zip of HTML/XHTML chapters).
     */
    private fun parseEpub(file: File): List<ParsedChapter> {
        val chapters = mutableListOf<ParsedChapter>()
        try {
            val zipStream = ZipInputStream(FileInputStream(file))
            var entry = zipStream.nextEntry
            var htmlIndex = 1

            while (entry != null) {
                val name = entry.name.lowercase()
                if (name.endsWith(".html") || name.endsWith(".xhtml")) {
                    val bytes = zipStream.readBytes()
                    val rawHtml = String(bytes, Charsets.UTF_8)
                    val plainText = stripHtmlTags(rawHtml).trim()

                    if (plainText.isNotBlank() && plainText.length > 100) {
                        // Extract a header or title if possible
                        val chapterTitle = extractHtmlTitle(rawHtml) ?: "Section $htmlIndex"
                        chapters.add(ParsedChapter(chapterTitle, plainText))
                        htmlIndex++
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
            zipStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing EPUB: ${e.message}", e)
        }

        return if (chapters.isEmpty()) {
            // Try as text if zipped EPUB reading failed
            parseTxt(file)
        } else {
            chapters
        }
    }

    /**
     * Parses a PDF file page-by-page. If text rendering fails, uses on-device ML Kit OCR.
     */
    private suspend fun parsePdf(context: Context, file: File): List<ParsedChapter> = withContext(Dispatchers.IO) {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount
        val fullTextBuilder = StringBuilder()

        Log.d(TAG, "Parsing PDF with $pageCount pages...")

        // OCR/Text recognizer client
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        for (i in 0 until pageCount) {
            try {
                // Render page as bitmap
                val page = renderer.openPage(i)
                val width = 1080
                val height = (width * page.height / page.width)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Run ML Kit text recognition
                val pageText = recognizeText(recognizer, bitmap)
                if (pageText.isNotBlank()) {
                    fullTextBuilder.append(pageText).append("\n\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error OCR-ing page $i: ${e.message}")
            }
        }

        recognizer.close()
        renderer.close()
        pfd.close()

        val fullText = fullTextBuilder.toString()
        autoDetectChapters(fullText)
    }

    /**
     * Suspend function to recognize text using ML Kit Client
     */
    private suspend fun recognizeText(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        bitmap: Bitmap
    ): String = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (continuation.isActive) {
                    continuation.resume(visionText.text)
                }
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
    }

    /**
     * Automatically splits a giant string into chapters by searching for chapter patterns.
     */
    fun autoDetectChapters(fullText: String): List<ParsedChapter> {
        if (fullText.isBlank()) return emptyList()

        val chapters = mutableListOf<ParsedChapter>()
        // Chapter detection patterns: "Chapter 1", "CHAPTER I", "Part Two", "Section 3", "Introduction"
        val chapterRegex = Regex(
            "(?m)^\\s*(Chapter|CHAPTER|Part|PART|Section|SECTION)\\s+([0-9a-zA-ZIVXLCDM\\-]+)",
            RegexOption.IGNORE_CASE
        )

        val matches = chapterRegex.findAll(fullText).toList()

        if (matches.isEmpty()) {
            // Split into equal size pages/sections of approx 3000 characters
            val chunkSize = 4000
            var index = 0
            var chunkNum = 1
            while (index < fullText.length) {
                val end = minOf(index + chunkSize, fullText.length)
                val chunkText = fullText.substring(index, end)
                chapters.add(ParsedChapter("Section $chunkNum", chunkText))
                index = end
                chunkNum++
            }
        } else {
            var lastIndex = 0
            var currentTitle = "Introduction"

            for (i in matches.indices) {
                val match = matches[i]
                val matchIndex = match.range.first

                // Save previous chapter content
                if (matchIndex > lastIndex) {
                    val content = fullText.substring(lastIndex, matchIndex).trim()
                    if (content.isNotBlank()) {
                        chapters.add(ParsedChapter(currentTitle, content))
                    }
                }

                currentTitle = match.value.trim()
                lastIndex = matchIndex
            }

            // Add the last chapter
            if (lastIndex < fullText.length) {
                val content = fullText.substring(lastIndex).trim()
                if (content.isNotBlank()) {
                    chapters.add(ParsedChapter(currentTitle, content))
                }
            }
        }

        return chapters
    }

    /**
     * Cleans text to remove repeated elements like page numbers, headers, and footers.
     */
    private fun cleanText(text: String): String {
        val lines = text.lines()
        val cleanedLines = lines.filter { line ->
            val trimmed = line.trim()
            // Skip lines that are just numbers (page numbers)
            if (trimmed.matches(Regex("\\d+"))) return@filter false
            // Skip lines that are just single characters
            if (trimmed.length <= 1 && trimmed.matches(Regex("[^a-zA-Z0-9]"))) return@filter false
            true
        }
        return cleanedLines.joinToString("\n")
    }

    /**
     * Strips HTML tags from EPUB file content.
     */
    private fun stripHtmlTags(html: String): String {
        // Simple regex-based HTML tag stripper
        val noTags = html.replace(Regex("<[^>]*>"), " ")
        // Decode common html entities
        return noTags
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Extracts title from <title> tag or <h1> tag in HTML
     */
    private fun extractHtmlTitle(html: String): String? {
        val titleRegex = Regex("<title>([^<]*)</title>", RegexOption.IGNORE_CASE)
        val h1Regex = Regex("<h1>([^<]*)</h1>", RegexOption.IGNORE_CASE)

        val titleMatch = titleRegex.find(html)
        if (titleMatch != null) return titleMatch.groupValues[1].trim()

        val h1Match = h1Regex.find(html)
        if (h1Match != null) return h1Match.groupValues[1].trim()

        return null
    }

    /**
     * Splits a given chapter content into individual sentences for fine-grained reading progress.
     */
    fun splitIntoSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        // Match sentence boundary
        val sentenceRegex = Regex("(?<=[.!?])\\s+(?=[A-Z0-9])")
        return text.split(sentenceRegex).map { it.trim() }.filter { it.isNotEmpty() }
    }
}
