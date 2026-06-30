package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToVoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val book by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val sentences by viewModel.currentSentences.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val sleepTimeRemaining by viewModel.sleepTimeRemaining.collectAsState()

    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkNote by remember { mutableStateOf("") }
    var showChaptersDrawer by remember { mutableStateOf(false) }

    var showAdvancedPanel by remember { mutableStateOf(false) }
    var showSmartResumeDialog by remember { mutableStateOf(false) }
    var previousBookId by remember { mutableStateOf<Int?>(null) }
    var activeTab by remember { mutableStateOf("Skip") } // Skip, Dict, Queue, Stats, Bookmarks

    var newWord by remember { mutableStateOf("") }
    var newReplacement by remember { mutableStateOf("") }

    val bookmarks by viewModel.bookmarks.collectAsState()
    val readingQueue by viewModel.readingQueue.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    val pronunciations by viewModel.pronunciations.collectAsState()
    val listeningMinutes by viewModel.listeningMinutes.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()
    val completedBooksCount by viewModel.completedBooksCount.collectAsState()

    LaunchedEffect(book?.id) {
        if (book != null && book!!.id != previousBookId) {
            if (book!!.currentChapterIndex > 0 || book!!.currentSentenceIndex > 0) {
                showSmartResumeDialog = true
            }
            previousBookId = book!!.id
        }
    }

    val activeChapterTitle = if (chapters.isNotEmpty() && book != null && book!!.currentChapterIndex < chapters.size) {
        chapters[book!!.currentChapterIndex].title
    } else "Chapter 1"

    val activeSentence = if (sentences.isNotEmpty() && book != null && book!!.currentSentenceIndex < sentences.size) {
        sentences[book!!.currentSentenceIndex]
    } else "No text loaded."

    // Animated visual rotating disk
    val infiniteTransition = rememberInfiniteTransition(label = "player")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 6000 else 0, easing = LinearEasing)
        ),
        label = "rotation"
    )

    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active book selected", color = GlassTextSecondary)
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Upper navigation bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("player_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GlassTextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = book!!.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Now Storytelling",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onNavigateToSearch, modifier = Modifier.testTag("player_search_button")) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GlassTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Central rotating disk cover illustration
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(book!!.coverColor), Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .rotate(rotationAngle),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl Grooves & Cover
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                )
                // Center pin
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Text Highlights Container
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                backgroundColor = GlassSurface
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(color = NeonCyan)
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    text = activeSentence,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GlassTextPrimary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 26.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Chapter and Progress indicator info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeChapterTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTextSecondary,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showChaptersDrawer = true }
                        .testTag("chapter_selector_trigger")
                )
                Text(
                    text = "${book!!.currentSentenceIndex + 1}/${sentences.size} sentences",
                    fontSize = 12.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sentence-level Slider Seekbar
            if (sentences.isNotEmpty()) {
                Slider(
                    value = book!!.currentSentenceIndex.toFloat(),
                    onValueChange = { viewModel.seekToSentence(it.toInt()) },
                    valueRange = 0f..(sentences.size - 1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_seekbar")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Audio Player Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev Sentence
                IconButton(
                    onClick = { viewModel.skipBackwardSentence() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Previous sentence",
                        tint = GlassTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause Glass Action Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonCyan, NeonPurple)
                            )
                        )
                        .clickable {
                            if (isPlaying) viewModel.pause() else viewModel.resume()
                        }
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Next Sentence
                IconButton(
                    onClick = { viewModel.skipForwardSentence() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Next sentence",
                        tint = GlassTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Settings Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice selector
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onNavigateToVoices() }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Voice selection",
                        tint = GlassTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Voice Library",
                        fontSize = 10.sp,
                        color = GlassTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Reading Mode Selector
                var modeExpanded by remember { mutableStateOf(false) }
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { modeExpanded = true }
                            .testTag("mode_selector_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Reading Mode",
                            tint = if (readingMode != "Story") NeonCyan else GlassTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$readingMode Mode",
                            fontSize = 10.sp,
                            color = if (readingMode != "Story") NeonCyan else GlassTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        listOf("Story", "Study", "Podcast", "Documentary", "Bedtime").forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode, color = Color.White) },
                                onClick = {
                                    viewModel.setReadingMode(mode)
                                    modeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Premium Control Center (Advanced Tab Sheets)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showAdvancedPanel = true }
                        .testTag("advanced_panel_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Control Panel",
                        tint = NeonPurple
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Control Center",
                        fontSize = 10.sp,
                        color = NeonPurple,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Speed
                var speedExpanded by remember { mutableStateOf(false) }
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { speedExpanded = true }
                            .testTag("speed_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Playback speed",
                            tint = GlassTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${book!!.speed}x Speed",
                            fontSize = 10.sp,
                            color = GlassTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = speedExpanded,
                        onDismissRequest = { speedExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { spd ->
                            DropdownMenuItem(
                                text = { Text("${spd}x", color = Color.White) },
                                onClick = {
                                    viewModel.changeSpeed(spd)
                                    speedExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sleep Timer
                var sleepExpanded by remember { mutableStateOf(false) }
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { sleepExpanded = true }
                            .testTag("sleep_timer_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Sleep timer",
                            tint = if (sleepTimeRemaining != null) NeonCyan else GlassTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val timerLabel = when {
                            sleepTimeRemaining == null -> "Timer Off"
                            sleepTimeRemaining == -1L -> "End of Chap"
                            else -> {
                                val min = TimeUnit.MILLISECONDS.toMinutes(sleepTimeRemaining!!)
                                val sec = TimeUnit.MILLISECONDS.toSeconds(sleepTimeRemaining!!) % 60
                                String.format("%02d:%02d", min, sec)
                            }
                        }
                        Text(
                            text = timerLabel,
                            fontSize = 10.sp,
                            color = if (sleepTimeRemaining != null) NeonCyan else GlassTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = sleepExpanded,
                        onDismissRequest = { sleepExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Turn Off", color = Color.White) },
                            onClick = { viewModel.stopSleepTimer(); sleepExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("15 minutes", color = Color.White) },
                            onClick = { viewModel.startSleepTimer(15); sleepExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("30 minutes", color = Color.White) },
                            onClick = { viewModel.startSleepTimer(30); sleepExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("45 minutes", color = Color.White) },
                            onClick = { viewModel.startSleepTimer(45); sleepExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("60 minutes", color = Color.White) },
                            onClick = { viewModel.startSleepTimer(60); sleepExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("End of Chapter", color = Color.White) },
                            onClick = { viewModel.startSleepTimer(-1); sleepExpanded = false }
                        )
                    }
                }
            }
        }

        // Bookmark Dialog
        if (showBookmarkDialog) {
            AlertDialog(
                onDismissRequest = { showBookmarkDialog = false },
                title = { Text("Add Bookmark note", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "\"${activeSentence.take(60)}...\"",
                            fontSize = 13.sp,
                            color = GlassTextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        TextField(
                            value = bookmarkNote,
                            onValueChange = { bookmarkNote = it },
                            placeholder = { Text("Enter personal notes (optional)") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        onClick = {
                            viewModel.addBookmark(bookmarkNote)
                            bookmarkNote = ""
                            showBookmarkDialog = false
                        }
                    ) {
                        Text("Save Bookmark", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        onClick = { showBookmarkDialog = false }
                    ) {
                        Text("Cancel", color = GlassTextSecondary)
                    }
                },
                containerColor = Color(0xFF151F32)
            )
        }

        // Smart Resume Dialogue Popup
        if (showSmartResumeDialog) {
            AlertDialog(
                onDismissRequest = { showSmartResumeDialog = false },
                title = { Text("Smart Resume", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "You were listening to Chapter ${book!!.currentChapterIndex + 1}, sentence ${book!!.currentSentenceIndex + 1}. Would you like to resume?",
                        color = GlassTextPrimary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        onClick = {
                            showSmartResumeDialog = false
                            viewModel.play()
                        }
                    ) {
                        Text("Continue Listening", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        onClick = {
                            viewModel.seekToSentence(0)
                            viewModel.skipToChapter(0)
                            showSmartResumeDialog = false
                        }
                    ) {
                        Text("Start Over", color = Color.White)
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // Chapters Drawer Overlay
        AnimatedVisibility(
            visible = showChaptersDrawer,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showChaptersDrawer = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clickable(enabled = false) {}, // Intercept click
                    backgroundColor = Color(0xFF0F172A),
                    borderColor = GlassBorder
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Chapter",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary
                            )
                            IconButton(onClick = { showChaptersDrawer = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                        ) {
                            itemsIndexed(chapters) { idx, chapter ->
                                val isSelected = book!!.currentChapterIndex == idx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable {
                                            viewModel.skipToChapter(idx)
                                            showChaptersDrawer = false
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = if (isSelected) NeonCyan else GlassTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = chapter.title,
                                        fontSize = 15.sp,
                                        color = if (isSelected) NeonCyan else GlassTextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Premium Control Center Slide-up Sheet Drawer
        AnimatedVisibility(
            visible = showAdvancedPanel,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showAdvancedPanel = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp)
                        .clickable(enabled = false) {}, // Intercept click
                    backgroundColor = Color(0xFF0F172A),
                    borderColor = GlassBorder
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        // Title header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Premium Options Center",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary
                            )
                            IconButton(onClick = { showAdvancedPanel = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category tabs selection row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Skip", "Dict", "Queue", "Stats", "Bookmarks").forEach { tab ->
                                val active = activeTab == tab
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) NeonPurple.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { activeTab = tab }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tab,
                                        color = if (active) NeonCyan else GlassTextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tab Display Dispatcher
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (activeTab) {
                                "Skip" -> {
                                    val rules = viewModel.getSkipRulesForBook(book!!.id)
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            "🛡️ Intelligent Document Skipping Rules",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )
                                        
                                        listOf(
                                            "skip_copyright" to "Skip Copyright & Publishers metadata",
                                            "skip_toc" to "Skip Tables of Contents and Indexes",
                                            "skip_intro" to "Skip Preface, Forewords & Authors notes",
                                            "skip_backmatter" to "Skip Appendices & Bibliographies"
                                        ).forEach { (key, label) ->
                                            val enabled = rules[key] == true
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.White.copy(alpha = 0.03f))
                                                    .clickable { viewModel.setSkipRuleForBook(book!!.id, key, !enabled) }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(label, color = GlassTextPrimary, fontSize = 12.sp)
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp, 24.dp)
                                                        .clip(CircleShape)
                                                        .background(if (enabled) NeonCyan else Color.White.copy(alpha = 0.2f)),
                                                    contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(2.dp)
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                "Dict" -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            "🗣️ Pronunciation Dictionary Override",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TextField(
                                                value = newWord,
                                                onValueChange = { newWord = it },
                                                placeholder = { Text("Word (e.g. SQL)") },
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextField(
                                                value = newReplacement,
                                                onValueChange = { newReplacement = it },
                                                placeholder = { Text("Speech mapping") },
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (newWord.isNotBlank() && newReplacement.isNotBlank()) {
                                                        viewModel.addPronunciation(newWord, newReplacement)
                                                        newWord = ""
                                                        newReplacement = ""
                                                    }
                                                },
                                                modifier = Modifier.background(NeonPurple, CircleShape)
                                            ) {
                                                Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "Add", tint = Color.White)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (pronunciations.isEmpty()) {
                                                item {
                                                    Text("No custom overrides configured yet.", color = GlassTextSecondary, fontSize = 12.sp)
                                                }
                                            } else {
                                                items(pronunciations.toList()) { (word, replacement) ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.02f))
                                                            .padding(8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(word, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text("pronounced as: \"$replacement\"", color = NeonCyan, fontSize = 11.sp)
                                                        }
                                                        IconButton(onClick = { viewModel.deletePronunciation(word) }) {
                                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "Queue" -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "📋 Custom Audiobook Listening Queue",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan
                                            )
                                            Row {
                                                Button(
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                                                    onClick = {
                                                        viewModel.addToQueue(
                                                            book = book!!,
                                                            chapterIndex = book!!.currentChapterIndex,
                                                            chapterTitle = activeChapterTitle,
                                                            sentenceIndex = book!!.currentSentenceIndex,
                                                            label = "Snippet from chapter ${book!!.currentChapterIndex + 1}"
                                                        )
                                                    }
                                                ) {
                                                    Text("Queue Current", fontSize = 11.sp, color = NeonCyan)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(onClick = { viewModel.clearQueue() }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Queue", tint = Color.White)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (readingQueue.isEmpty()) {
                                                item {
                                                    Text("Queue is empty. Select parts or bookmarks to listen next.", color = GlassTextSecondary, fontSize = 12.sp)
                                                }
                                            } else {
                                                items(readingQueue) { item ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.02f))
                                                            .clickable { viewModel.playQueueItem(item) }
                                                            .padding(8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(item.bookTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text("${item.chapterTitle} • sentence ${item.sentenceIndex + 1}", color = NeonCyan, fontSize = 11.sp)
                                                        }
                                                        IconButton(onClick = { viewModel.removeFromQueue(item.id) }) {
                                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "Stats" -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            "📊 Personal Reading Statistics",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Streak Card
                                            GlassCard(
                                                modifier = Modifier.weight(1f).height(100.dp),
                                                backgroundColor = Color.White.copy(alpha = 0.03f)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.Center,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text("🔥 Streak Days", color = GlassTextSecondary, fontSize = 12.sp)
                                                    Text("$streakDays Days", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                }
                                            }

                                            // Minutes Card
                                            GlassCard(
                                                modifier = Modifier.weight(1f).height(100.dp),
                                                backgroundColor = Color.White.copy(alpha = 0.03f)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.Center,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text("⏱️ Listening Time", color = GlassTextSecondary, fontSize = 12.sp)
                                                    Text(String.format("%.1f mins", listeningMinutes), color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                }
                                            }
                                        }

                                        // Completed Books Card
                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            backgroundColor = Color.White.copy(alpha = 0.03f)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("📚 Completed Audiobooks count:", color = GlassTextPrimary, fontSize = 14.sp)
                                                Text("$completedBooksCount Books", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            }
                                        }
                                    }
                                }

                                "Bookmarks" -> {
                                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "🔖 Bookmarks & Highlights",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan
                                            )
                                            
                                            // Export bookmarks
                                            IconButton(
                                                onClick = {
                                                    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                                                        append("EchoReader Bookmarks Export - ${book!!.title}\n\n")
                                                        bookmarks.forEach {
                                                            append("Chapter ${it.chapterIndex + 1} - sentence ${it.sentenceIndex + 1}\n")
                                                            append("\"${it.quoteText}\"\n")
                                                            if (it.note.isNotBlank()) append("Note: ${it.note}\n")
                                                            append("\n")
                                                        }
                                                    }
                                                    clipboardManager.setText(annotatedString)
                                                }
                                            ) {
                                                Icon(imageVector = Icons.Default.Share, contentDescription = "Export bookmarks", tint = NeonCyan)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (bookmarks.isEmpty()) {
                                                item {
                                                    Text("No bookmarks saved for this book. Tap 'Bookmark' on the player.", color = GlassTextSecondary, fontSize = 12.sp)
                                                }
                                            } else {
                                                items(bookmarks) { b ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.02f))
                                                            .clickable {
                                                                viewModel.skipToChapter(b.chapterIndex)
                                                                viewModel.seekToSentence(b.sentenceIndex)
                                                                showAdvancedPanel = false
                                                            }
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text("\"${b.quoteText.take(50)}...\"", color = Color.White, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                            if (b.note.isNotBlank()) {
                                                                Text("Note: ${b.note}", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                            Text("Chapter ${b.chapterIndex + 1} • sentence ${b.sentenceIndex + 1}", color = GlassTextSecondary, fontSize = 10.sp)
                                                        }
                                                        IconButton(onClick = { viewModel.deleteBookmark(b.id) }) {
                                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
