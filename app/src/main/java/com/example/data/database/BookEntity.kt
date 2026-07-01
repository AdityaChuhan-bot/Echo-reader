package com.example.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val filePath: String,
    val coverColor: Int, // Hex ARGB color representation
    val totalChapters: Int,
    val totalSentences: Int,
    val currentChapterIndex: Int = 0,
    val currentSentenceIndex: Int = 0,
    val playbackPositionMs: Long = 0,
    val lastOpened: Long = System.currentTimeMillis(),
    val estimatedTimeMinutes: Int = 0,
    val speed: Float = 1.0f,
    val voiceId: String = "kitten_mimi",
    val voiceProvider: String = "KITTENTTS", // KITTENTTS, NATIVE
    val isPremium: Boolean = false
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val chapterIndex: Int,
    val title: String,
    val textContent: String
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val chapterIndex: Int,
    val sentenceIndex: Int,
    val quoteText: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)
