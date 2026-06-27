package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.BookEntity
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onBookSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val books by viewModel.books.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()

    // File picker contract
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importBook(context, it)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EchoReader AI",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Your AI Bookshelf",
                        fontSize = 16.sp,
                        color = GlassTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.1f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Audiobook icon",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Gemini Server-Side PDF Extractor Input Card
            GeminiPdfInputCard(
                onSelectPdf = { uri ->
                    viewModel.importBookViaGemini(context, uri)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Bookshelf List
            if (books.isEmpty()) {
                // Empty state card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        backgroundColor = Color.White.copy(alpha = 0.03f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Upload prompt icon",
                                tint = NeonCyan,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Your Shelf is Empty",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Upload any PDF, EPUB, or TXT file. Our intelligent AI will split it into readable chapters and narrate it using human-like natural voices.",
                                fontSize = 14.sp,
                                color = GlassTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(NeonCyan, NeonPurple)
                                        )
                                    )
                                    .clickable {
                                        filePickerLauncher.launch(
                                            arrayOf(
                                                "application/pdf",
                                                "application/epub+zip",
                                                "text/plain"
                                            )
                                        )
                                    }
                                    .padding(vertical = 12.dp, horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Import a Book",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(books) { book ->
                        BookCard(
                            book = book,
                            onClick = {
                                viewModel.selectBook(book)
                                onBookSelected()
                            },
                            onDelete = {
                                viewModel.deleteBook(book)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp)) // Space for FAB
                    }
                }
            }
        }

        // FAB to import new book
        FloatingActionButton(
            onClick = {
                filePickerLauncher.launch(
                    arrayOf(
                        "application/pdf",
                        "application/epub+zip",
                        "text/plain"
                    )
                )
            },
            containerColor = NeonCyan,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("import_book_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Import book",
                modifier = Modifier.size(28.dp)
            )
        }

        // Loading Overlay
        AnimatedVisibility(
            visible = importProgress != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    backgroundColor = Color(0xFF0F172A).copy(alpha = 0.95f),
                    borderColor = NeonCyan
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "EchoReader AI Processing",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = importProgress ?: "Processing...",
                            fontSize = 14.sp,
                            color = GlassTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(
    book: BookEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (book.totalSentences > 0) {
        val readCount = (book.currentChapterIndex * 50) + book.currentSentenceIndex // Approximate
        val percentage = readCount.toFloat() / book.totalSentences.toFloat()
        percentage.coerceIn(0f, 1f)
    } else 0f

    val progressPercent = (progress * 100).toInt()
    val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(book.lastOpened))

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("book_card_${book.id}"),
        backgroundColor = GlassSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover block
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(book.coverColor)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.title.take(3).uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${book.totalChapters} Chapters • ${book.estimatedTimeMinutes} min remaining",
                    fontSize = 13.sp,
                    color = GlassTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = NeonCyan,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress: $progressPercent%",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Last: $formattedDate",
                        fontSize = 11.sp,
                        color = GlassTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Delete Action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_book_button_${book.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete book",
                    tint = Color.Red.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun GeminiPdfInputCard(
    onSelectPdf: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // File picker launcher specific to PDF and Gemini cloud processing
    val geminiPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onSelectPdf(it) }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gemini_pdf_input_card"),
        backgroundColor = Color(0xFF1E1B4B).copy(alpha = 0.4f), // Deep indigo glass feel
        borderColor = NeonPurple
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sparkling AI Icon with gradient feel
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonPurple.copy(alpha = 0.15f))
                        .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Document Parser",
                        tint = NeonPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "AI Server-Side PDF Extractor",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Extract structured chapters via Gemini 3.5 Flash",
                        fontSize = 11.sp,
                        color = GlassTextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = "Perfect for complex, multi-page PDFs. The document is processed securely using cloud models to deliver clean, formatting-preserved text.",
                fontSize = 12.sp,
                color = GlassTextSecondary.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(NeonPurple, NeonCyan)
                        )
                    )
                    .clickable {
                        geminiPickerLauncher.launch(arrayOf("application/pdf"))
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload PDF for AI Extraction",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
